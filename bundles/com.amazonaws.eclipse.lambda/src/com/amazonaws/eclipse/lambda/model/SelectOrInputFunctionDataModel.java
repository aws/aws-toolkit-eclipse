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

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;

import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.SelectOrInputDataModel;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;

public class SelectOrInputFunctionDataModel extends SelectOrInputDataModel<FunctionConfiguration, AwsResourceScopeParamBase> {
    private static final FunctionConfiguration LOADING = new FunctionConfiguration().withFunctionName("Loading...");
    private static final FunctionConfiguration NOT_FOUND = new FunctionConfiguration().withFunctionName("Not Found");
    private static final FunctionConfiguration ERROR = new FunctionConfiguration().withFunctionName("Error");

    public String getFunctionName() {
        return this.isCreateNewResource() ? this.getNewResourceName() :
            this.isSelectExistingResource() ? this.getExistingResource().getFunctionName() : null;
    }

    @Override
    public FunctionConfiguration getLoadingItem() {
        return LOADING;
    }

    @Override
    public FunctionConfiguration getNotFoundItem() {
        return NOT_FOUND;
    }

    @Override
    public FunctionConfiguration getErrorItem() {
        return ERROR;
    }

    @Override
    public String getResourceType() {
        return "Lambda Function";
    }

    @Override
    public String getDefaultResourceName() {
        return "MyFunction";
    }

    @Override
    public List<FunctionConfiguration> loadAwsResources(AwsResourceScopeParamBase param) {
        AWSLambda lambdaClient = AwsToolkitCore.getClientFactory(param.getAccountId())
                .getLambdaClientByRegion(param.getRegionId());
        return ServiceApiUtils.getAllJavaFunctions(lambdaClient);
    }

    @Override
    public String getResourceName(FunctionConfiguration resource) {
        return resource.getFunctionName();
    }
}