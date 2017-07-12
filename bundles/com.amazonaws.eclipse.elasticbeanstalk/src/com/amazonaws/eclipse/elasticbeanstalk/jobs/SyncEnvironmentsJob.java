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

import java.util.List;
import java.util.Random;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.EnvironmentBehavior;
import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;

public class SyncEnvironmentsJob extends Job {

    private static final int SHORT_DELAY = 1000 * 30;
    private static final int LONG_DELAY = 1000 * 60 * 4;
    private static final Random RANDOM = new Random();
    private String previousErrorMessage;


    public SyncEnvironmentsJob() {
        super("Synchronizing AWS Elastic Beanstalk environments");
        setProperty(IProgressConstants.ICON_PROPERTY,
            AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON));

        setSystem(true);
        setPriority(LONG);
        setUser(false);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Syncing", IProgressMonitor.UNKNOWN);
        trace("Syncing environment statuses");

        boolean transitioningEnvironment = false;
        Exception syncingError = null;
        for (IServer server : ServerCore.getServers()) {
            if (server.getServerType() == null) continue;
            String id = server.getServerType().getId();

            if (ElasticBeanstalkPlugin.SERVER_TYPE_IDS.contains(id)) {
                convertLegacyServer(server, monitor);
                Environment environment = (Environment)server.loadAdapter(Environment.class, monitor);
                EnvironmentBehavior behavior = (EnvironmentBehavior)server.loadAdapter(EnvironmentBehavior.class, monitor);

                monitor.setTaskName("Syncing environment " + environment.getEnvironmentName());
                try {
                    trace("Syncing server: " + server.getName() + ", " + "environment: " + environment.getEnvironmentName());
                    transitioningEnvironment |= syncEnvironment(environment, behavior);
                    previousErrorMessage = null;
                } catch (AmazonClientException ace) {
                    syncingError = ace;
                }
            }
        }

        if ( syncingError != null ) {
            schedule(LONG_DELAY);

            // Don't keep complaining about being unable to synchronize
            if ( previousErrorMessage != null &&
                    previousErrorMessage.equals(syncingError.getMessage()) ) {
                return Status.OK_STATUS;
            }

            previousErrorMessage = syncingError.getMessage();
            return new Status(Status.WARNING, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to synchronize an environment", syncingError);
        }

        if (transitioningEnvironment) schedule(SHORT_DELAY + RANDOM.nextInt(5 * 1000));
        else schedule(LONG_DELAY + RANDOM.nextInt(5 * 1000));

        return Status.OK_STATUS;
    }

    /**
     * Synchronizes the environment given with its AWS Elastic Beanstalk state and returns whether
     * it should be considered in a "transitioning" state.
     */
    private boolean syncEnvironment(Environment environment, EnvironmentBehavior behavior) {
        ElasticBeanstalkClientExtensions clientExt = new ElasticBeanstalkClientExtensions(environment);

        EnvironmentDescription environmentDescription = clientExt.getEnvironmentDescription(environment.getEnvironmentName());
        List<ConfigurationSettingsDescription> settings = environment.getCurrentSettings();
        behavior.updateServer(environmentDescription, settings);

        if (environmentDescription == null) return false;

        EnvironmentStatus environmentStatus = null;
        try {
            environmentStatus = EnvironmentStatus.fromValue(environmentDescription.getStatus());
        } catch (IllegalArgumentException e) {
            Status status = new Status(Status.INFO, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Unknown environment status: " + environmentDescription.getStatus());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        return (environmentStatus == EnvironmentStatus.Launching ||
                environmentStatus == EnvironmentStatus.Updating ||
                environmentStatus == EnvironmentStatus.Terminating);
    }

    /**
     * We change the data model to save the server environment information. This
     * function is used to convert the old data format to the new one.
     */
    private void convertLegacyServer(IServer server, IProgressMonitor monitor) {
        IServerWorkingCopy serverWorkingCopy = server.createWorkingCopy();
        Environment env = (Environment) serverWorkingCopy.loadAdapter(Environment.class, monitor);
        env.convertLegacyServer();
        try {
            serverWorkingCopy.save(true, monitor);
        } catch (CoreException e) {
            throw new AmazonClientException("Unable to synchronize with the beanstalk", e);
        }
    }

}
