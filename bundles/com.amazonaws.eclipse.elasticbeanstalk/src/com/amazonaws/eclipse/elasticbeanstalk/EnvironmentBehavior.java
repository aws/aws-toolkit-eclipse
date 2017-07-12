/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
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
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;

public class EnvironmentBehavior extends ServerBehaviourDelegate {

    /** The latest status of this environment, as reported from AWS Elastic Beanstalk. */
    private EnvironmentStatus latestEnvironmentStatus;

    @Override
    public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor)
            throws CoreException {
        trace("setupLaunchConfiguration(launchConfig: " + workingCopy+ ", environment: " + getEnvironment().getEnvironmentName() + ")");
        super.setupLaunchConfiguration(workingCopy, monitor);
    }

    /**
     * The current job to update an AWS Elastic Beanstalk environment. We set up the
     * job as part of the WTP publishing process, then schedule it at the end of
     * publishing.
     */
    private UpdateEnvironmentJob currentUpdateEnvironmentJob;

    /**
     * Dialog to collect deployment information
     */
    private DeploymentInformationDialog deploymentInformationDialog;

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

        if ( getServer().getMode().equals(launchMode) ) {
            new RestartEnvironmentJob(getEnvironment()).schedule();
        } else if ( launchMode.equals(ILaunchManager.DEBUG_MODE) ) {
            enableDebugging();
            trace("Adding a debug port for environment " + getEnvironment().getEnvironmentName());
        }

        ElasticBeanstalkPlugin.getDefault().syncEnvironments();
    }

    @Override
    protected void publishStart(IProgressMonitor monitor) throws CoreException {
        trace("PublishStart: " + getEnvironment().getEnvironmentName());
        currentUpdateEnvironmentJob = new UpdateEnvironmentJob(getEnvironment(), getServer());
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

        try {
            if ( currentUpdateEnvironmentJob.needsToDeployNewVersion() ) {
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        Shell shell = Display.getDefault().getActiveShell();
                        if ( shell == null )
                            shell = Display.getDefault().getShells()[0];

                        /*
                         * Use the deployment dialog from earlier, if we have
                         * one. Otherwise create a new one.
                         */
                        if ( deploymentInformationDialog == null ) {
                            final List<ConfigurationSettingsDescription> settings = getEnvironment().getCurrentSettings();
                            String debugPort = Environment.getDebugPort(settings);
                            String securityGroup = Environment.getSecurityGroup(settings);
                            boolean confirmIngress = false;
                            if ( debugPort != null && securityGroup != null
                                    && !getEnvironment().isIngressAllowed(debugPort, settings) ) {
                                confirmIngress = true;
                            }

                            boolean letUserSelectVersionLabel = !getEnvironment().getIncrementalDeployment();

                            if (letUserSelectVersionLabel || confirmIngress) {
                                deploymentInformationDialog = new DeploymentInformationDialog(shell, getEnvironment(),
                                    getServer().getMode(), letUserSelectVersionLabel, false, confirmIngress);
                                deploymentInformationDialog.open();

                                if ( deploymentInformationDialog.getReturnCode() != MessageDialog.OK ) return;

                                // Allow ingress on their security group if necessary
                                if ( confirmIngress ) getEnvironment().openSecurityGroupPort(debugPort, securityGroup);
                            }
                        }

                        if (deploymentInformationDialog != null) {
                            currentUpdateEnvironmentJob.setVersionLabel(deploymentInformationDialog.getVersionLabel());
                            currentUpdateEnvironmentJob.setDebugInstanceId(deploymentInformationDialog.getDebugInstanceId());
                        }
                        setServerState(IServer.STATE_STARTING);
                        currentUpdateEnvironmentJob.schedule();
                    }
                });

                if ( deploymentInformationDialog != null
                        && deploymentInformationDialog.getReturnCode() != MessageDialog.OK ) {
                    updateModuleState(currentUpdateEnvironmentJob.getModuleToPublish(), IServer.STATE_UNKNOWN,
                            IServer.PUBLISH_STATE_UNKNOWN);
                    throw new CoreException(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                            "Publish canceled"));
                }
            }

        } finally {
            currentUpdateEnvironmentJob = null;
            deploymentInformationDialog = null;
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
        trace("canRestart(launchMode: " + mode + ", environment: " + getEnvironment().getEnvironmentName() + ")");
        if (latestEnvironmentStatus == null) return ERROR_STATUS;
        return super.canRestart(mode);
    }

    @Override
    public IStatus canStop() {
        trace("canStop(environment: " + getEnvironment().getEnvironmentName() + ")");
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

    public void updateServer(EnvironmentDescription environmentDescription, List<ConfigurationSettingsDescription> settings) {
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

            setServerStatus(new Status(Status.WARNING, ElasticBeanstalkPlugin.PLUGIN_ID,
                    environmentDescription.getSolutionStackName() + " : " + environmentDescription.getStatus()));
            if ( settings != null ) {
                String debugPort = Environment.getDebugPort(settings);
                if ( debugPort != null )
                    setMode(ILaunchManager.DEBUG_MODE);
                else
                    setMode(ILaunchManager.RUN_MODE);
            }
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

    /**
     * Enables debugging on a port the user chooses.
     */
    public void enableDebugging() {
        final List<ConfigurationSettingsDescription> settings = getEnvironment().getCurrentSettings();

        ConfigurationOptionSetting opt = Environment.getJVMOptions(settings);
        if ( opt == null ) {
            opt = new ConfigurationOptionSetting().withNamespace(ConfigurationOptionConstants.JVMOPTIONS)
                    .withOptionName("JVM Options");
        }

        String currentOptions = opt.getValue();
        if ( currentOptions == null ) {
            currentOptions = "";
        }

        if ( !currentOptions.contains("-Xdebug") ) {
            currentOptions += " " + "-Xdebug";
        }

        if ( !currentOptions.contains("-Xrunjdwp:") ) {

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    // Incremental deployments are automatically assigned version labels
                    boolean letUserSelectVersionLabel = !getEnvironment().getIncrementalDeployment();
                    deploymentInformationDialog = new DeploymentInformationDialog(Display.getDefault().getActiveShell(), getEnvironment(), ILaunchManager.DEBUG_MODE, letUserSelectVersionLabel, true, true);
                    deploymentInformationDialog.open();
                }
            });

            int result = deploymentInformationDialog.getReturnCode();
            if ( result == Dialog.OK ) {
                String debugPort = deploymentInformationDialog.getDebugPort();
                currentOptions += " " + "-Xrunjdwp:transport=dt_socket,address=" + debugPort + ",server=y,suspend=n";
            } else {
                deploymentInformationDialog = null;
                setServerState(IServer.STATE_UNKNOWN);
                ElasticBeanstalkPlugin.getDefault().syncEnvironments();
                throw new RuntimeException("Operation canceled");
            }

        } else {
            deploymentInformationDialog = null;
            throw new RuntimeException("Environment JVM options already contains -Xrunjdwp argument, " +
                    "but we were unable to determine the remote debugging port");
        }

        opt.setValue(currentOptions);

        UpdateEnvironmentRequest rq = new UpdateEnvironmentRequest();
        rq.setEnvironmentName(getEnvironment().getEnvironmentName());
        Collection<ConfigurationOptionSetting> outgoingSettings = new ArrayList<>();
        outgoingSettings.add(opt);
        rq.setOptionSettings(outgoingSettings);

        getEnvironment().getClient().updateEnvironment(rq);

        if ( !getEnvironment().isIngressAllowed(deploymentInformationDialog.getDebugPort(), settings) ) {
            getEnvironment().openSecurityGroupPort(deploymentInformationDialog.getDebugPort(),
                    Environment.getSecurityGroup(settings));
        }
    }
}
