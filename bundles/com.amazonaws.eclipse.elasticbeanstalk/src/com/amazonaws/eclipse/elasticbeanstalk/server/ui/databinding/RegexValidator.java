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

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * IValidator implementation that tests that a string matches the given regex
 */
public class RegexValidator implements IValidator {
    private final String message;
    private final String regex;

    public RegexValidator(String message, String regex) {
        this.message = message;
        this.regex = regex;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;
        if (!Pattern.matches(regex, s)) return ValidationStatus.error(message);
        return ValidationStatus.ok();
    }
}
