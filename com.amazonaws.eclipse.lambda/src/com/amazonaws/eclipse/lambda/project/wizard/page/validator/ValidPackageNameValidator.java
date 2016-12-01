/*
 * Copyright 2015 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.lambda.project.wizard.page.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.lambda.project.wizard.util.JavaPackageName;
import com.amazonaws.util.StringUtils;

public class ValidPackageNameValidator implements IValidator {
    public String message;

    public ValidPackageNameValidator(String message) {
        this.message = message;
    }

    public IStatus validate(Object value) {
        String s = (String)value;
        if (StringUtils.isNullOrEmpty(s)) {
            return ValidationStatus.error("The package prefix must be provided!");
        }
        try {
            JavaPackageName.parse(s);
            return ValidationStatus.ok();

        } catch (Exception e) {
            return ValidationStatus.error(message);
        }
    }
}
