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

import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.util.StringUtils;

public class SelectOrInputStackDataModel extends SelectOrInputDataModel<StackSummary> {
    public static final StackSummary LOADING = new StackSummary().withStackName("Loading...");
    public static final StackSummary NONE_FOUND = new StackSummary().withStackName("None found");

    private String defaultStackNamePrefix;
    private String stackName;

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
        return StringUtils.isNullOrEmpty(stackNameFromModel) ? stackName : stackNameFromModel;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }
}
