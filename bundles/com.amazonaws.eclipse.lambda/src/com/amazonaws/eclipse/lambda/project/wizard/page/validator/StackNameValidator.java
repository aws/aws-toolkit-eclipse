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
package com.amazonaws.eclipse.lambda.project.wizard.page.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.util.StringUtils;

public class StackNameValidator implements IValidator {
    public String message;
    private static final String PATTERN = "^[a-zA-Z]([a-zA-Z\\-0-9])*";

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;
        if (StringUtils.isNullOrEmpty(s)) {
            return ValidationStatus.error("The stack name must be provided!");
        }
        s = s.trim();
        if (s.matches(PATTERN)) {
            return ValidationStatus.ok();
        } else {
            return ValidationStatus.error("Must contain only letters, numbers, dashes and start with an alpha character.");
        }
    }
}
