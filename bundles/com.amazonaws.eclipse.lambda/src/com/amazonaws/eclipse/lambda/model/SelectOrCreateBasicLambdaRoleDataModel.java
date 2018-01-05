/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.model;

import java.util.List;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.SelectOrCreateDataModel;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;

public class SelectOrCreateBasicLambdaRoleDataModel extends SelectOrCreateDataModel<Role, AwsResourceScopeParamBase> {

    private static final Role LOADING = new Role().withRoleName("Loading...");
    private static final Role NONE_FOUND = new Role().withRoleName("None found");
    private static final Role ERROR = new Role().withRoleName("Error Loading Roles");

    @Override
    public Role getLoadingItem() {
        return LOADING;
    }

    @Override
    public Role getNotFoundItem() {
        return NONE_FOUND;
    }

    @Override
    public Role getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return "IAM Role";
    }

    @Override
    public String getDefaultResourceName() {
        return "lambda-basic-execution";
    }

    @Override
    public List<Role> loadAwsResources(AwsResourceScopeParamBase param) {
        AmazonIdentityManagement iamClient = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getIAMClientByRegion(param.getRegionId());
        return ServiceApiUtils.getAllLambdaRoles(iamClient);
    }

    @Override
    public String getResourceName(Role resource) {
        return resource.getRoleName();
    }
}