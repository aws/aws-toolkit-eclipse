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

import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.LambdaUtils;
import com.amazonaws.eclipse.lambda.LambdaUtils.FunctionConfigurationConverter;
import com.amazonaws.services.lambda.model.FunctionConfiguration;

public class LambdaContentProvider extends AbstractContentProvider {

    public static final class LambdaRootElement {
        public static final LambdaRootElement ROOT_ELEMENT = new LambdaRootElement();
    }

    public static class FunctionNode extends ExplorerNode {
        private final FunctionConfiguration function;

        public FunctionNode(FunctionConfiguration function) {
            super(function.getFunctionName(), 0,
                loadImage(LambdaPlugin.getDefault(), LambdaPlugin.IMAGE_FUNCTION),
                new OpenFunctionEditorAction(function.getFunctionArn(), function.getFunctionName()));
            this.function = function;
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof LambdaRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] {LambdaRootElement.ROOT_ELEMENT};
        }

        if (parentElement instanceof LambdaRootElement) {
            new DataLoaderThread(parentElement) {

                @Override
                public Object[] loadData() {
                    return LambdaUtils.listFunctions(new FunctionConfigurationConverter<FunctionNode>() {
                        @Override
                        public FunctionNode convert(FunctionConfiguration function) {
                            return new FunctionNode(function);
                        }
                    }).toArray();
                }
            }.start();
        }
        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.LAMBDA;
    }

}
