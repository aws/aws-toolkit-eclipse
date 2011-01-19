/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.amazonaws.eclipse.elasticbeanstalk.deploy.WTPWarUtils;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.RestartEnvironmentJob;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.TerminateEnvironmentJob;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.UpdateEnvironmentJob;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;

public class EnvironmentBehavior extends ServerBehaviourDelegate {

    /** The latest status of this environment, as reported from AWS Elastic Beanstalk. */
    private EnvironmentStatus latestEnvironmentStatus;

    /**
     * The current job to update an AWS Elastic Beanstalk environment. We set up the
     * job as part of the WTP publishing process, then schedule it at the end of
     * publishing.
     */
    private UpdateEnvironmentJob currentUpdateEnvironmentJob;

    private static final IStatus ERROR_STATUS = new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Environment is not ready");


    @Override
    public void stop(boolean force) {
        trace("Stopping (force:" + force + ")");
        setServerState(IServer.STATE_STOPPING);
        new TerminateEnvironmentJob(getEnvironment()).schedule();
    }

    @Override
    public void restart(String launchMode) throws CoreException {
        trace("Restarting(launchMode: " + launchMode + ", environment: " + getEnvironment().getEnvironmentName());
        setServerState(IServer.STATE_STARTING);
        new RestartEnvironmentJob(getEnvironment()).schedule();
    }

    @Override
    protected void publishStart(IProgressMonitor monitor) throws CoreException {
        trace("PublishStart: " + getEnvironment().getEnvironmentName());
        currentUpdateEnvironmentJob = new UpdateEnvironmentJob(getEnvironment());
    }

    @Override
    protected void publishModule(int publishKind, int deltaKind, IModule[] moduleTree, IProgressMonitor monitor)
            throws CoreException {
        trace("PublishModule:"
                + " (publishKind: " + WtpConstantsUtils.lookupPublishKind(publishKind)
                + " deltaKind: " + WtpConstantsUtils.lookupDeltaKind(deltaKind)
                + " moduleTree: " + Arrays.asList(moduleTree) + ")");

        // Ignore automatic publishes
        if (publishKind == IServer.PUBLISH_AUTO) return;

        // If the module doesn't need any publishing, and we don't need a full publish, don't do anything
        if (publishKind == IServer.PUBLISH_INCREMENTAL && deltaKind == NO_CHANGE) return;

        // If we're just removing a module, we don't need to do anything
        if (deltaKind == REMOVED) return;

        // TODO: If we can ask the job what module its uploading, we can check and not export twice
        IPath exportedWar = WTPWarUtils.exportProjectToWar(moduleTree[0].getProject(), getTempDirectory());
        monitor.worked(100);
        trace("Created war: " + exportedWar.toOSString());
        currentUpdateEnvironmentJob.setModuleToPublish(moduleTree[0], exportedWar);

        updateModuleState(moduleTree[0], IServer.STATE_STARTING, IServer.PUBLISH_STATE_NONE);
    }

    @Override
    protected void publishFinish(IProgressMonitor monitor) throws CoreException {
        trace("PublishFinish(" + getEnvironment().getEnvironmentName() + ")");

        final List<Integer> dialogReturnCode = new ArrayList<Integer>();
        if (currentUpdateEnvironmentJob.needsToDeployNewVersion()) {
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    Shell shell = Display.getDefault().getActiveShell();
                    if (shell == null) shell = Display.getDefault().getShells()[0];

                    String defaultVersionLabel = "v" + System.currentTimeMillis();
                    PublishDialog dialog = new PublishDialog(shell, defaultVersionLabel);
                    dialog.open();

                    dialogReturnCode.add(dialog.getReturnCode());
                    if (dialog.getReturnCode() != MessageDialog.OK) return;

                    setServerState(IServer.STATE_STARTING);
                    currentUpdateEnvironmentJob.setVersionLabel(dialog.getVersionLabel());
                    currentUpdateEnvironmentJob.schedule();
                }
            });
        }

        try {
        if (!dialogReturnCode.isEmpty() && dialogReturnCode.get(0) != MessageDialog.OK) {
                updateModuleState(currentUpdateEnvironmentJob.getModuleToPublish(), IServer.STATE_UNKNOWN, IServer.PUBLISH_STATE_UNKNOWN);
            throw new CoreException(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Publish canceled"));
        }
        } finally {
            currentUpdateEnvironmentJob = null;
        }
    }

    private int translateStatus(EnvironmentStatus status) {
        if (status == null) return IServer.STATE_STOPPED;

        switch (status) {
            case Launching:
            case Updating:
                return IServer.STATE_STARTING;
            case Ready:
                return IServer.STATE_STARTED;
            case Terminated:
                return IServer.STATE_STOPPED;
            case Terminating:
                return IServer.STATE_STOPPING;
            default:
                return IServer.STATE_UNKNOWN;
        }
    }

    @Override
    public IStatus canRestart(String mode) {
        if (latestEnvironmentStatus == null) return ERROR_STATUS;
        return super.canRestart(mode);
    }

    @Override
    public IStatus canStop() {
        if (latestEnvironmentStatus == null) return ERROR_STATUS;
        return super.canStop();
    }

    @Override
    public IStatus canStart(String launchMode) {
        trace("canStart(launchMode: " + launchMode + ", environment: " + getEnvironment().getEnvironmentName() + ")");

        // Don't allow the user to start the server if no projects are added yet
        if (getServer().getModules().length == 0) return ERROR_STATUS;

        if (latestEnvironmentStatus == null) return super.canStart(launchMode);

        if (latestEnvironmentStatus == EnvironmentStatus.Launching ||
            latestEnvironmentStatus == EnvironmentStatus.Updating ||
            latestEnvironmentStatus == EnvironmentStatus.Terminating) {
            return ERROR_STATUS;
        }
        return super.canStart(launchMode);
    }

    @Override
    public IStatus canPublish() {
        trace("canPublish(environment: " + getEnvironment().getEnvironmentName() + ")");

        // Don't allow the user to publish to the server if no projects are added yet
        if (getServer().getModules().length == 0) return ERROR_STATUS;

        if (latestEnvironmentStatus == null) return super.canPublish();

        if (latestEnvironmentStatus == EnvironmentStatus.Launching ||
            latestEnvironmentStatus == EnvironmentStatus.Updating ||
            latestEnvironmentStatus == EnvironmentStatus.Terminating) {
            return ERROR_STATUS;
        }

        return super.canPublish();
    }

    public void updateServer(EnvironmentDescription environmentDescription) {
        trace("Updating server with latest AWS Elastic Beanstalk environment description (server: " + getServer().getName() + ")");
        if (environmentDescription == null) {
            latestEnvironmentStatus = null;
        } else {
            try {
                latestEnvironmentStatus = EnvironmentStatus.fromValue(environmentDescription.getStatus());
            } catch (IllegalArgumentException e) {
                Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unknown environment status: " + environmentDescription.getStatus());
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }

            setServerStatus(new Status(Status.WARNING, ElasticBeanstalkPlugin.PLUGIN_ID, environmentDescription.getSolutionStackName() + " : " + environmentDescription.getStatus()));
        }

        setServerState(translateStatus(latestEnvironmentStatus));
        getEnvironment().setCachedEnvironmentDescription(environmentDescription);

        for (IModule module : getServer().getModules()) {
            setModuleStatus(new IModule[] {module}, new Status(IStatus.OK, ElasticBeanstalkPlugin.PLUGIN_ID, getEnvironment().getApplicationName()));
        }
    }

    protected Environment getEnvironment() {
        return (Environment)getServer().loadAdapter(ServerDelegate.class, null);
    }

    // This is called by our ElasticBeanstalkLaunchConfigurationDelegate, but only when
    //    the server is moving from stopped -> starting (if we have that flag set in plugin.xml).
    public void setupLaunch(ILaunch launch, String launchMode, IProgressMonitor monitor) throws CoreException {
        trace("EnvironmentBehavior:setupLaunch(" + launch + ", " + launchMode + ")");

        setServerRestartState(false);
        setServerState(IServer.STATE_STARTING);
        setMode(launchMode);
        setServerState(IServer.STATE_STARTED);
    }

    public void updateServerState(int state) {
        setServerState(state);
    }

    public void updateModuleState(IModule module, int moduleState, int modulePublishState) {
        setModuleState(new IModule[] {module}, moduleState);
        setModulePublishState(new IModule[] {module}, modulePublishState);

        for (IModule module2 : getEnvironment().getChildModules(new IModule[] {module})) {
            setModuleState(new IModule[] {module, module2}, moduleState);
            setModulePublishState(new IModule[] {module, module2}, modulePublishState);
        }
    }

}
