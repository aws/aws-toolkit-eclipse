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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import static com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.LaunchClientJob;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPublishingUtils;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.EnvironmentBehavior;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;

public class UpdateEnvironmentJob extends Job {

    private IPath exportedWar;
    private IModule moduleToPublish;
    private final Environment environment;
    private Job launchClientJob;
    private String versionLabel;

    public UpdateEnvironmentJob(Environment environment) {
        super("Updating AWS Elastic Beanstalk environment: " + environment.getEnvironmentName());
        this.environment = environment;

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

    // Try to delay the scheduling of the LauncClientJob
    // since use a scheduling rule to lock the server, which
    // locks up if we try to save files deployed to that server.
    private void cancelLaunchClientJob() {
        if (launchClientJob != null) return;

        launchClientJob = findLaunchClientJob();

        if (launchClientJob != null) launchClientJob.cancel();
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
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3Client();

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

                ElasticBeanstalkPublishingUtils utils = new ElasticBeanstalkPublishingUtils(client, s3, environment);
                utils.publishApplicationToElasticBeanstalk(exportedWar, versionLabel, new SubProgressMonitor(monitor, 20));
                utils.waitForEnvironmentToBecomeAvailable(moduleToPublish, new SubProgressMonitor(monitor, 20), runnable);

                behavior.updateServerState(IServer.STATE_STARTED);
                if (moduleToPublish != null) {
                    behavior.updateModuleState(moduleToPublish, IServer.STATE_STARTED, IServer.PUBLISH_STATE_NONE);
                }
            } catch (CoreException e) {
                behavior.updateServerState(IServer.STATE_UNKNOWN);
                behavior.updateModuleState(moduleToPublish, IServer.STATE_UNKNOWN, IServer.PUBLISH_STATE_UNKNOWN);
                return e.getStatus();
            }
        }

        if (monitor.isCanceled() == false && launchClientJob != null) launchClientJob.schedule();

        return Status.OK_STATUS;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

}
