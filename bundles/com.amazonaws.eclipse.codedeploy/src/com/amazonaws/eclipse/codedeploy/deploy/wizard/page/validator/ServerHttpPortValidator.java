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
package com.amazonaws.eclipse.codedeploy.deploy.wizard.page.validator;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

public class ServerHttpPortValidator implements IValidator {
    public String message;

    public ServerHttpPortValidator(String message) {
        this.message = message;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String)value;

        if (s == null || s.trim().length() == 0) {
            return ValidationStatus.error(message);
        }

        try {
            int port = Integer.parseInt(s.trim());
            if (port < 0 || port > 65535) {
                return ValidationStatus.error(message + " Port out of range [0-65535].");
            }

        } catch (NumberFormatException e) {
            return ValidationStatus.error(message + " A number between [0-65535] is expected.");
        }

        return ValidationStatus.ok();
    }
}
