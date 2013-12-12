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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.trace;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.LaunchClientJob;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkHttpLaunchable;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkLaunchableAdapter;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPublishingUtils;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.EnvironmentBehavior;
import com.amazonaws.eclipse.elasticbeanstalk.git.AWSGitPushCommand;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;

public class UpdateEnvironmentJob extends Job {

    private IPath exportedWar;
    private IModule moduleToPublish;
    private final Environment environment;
    private Job launchClientJob;
    private String versionLabel;
    private String debugInstanceId;
    private final IServer server;

    public UpdateEnvironmentJob(Environment environment, IServer server) {
        super("Updating AWS Elastic Beanstalk environment: " + environment.getEnvironmentName());
        this.environment = environment;
        this.server = server;

        ImageDescriptor imageDescriptor = AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON);
        setProperty(IProgressConstants.ICON_PROPERTY, imageDescriptor);

        setUser(true);
    }

    public void setModuleToPublish(IModule moduleToPublish, IPath exportedWar) {
        this.moduleToPublish = moduleToPublish;
        this.exportedWar = exportedWar;
    }

    public IModule getModuleToPublish() {
        return this.moduleToPublish;
    }

    public boolean needsToDeployNewVersion() {
        return (exportedWar != null);
    }

    // Try to delay the scheduling of the LaunchClientJob
    // since use a scheduling rule to lock the server, which
    // locks up if we try to save files deployed to that server.
    private void cancelLaunchClientJob() {
        if (launchClientJob != null) {
            return;
        }

        launchClientJob = findLaunchClientJob();

        if (launchClientJob != null) {
            launchClientJob.cancel();
        }
    }

    private Job findLaunchClientJob() {
        // Try to delay the scheduling of the LaunchClientJob
        // since use a scheduling rule to lock the server, which
        // locks up if we try to save files deployed to that server.
        Job[] jobs = Job.getJobManager().find(null);
        for (Job job : jobs) {
            if (job instanceof LaunchClientJob) {
                if (((LaunchClientJob) job).getServer().getId().equals(environment.getServer().getId())) {
                    trace("Identified LaunchClientJob: " + job);
                    return job;
                }
            }
        }

        trace("Unable to find LaunchClientJob!");
        return null;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());

        cancelLaunchClientJob();

        monitor.beginTask("Publishing to AWS Elastic Beanstalk", IProgressMonitor.UNKNOWN);
        EnvironmentBehavior behavior = (EnvironmentBehavior)environment.getServer().loadAdapter(EnvironmentBehavior.class, null);

        if (needsToDeployNewVersion()) {
            try {
                behavior.updateServerState(IServer.STATE_STARTING);

                Runnable runnable = new Runnable() {
                    public void run() {
                        cancelLaunchClientJob();
                    }
                };

                ElasticBeanstalkPublishingUtils utils = new ElasticBeanstalkPublishingUtils(environment);
                boolean doesEnvironmentExist = utils.doesEnvironmentExist(client, environment.getEnvironmentName());

                if (environment.getIncrementalDeployment()) {
                    AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountInfo(environment.getAccountId());
                    AWSGitPushCommand pushCommand = new AWSGitPushCommand(
                            getPrivateGitRepoLocation(environment), exportedWar.toFile(), environment,
                            new BasicAWSCredentials(accountInfo.getAccessKey(), accountInfo.getSecretKey()));

                    if (doesEnvironmentExist) {
                        /*
                         * If the environment already exists, then all we have to do is push through
                         * Git and it'll automatically create a new application version and kick off
                         * a deployment to the environment.
                         */
                        pushCommand.execute();
                    } else {
                        /*
                         * If the environment doesn't exist yet, then we need to create the application
                         * and push a new version with Git, then grab the ID of that new version and
                         * call the Beanstalk CreateEnvironment API.
                         */
                        utils.createNewApplication(environment.getApplicationName(),
                                                   environment.getApplicationDescription());
                        pushCommand.skipEnvironmentDeployment(true);
                        pushCommand.execute();

                        try {
                            versionLabel = utils.getLatestApplicationVersion(environment.getApplicationName());
                            utils.createNewEnvironment(versionLabel);
                        } catch (AmazonClientException ace) {
                            throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                                "Unable to create new environment: " + ace.getMessage(), ace));
                        }
                    }
                } else {
                    if (versionLabel == null) {
                        versionLabel = UUID.randomUUID().toString();
                    }
                    utils.publishApplicationToElasticBeanstalk(exportedWar, versionLabel, new SubProgressMonitor(monitor, 20));
                }

                utils.waitForEnvironmentToBecomeAvailable(moduleToPublish, new SubProgressMonitor(monitor, 20), runnable);

                behavior.updateServerState(IServer.STATE_STARTED);
                if ( moduleToPublish != null ) {
                    behavior.updateModuleState(moduleToPublish, IServer.STATE_STARTED, IServer.PUBLISH_STATE_NONE);
                }

                if ( server.getMode().equals(ILaunchManager.DEBUG_MODE) ) {
                    connectDebugger(monitor);
                }
            } catch (CoreException e) {
                // Ensure the launch client job has been canceled so that the
                // internal browser isn't opened with the user's application
                long startTime = System.currentTimeMillis();
                while (launchClientJob == null &&
                       (System.currentTimeMillis() - startTime) < 1000 * 60) {
                    try {Thread.sleep(1000);} catch (InterruptedException ie) {}
                    cancelLaunchClientJob();
                }

                behavior.updateServerState(IServer.STATE_UNKNOWN);
                behavior.updateModuleState(moduleToPublish, IServer.STATE_UNKNOWN, IServer.PUBLISH_STATE_UNKNOWN);
                return e.getStatus();
            }
        }

        // Update the URL of the client launch job (in a roundabout manner) to
        // point to the correct endpoint, depending on whether we intend to
        // connect to the environment CNAME or a particular instance
        ElasticBeanstalkHttpLaunchable launchable = ElasticBeanstalkLaunchableAdapter.getLaunchable(server);
        if ( launchable != null ) {
            if ( debugInstanceId != null && debugInstanceId.length() > 0 ) {
                try {
                    launchable.setHost(getEc2InstanceHostname());
                } catch ( Exception e ) {
                    AwsToolkitCore.getDefault().logException("Failed to set hostname", e);
                }
            } else {
                launchable.clearHost();
            }
        }

        if (monitor.isCanceled() == false
                && launchClientJob != null
                && ConfigurationOptionConstants.WEB_SERVER
                        .equals(environment.getEnvironmentTier())) {

            launchClientJob.schedule();
        }

        return Status.OK_STATUS;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public void setDebugInstanceId(String debugInstanceId) {
        this.debugInstanceId = debugInstanceId;
    }

    private File getPrivateGitRepoLocation(Environment environment) {
        String accountId = environment.getAccountId();
        String environmentName = environment.getEnvironmentName();

        IPath stateLocation = Platform.getStateLocation(ElasticBeanstalkPlugin.getDefault().getBundle());
        File gitReposDir = new File(stateLocation.toFile(), "git");
        return new File(gitReposDir, accountId + "-" + environmentName);
    }

    /**
     * Opens up a remote debugger connection based on the specified launch,
     * host, and port and optionally reports progress through a specified
     * progress monitor.
     *
     * @param monitor
     *            An optional progress monitor if progress reporting is desired.
     * @throws CoreException
     *             If any problems were encountered setting up the remote
     *             debugger connection to the specified host.
     */
    private void connectDebugger(IProgressMonitor monitor) throws CoreException {

        ILaunch launch = findLaunch();
        if (launch == null) {
            return;
        }

        try {
            List<ConfigurationSettingsDescription> settings = environment.getCurrentSettings();

            String debugPort = Environment.getDebugPort(settings);
            if ( !confirmSecurityGroupIngress(debugPort, settings) ) {
                return;
            }

            IVMConnector debuggerConnector = JavaRuntime.getDefaultVMConnector();

            Map<String, String> arguments = new HashMap<String, String>();
            arguments.put("timeout", "60000");
            arguments.put("hostname", getEc2InstanceHostname());
            arguments.put("port", debugPort);

            debuggerConnector.connect(arguments, monitor, launch);
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logException("Unable to connect debugger: " + e.getMessage(), e);
        }
    }

    /**
     * Confirms that the security group of the environment allows ingress on the
     * debug port given, prompting the user for permission to open it if not.
     */
    private boolean confirmSecurityGroupIngress(String debugPort, List<ConfigurationSettingsDescription> settings) {

        int debugPortInt = Integer.parseInt(debugPort);
        String securityGroup = Environment.getSecurityGroup(settings);

        if ( environment.isIngressAllowed(debugPortInt, settings) ) {
            return true;
        }

        // Prompt the user for security group ingress -- this is an edge case to
        // cover races only. In almost all cases, the user should have been
        // prompted for this information much earlier.
        final DebugPortDialog dialog = new DebugPortDialog(securityGroup, debugPort);
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                dialog.openDialog();
            }
        });

        if ( dialog.result == 0 ) {
            environment.openSecurityGroupPort(debugPortInt, securityGroup);
            return true;
        } else {
            return false;
        }

    }

    /**
     * Simple dialog to confirm the opening of a port on a security group.
     */
    private class DebugPortDialog {

        private int result;
        private final String debugPort;
        private final String securityGroup;

        public DebugPortDialog(String securityGroup, String debugPort) {
            super();
            this.securityGroup = securityGroup;
            this.debugPort = debugPort;
        }

        private void openDialog() {
            MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(),
                    "Authorize security group ingress?", AwsToolkitCore.getDefault().getImageRegistry()
                            .get(AwsToolkitCore.IMAGE_AWS_ICON),
                    "To connect the remote debugger, you will need to allow TCP ingress on port " + debugPort
                            + " for your EC2 security group " + securityGroup + ".  Continue?", MessageDialog.WARNING,
                    new String[] { "Continue", "Abort" }, 0);

            result = dialog.open();
        }

    }

    /**
     * Returns the public dns name of the instance in the environment to connect
     * the remote debugger to.
     */
    private String getEc2InstanceHostname() {
        String instanceId = debugInstanceId;
        // For some launches, we won't know the EC2 instance ID until this point.
        if ( instanceId == null || instanceId.length() == 0 ) {
            instanceId = environment.getEC2InstanceIds().iterator().next();
        }
        DescribeInstancesResult describeInstances = environment.getEc2Client().describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        if ( describeInstances.getReservations().isEmpty()
                || describeInstances.getReservations().get(0).getInstances().isEmpty() ) {
            return null;
        }
        return describeInstances.getReservations().get(0).getInstances().get(0).getPublicDnsName();
    }

    /**
     * Returns the debug launch object corresponding to this update operation,
     * or null if no such launch exists.
     */
    private ILaunch findLaunch() throws CoreException {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        for (ILaunch launch : manager.getLaunches()) {

            // TODO: figure out a more correct way of doing this
            if ( launch.getLaunchMode().equals(ILaunchManager.DEBUG_MODE)
                    && launch.getLaunchConfiguration() != null
                    && launch.getLaunchConfiguration().getAttribute("launchable-adapter-id", "")
                            .equals("com.amazonaws.eclipse.wtp.elasticbeanstalk.launchableAdapter")
                    && launch.getLaunchConfiguration().getAttribute("module-artifact", "")
                            .contains(moduleToPublish.getName()) ) {
                return launch;
            }
        }
        return null;
    }

}
