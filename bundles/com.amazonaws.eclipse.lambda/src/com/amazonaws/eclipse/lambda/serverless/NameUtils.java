/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.serverless;

import com.amazonaws.util.StringUtils;

public class NameUtils {

    public static final String SERVERLESS_GENERATED_TEMPLATE_FILE_NAME = "serverless.generated.template";
    public static final String SERVERLESS_INPUT_CLASS_NAME = "ServerlessInput";
    public static final String SERVERLESS_OUTPUT_CLASS_NAME = "ServerlessOutput";

    /*
     * Transform the handler name (may or may not be the FQCN) to the class name.
     */
    public static String toHandlerClassName(String handlerName) {
        return capitalizeWord(handlerName.substring(handlerName.lastIndexOf(".") + 1));
    }

    public static String toModelClassName(String modelName) {
        return capitalizeWord(modelName);
    }

    public static String toHandlerPackageName(String packagePrefix) {
        return packagePrefix + ".function";
    }

    public static String toModelPackageName(String packagePrefix) {
        return packagePrefix + ".model";
    }

    // Replace handlerName with it's FQCN: if the handlerName is already FQCN or no package prefix
    // is specified, return itself.
    public static String toHandlerClassFqcn(String packagePrefix, String handlerName) {
        int lastIndexOfDot = handlerName.lastIndexOf('.');
        if (lastIndexOfDot != -1 || StringUtils.isNullOrEmpty(packagePrefix)) {
            return handlerName;
        }
        return toHandlerPackageName(packagePrefix) + "." + handlerName;
    }

    public static String toServerlessInputModelFqcn(String packagePrefix) {
        return toModelPackageName(packagePrefix) + "." + SERVERLESS_INPUT_CLASS_NAME;
    }

    public static String toServerlessOutputModelFqcn(String packagePrefix) {
        return toModelPackageName(packagePrefix) + "." + SERVERLESS_OUTPUT_CLASS_NAME;
    }

    public static String toRequestClassName(String operationName) {
        return toHandlerClassName(operationName) + "Request";
    }

    public static String toResponseClassName(String operationName) {
        return toHandlerClassName(operationName) + "Response";
    }

    public static String toRequestClassFqcn(String packagePrefix,
            String operationName) {
        return toModelPackageName(packagePrefix) + "."
                + toRequestClassName(operationName);
    }

    public static String toResponseClassFqcn(String packagePrefix,
            String operationName) {
        return toModelPackageName(packagePrefix) + "."
                + toResponseClassName(operationName);
    }

    private static String capitalizeWord(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }
}