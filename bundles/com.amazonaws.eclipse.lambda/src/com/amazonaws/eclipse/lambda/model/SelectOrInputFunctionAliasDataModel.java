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

import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.eclipse.lambda.LambdaUtils;
import com.amazonaws.services.lambda.model.AliasConfiguration;

/**
 * Data model for selecting or creating a new Lambda function alias.
 */
public class SelectOrInputFunctionAliasDataModel extends SelectOrInputDataModel<AliasConfiguration, LambdaFunctionAliasesScopeParam> {
    private static final String RESOURCE_TYPE = "Function Alias";
    private static final AliasConfiguration LOADING = new AliasConfiguration().withName("Loading...");
    private static final AliasConfiguration NOT_FOUND = new AliasConfiguration().withName("Not Found");
    private static final AliasConfiguration ERROR = new AliasConfiguration().withName("Error");

    @Override
    public AliasConfiguration getLoadingItem() {
        return LOADING;
    }

    @Override
    public AliasConfiguration getNotFoundItem() {
        return NOT_FOUND;
    }

    @Override
    public AliasConfiguration getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getDefaultResourceName() {
        return "beta";
    }

    @Override
    public List<AliasConfiguration> loadAwsResources(LambdaFunctionAliasesScopeParam param) {
        return LambdaUtils.listFunctionAlias(param.getAccountId(), param.getRegionId(), param.getFunctionName());
    }

    @Override
    public String getResourceName(AliasConfiguration alias) {
        return alias.getName();
    }
}
