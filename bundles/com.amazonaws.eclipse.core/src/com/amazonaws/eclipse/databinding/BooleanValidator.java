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

/**
 * Simple validator that checks a boolean value and returns an error message if
 * it's false.
 */
public class BooleanValidator implements IValidator {

    public String message;

    public BooleanValidator(String message) {
        this.message = message;
    }

    @Override
    public IStatus validate(Object value) {
        if ( !((Boolean) value) )
            return ValidationStatus.error(message);
        return ValidationStatus.ok();
    }
}
