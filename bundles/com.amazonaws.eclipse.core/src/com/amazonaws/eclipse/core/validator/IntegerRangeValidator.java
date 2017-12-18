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

public class IntegerRangeValidator implements IValidator {
    private final String propertyName;
    private final int min;
    private final int max;

    public IntegerRangeValidator(String propertyName, int min, int max) {
        this.propertyName = propertyName;
        this.min = min;
        this.max = max;
    }

    @Override
    public IStatus validate(final Object value) {
        if (!(value instanceof String)) {
            return ValidationStatus.error(propertyName + " value must be specified!");
        }

        int number;
        try {
            number = Integer.parseInt((String) value);
        } catch (NumberFormatException exception) {
            return ValidationStatus.error(propertyName + " value must be an integer!");
        }

        if (number < min || number > max) {
            return ValidationStatus.error(String.format("%s value must be between %d and %d", propertyName, min, max));
        }

        return ValidationStatus.ok();
    }
}