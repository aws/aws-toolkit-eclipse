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
package com.amazonaws.eclipse.cloudformation.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * A set of {@link #org.eclipse.core.databinding.validation.IValidator} for validating AWS CloudFormation template parameters.
 */
public class TemplateParameterValidators {

    public static IValidator newPatternValidator(final Pattern pattern, final String errorMessage) {
        return new IValidator() {
            @Override
            public IStatus validate(Object value) {
                Matcher matcher = pattern.matcher((CharSequence) value);
                if ( matcher.matches() ) {
                    return ValidationStatus.ok();
                } else {
                    return ValidationStatus.error(errorMessage);
                }
            }
        };
    }

    public static IValidator newMaxLengthValidator(final int maxLength, final String fieldName) {
        return new IValidator() {
            @Override
            public IStatus validate(Object value) {
                if ( ((String) value).length() > maxLength ) {
                    return ValidationStatus.error(fieldName + " must be at most " + maxLength + " characters long");
                } else {
                    return ValidationStatus.ok();
                }
            }
        };
    }

    public static IValidator newMinLengthValidator(final int minLength, final String fieldName) {
        return new IValidator() {
            @Override
            public IStatus validate(Object value) {
                if ( ((String) value).length() < minLength ) {
                    return ValidationStatus.error(fieldName + " must be at least " + minLength + " characters long");
                } else {
                    return ValidationStatus.ok();
                }
            }
        };
    }

    public static IValidator newMinValueValidator(final int minValue, final String fieldName) {
        return new IValidator() {
            @Override
            public IStatus validate(Object value) {
                String string = (String) value;
                try {
                    if ( Integer.parseInt(string) < minValue ) {
                        return ValidationStatus.error(fieldName + " must be at least " + minValue);
                    } else {
                        return ValidationStatus.ok();
                    }
                } catch ( Exception e ) {
                    return ValidationStatus.error(fieldName + " must be at least " + minValue);
                }
            }
        };
    }

    public static IValidator newMaxValueValidator(final int maxValue, final String fieldName) {
        return new IValidator() {
            @Override
            public IStatus validate(Object value) {
                String string = (String) value;
                try {
                    if ( Integer.parseInt(string) > maxValue ) {
                        return ValidationStatus.error(fieldName + " must be at most " + maxValue);
                    } else {
                        return ValidationStatus.ok();
                    }
                } catch ( Exception e ) {
                    return ValidationStatus.error(fieldName + " must be at most " + maxValue);
                }
            }
        };
    }
}
