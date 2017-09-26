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
package com.amazonaws.eclipse.lambda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;

public class LambdaUtils {

    /**
     * Retrieve all the Lambda functions with the default setting and convert them to the specified type.
     */
    public static <T> List<T> listFunctions(FunctionConfigurationConverter<T> converter) {
        AWSLambda lambda = AwsToolkitCore.getClientFactory().getLambdaClient();

        List<T> newItems = new ArrayList<>();
        ListFunctionsRequest request = new ListFunctionsRequest();

        ListFunctionsResult result = null;
        do {
            result = lambda.listFunctions(request);

            for (FunctionConfiguration function : result.getFunctions()) {
                newItems.add(converter.convert(function));
            }
            request.setMarker(result.getNextMarker());
        } while (result.getNextMarker() != null);
        return newItems;
    }

    public static List<AliasConfiguration> listFunctionAlias(String accountId, String regionId, String functionName) {
        if (functionName == null) {
            return Collections.emptyList();
        }
        AWSLambda lambdaClient = AwsToolkitCore.getClientFactory(accountId).getLambdaClientByRegion(regionId);
        List<AliasConfiguration> aliasList = new ArrayList<>();

        ListAliasesRequest request = new ListAliasesRequest().withFunctionName(functionName);
        ListAliasesResult result = null;
        do {
            result = lambdaClient.listAliases(request);
            aliasList.addAll(result.getAliases());
            request.setMarker(result.getNextMarker());
        } while (result.getNextMarker() != null);
        return aliasList;
    }

    public interface FunctionConfigurationConverter<T> {
        T convert(FunctionConfiguration function);
    }
}
