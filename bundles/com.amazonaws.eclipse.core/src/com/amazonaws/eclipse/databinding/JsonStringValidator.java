/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.databinding;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple IValidator implementation that returns an OK status if the String
 * is in valid JSON format, otherwise it returns a validation status with
 * the message specified in the constructor.
 */
public class JsonStringValidator implements IValidator {
    private final String message;
    private final boolean allowEmptyString;

    private static final ObjectMapper mapper = new ObjectMapper();

    public JsonStringValidator(String message, boolean allowEmptyString) {
        this.message = message;
        this.allowEmptyString = allowEmptyString;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;

        if (s == null || s.isEmpty()) {
            return allowEmptyString ? ValidationStatus.ok() : ValidationStatus
                    .error(message);
        }

        try {
            mapper.readTree(s);
        } catch (Exception e) {
            return ValidationStatus.error(message);
        }
        return ValidationStatus.ok();
    }
}
