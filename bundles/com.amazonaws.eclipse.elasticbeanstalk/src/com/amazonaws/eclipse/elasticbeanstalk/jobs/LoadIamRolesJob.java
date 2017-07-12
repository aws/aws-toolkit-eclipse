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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.diagnostic.utils.ServiceExceptionParser;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;

public final class LoadIamRolesJob extends LoadResourcesJob<Role> {

    private final AmazonIdentityManagement iam = AwsToolkitCore.getClientFactory().getIAMClient();

    public LoadIamRolesJob(LoadResourcesCallback<Role> callback) {
        super("Load IAM Roles", callback);
    }

    @Override
    protected List<Role> getAllResources() {
        List<Role> roles = new ArrayList<>();
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

    @Override
    protected boolean isInsufficientPermissions(Exception e) {
        return ServiceExceptionParser.isOperationNotAllowedException(e);
    }

    @Override
    protected String getOnFailureMessage() {
        return "Unable to query AWS Identity and Access Management for available instance profiles";
    }

}