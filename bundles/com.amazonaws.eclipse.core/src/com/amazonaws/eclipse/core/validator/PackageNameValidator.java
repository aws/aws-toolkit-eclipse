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

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.util.StringUtils;

/**
 * Package name validator. It validates package name not null or empty and a valid name string.
 */
public class PackageNameValidator implements IValidator {

    private final String message;

    public PackageNameValidator(String message) {
        this.message = message;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;
        if (StringUtils.isNullOrEmpty(s)) {
            return ValidationStatus.error(message);
        }
        try {
            JavaPackageName.parse(s);
            return ValidationStatus.ok();

        } catch (Exception e) {
            return ValidationStatus.error(e.getMessage(), e);
        }
    }

}
