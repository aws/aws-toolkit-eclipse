/*
 * Copyright 2008-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.core.runtime.IStatus;

/**
 * A pluggable validation strategy.
 */
public interface InputValidator {
    /**
     * Validate whether the given value is valid (for some definition of
     * valid). Return an OK status if so; if not, return an error status
     * describing why it's not for display to the user.
     *
     * @param value the value to validate
     * @return the validation status
     */
    IStatus validate(final Object value);
}