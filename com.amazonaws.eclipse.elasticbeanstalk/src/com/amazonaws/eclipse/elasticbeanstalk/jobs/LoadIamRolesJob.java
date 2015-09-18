/*
 * Copyright 2015 Amazon Technologies, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.diagnostic.utils.ServiceExceptionParser;
import com.amazonaws.eclipse.core.util.ValidationUtils;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;

public final class LoadIamRolesJob extends Job {

    /**
     * Callback to receive events from {@link LoadIamRolesJob}
     */
    public static interface LoadIamRolesCallback {

        /**
         * Called when roles are successfully loaded from service
         * 
         * @param roles
         *            List of roles in user's account
         */
        public void onLoadRoles(List<Role> roles);

        /**
         * Called when the currently configured user does not have permissions to list IAM roles
         */
        public void onInsufficientIamPermissions();

        /**
         * Called when we are unable to get the list of roles in the user's account for reasons
         * other then a permissions issue
         */
        public void onFailedToLoadRoles();
    }

    private final AmazonIdentityManagement iam = AwsToolkitCore.getClientFactory().getIAMClient();
    private final LoadIamRolesJob.LoadIamRolesCallback callback;

    public LoadIamRolesJob(LoadIamRolesJob.LoadIamRolesCallback callback) {
        super("Load IAM Roles");
        this.callback = ValidationUtils.validateNonNull(callback, "callback");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        List<Role> roles;
        try {
            roles = getAllRoles();
        } catch (Exception e) {
            return handleException(e);
        }

        // Callback is invoked outside the try block so that any exception it throws will fail the
        // job.
        callback.onLoadRoles(roles);
        return Status.OK_STATUS;
    }

    private IStatus handleException(Exception e) {
        if (ServiceExceptionParser.isOperationNotAllowedException(e)) {
            callback.onInsufficientIamPermissions();
            return Status.OK_STATUS;
        } else {
            callback.onFailedToLoadRoles();
            Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to query AWS Identity and Access Management for available instance profiles", e);
            ElasticBeanstalkPlugin.getDefault().getLog().log(status);
            return status;
        }
    }

    private List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<Role>();
        ListRolesResult result = null;
        do {
            ListRolesRequest request = new ListRolesRequest();
            result = iam.listRoles(request);
            roles.addAll(result.getRoles());

            if (result.isTruncated()) {
                request.setMarker(result.getMarker());
            }
        } while (result.isTruncated());

        return roles;
    }

}