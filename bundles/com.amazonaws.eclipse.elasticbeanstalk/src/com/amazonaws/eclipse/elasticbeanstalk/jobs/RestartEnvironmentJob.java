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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.server.core.IServer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.EnvironmentBehavior;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.RestartAppServerRequest;

public class RestartEnvironmentJob extends Job {
    private final Environment environment;

    public RestartEnvironmentJob(Environment environment) {
        super("Restarting environment " + environment.getEnvironmentName());
        this.environment = environment;

        // TODO: Should we use a Job family so that we can easily give them all the same Job image?
        setProperty(IProgressConstants.ICON_PROPERTY,
            AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON));

        setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId()).getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        EnvironmentBehavior behavior = (EnvironmentBehavior)environment.getServer().loadAdapter(EnvironmentBehavior.class, monitor);

        try {
            client.restartAppServer(new RestartAppServerRequest().withEnvironmentName(environment.getEnvironmentName()));
            behavior.updateServerState(IServer.STATE_STARTED);
            return Status.OK_STATUS;
        } catch (AmazonClientException ace) {
            return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Unable to terminate environment " + environment.getEnvironmentName() + " : " + ace.getMessage(), ace);
        }
    }
}
