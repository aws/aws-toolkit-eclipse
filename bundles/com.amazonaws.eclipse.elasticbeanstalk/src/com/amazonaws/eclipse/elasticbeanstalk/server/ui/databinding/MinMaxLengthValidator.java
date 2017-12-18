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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.core.validator.StringLengthValidator;

/**
 * IValidator implementation that tests that a string for length constraints.
 * Excludes the range specified.
 * @deprecated to {@link StringLengthValidator}
 */
@Deprecated
public class MinMaxLengthValidator implements IValidator {
    private final String message = "%s must be %d to %d characters in length.";
    private final int min;
    private final int max;
    private final String fieldName;

    public MinMaxLengthValidator(String fieldName, int min, int max) {
        this.min = min;
        this.max = max;
        this.fieldName = fieldName;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;
        if (s.length() < min || s.length() > max) {
            return ValidationStatus.error(String.format(message, fieldName, min,
                    max));
        }
        return ValidationStatus.ok();
    }
}
