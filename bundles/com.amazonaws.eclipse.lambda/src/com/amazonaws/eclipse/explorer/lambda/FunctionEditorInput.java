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
package com.amazonaws.eclipse.explorer.lambda;

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.services.lambda.AWSLambda;

public class FunctionEditorInput extends AbstractAwsResourceEditorInput {
    private final String functionArn;
    private final String functionName;

    public FunctionEditorInput(String accountId, String regionId, String functionArn, String functionName) {
        super(RegionUtils.getRegion(regionId).getServiceEndpoint(ServiceAbbreviations.LAMBDA), accountId, regionId);
        this.functionArn = functionArn;
        this.functionName = functionName;
    }

    public String getFunctionArn() {
        return functionArn;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return LambdaPlugin.getDefault().getImageRegistry().getDescriptor(LambdaPlugin.IMAGE_FUNCTION);
    }

    @Override
    public String getName() {
        return functionName;
    }

    @Override
    public String getToolTipText() {
        return "AWS Lambda Function Editor - " + getName();
    }

    public AWSLambda getLambdaClient() {
        return AwsToolkitCore.getClientFactory(getAccountId())
                .getLambdaClientByRegion(getRegionId());
    }
}
