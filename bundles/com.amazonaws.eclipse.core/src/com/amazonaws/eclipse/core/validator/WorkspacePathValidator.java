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
package com.amazonaws.eclipse.core.validator;

import java.io.File;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.core.util.PluginUtils;

/**
 * Validator that validates the provided path is valid and the underlying file exists.
 */
public class WorkspacePathValidator implements IValidator {
    private final String propertyName;
    private final boolean isEmptyAllowed;

    public WorkspacePathValidator(String propertyName, boolean isEmptyAllowed) {
        this.propertyName = propertyName;
        this.isEmptyAllowed = isEmptyAllowed;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.databinding.validation.IValidator#validate(java.lang.Object)
     */
    @Override
    public IStatus validate(Object value) {
        try {
            String filePath = (String) value;
            if (filePath == null || filePath.isEmpty()) {
                if (isEmptyAllowed) {
                    return ValidationStatus.ok();
                } else {
                    return ValidationStatus.error(propertyName + " value must not be empty.");
                }
            }
            filePath = PluginUtils.variablePluginReplace(filePath);
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                return ValidationStatus.error(propertyName + " value is not a valid file!");
            }
            return ValidationStatus.ok();
        } catch (CoreException e) {
            return ValidationStatus.error(propertyName + " value is not a valid file!");
        }
    }
}
