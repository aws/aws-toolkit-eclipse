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

import com.amazonaws.eclipse.cloudformation.CloudFormationUtils;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.util.StringUtils;

public class SelectOrInputStackDataModel extends SelectOrInputDataModel<StackSummary, AwsResourceScopeParamBase> {
    private static final String RESOURCE_TYPE = "Stack";
    private static final StackSummary LOADING = new StackSummary().withStackName("Loading...");
    private static final StackSummary NONE_FOUND = new StackSummary().withStackName("None found");
    private static final StackSummary ERROR = new StackSummary().withStackName("Error");

    private String defaultStackNamePrefix;

    public String getDefaultStackNamePrefix() {
        return defaultStackNamePrefix;
    }

    public void setDefaultStackNamePrefix(String defaultStackNamePrefix) {
        this.defaultStackNamePrefix = defaultStackNamePrefix;
    }

    public String getStackName() {
        String existingStackName = getExistingResource() == null ? null : getExistingResource().getStackName();
        String stackNameFromModel = isCreateNewResource() ? getNewResourceName()
                : isSelectExistingResource() ? existingStackName
                : null;
        return StringUtils.isNullOrEmpty(stackNameFromModel) ? null : stackNameFromModel;
    }

    @Override
    public StackSummary getLoadingItem() {
        return LOADING;
    }

    @Override
    public StackSummary getNotFoundItem() {
        return NONE_FOUND;
    }

    @Override
    public StackSummary getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getDefaultResourceName() {
        return defaultStackNamePrefix + "-stack";
    }


    @Override
    public List<StackSummary> loadAwsResources(AwsResourceScopeParamBase param) {
        return CloudFormationUtils.listExistingStacks(param.getRegionId());
    }

    @Override
    public String getResourceName(StackSummary stack) {
        return stack.getStackName();
    }
}
