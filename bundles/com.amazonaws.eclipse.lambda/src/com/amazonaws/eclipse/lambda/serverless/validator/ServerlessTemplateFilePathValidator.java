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
package com.amazonaws.eclipse.lambda.serverless.validator;

import java.io.IOException;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.core.util.PluginUtils;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.util.StringUtils;

public class ServerlessTemplateFilePathValidator implements IValidator {
    @Override
    public IStatus validate(Object value) {
        String filePath = (String) value;

        try {
            validateFilePath(filePath);
        } catch (Exception e) {
            return ValidationStatus.error(e.getMessage());
        }
        return ValidationStatus.ok();
    }

    public ServerlessModel validateFilePath(String filePath) {
        if (StringUtils.isNullOrEmpty(filePath)) {
            throw new IllegalArgumentException("Serverless template file path must be provided!");
        }
        try {
            filePath = PluginUtils.variablePluginReplace(filePath);
            ServerlessModel model = Serverless.load(filePath);
            if (model.getAdditionalResources().isEmpty() && model.getServerlessFunctions().isEmpty()) {
                throw new IllegalArgumentException("Serverless template file is not valid, you need to define at least one AWS Resource.");
            }
            return model;
        } catch (IOException | CoreException e) {
            throw new IllegalArgumentException(e);
        }
    }
}