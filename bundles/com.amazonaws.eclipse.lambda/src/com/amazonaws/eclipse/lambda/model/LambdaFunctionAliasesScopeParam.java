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

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam;

/**
 * Parameter class for loading AWS Lambda Function Aliases.
 */
public class LambdaFunctionAliasesScopeParam extends AbstractAwsResourceScopeParam<LambdaFunctionAliasesScopeParam> {
    private final String functionName;

    public LambdaFunctionAliasesScopeParam(String accountId, String regionId, String functionName) {
        super(accountId, regionId);
        this.functionName = functionName;
    }

    @Override
    public LambdaFunctionAliasesScopeParam copy() {
        return new LambdaFunctionAliasesScopeParam(getAccountId(), getRegionId(), getFunctionName());
    }

    public String getFunctionName() {
        return functionName;
    }
}
