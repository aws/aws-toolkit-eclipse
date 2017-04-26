/*
 * Copyright 2016 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.amazonaws.eclipse.core.util.ValidationUtils;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;

/**
 * A Job that load specified resources such as IAM Roles, VPCs, Subnets etc, and use
 * corresponding callback to handle different situations.
 */
public abstract class LoadResourcesJob<Resource> extends Job {

    protected final LoadResourcesCallback<Resource> callback;

    public LoadResourcesJob(String name, LoadResourcesCallback<Resource> callback) {
        super(name);
        this.callback = ValidationUtils.validateNonNull(callback, "Callback");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        List<Resource> resources;
        try {
            resources = getAllResources();
        } catch (Exception e) {
            return handleException(e);
        }

        // Callback is invoked outside the try block so that any exception it throws will fail the job.
        callback.onSuccess(resources);
        return Status.OK_STATUS;
    }

    private IStatus handleException(Exception e) {
        if (isInsufficientPermissions(e)) {
            callback.onInsufficientPermissions();
            return Status.OK_STATUS;
        } else {
            callback.onFailure();
            Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    getOnFailureMessage(), e);
            ElasticBeanstalkPlugin.getDefault().getLog().log(status);
            return status;
        }
    }

    // Load all the resources
    protected abstract List<Resource> getAllResources();

    // Whether this exception is due to lack of permission
    protected abstract boolean isInsufficientPermissions(Exception e);

    // The error message that will be shown in the log when failed to load the resources.
    protected abstract String getOnFailureMessage();
}
