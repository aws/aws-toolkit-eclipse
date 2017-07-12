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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding;

import java.util.regex.Pattern;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Validates a text-based config option
 */
public class ConfigurationSettingValidator implements IValidator {

    private ConfigurationOptionDescription configOption;

    public ConfigurationSettingValidator(ConfigurationOptionDescription configOption) {
        this.configOption = configOption;
    }

    @Override
    public IStatus validate(Object value) {
        String s = (String) value;

        if (s == null || s.length() == 0) {
            return ValidationStatus.ok();
        }

        if ( configOption.getMaxValue() != null ) {
            try {
                Integer i = Integer.parseInt(s);
                if ( i > configOption.getMaxValue() ) {
                    return ValidationStatus.error(configOption.getName() + " must be at most " + configOption.getMaxValue());
                }
            } catch ( NumberFormatException e ) {
                return ValidationStatus.error(s + " isn't an integer (" + configOption.getNamespace() + ":" + configOption.getName() + ")");
            }
        }

        if ( configOption.getMinValue() != null ) {
            try {
                Integer i = Integer.parseInt(s);
                if ( i < configOption.getMinValue() ) {
                    return ValidationStatus.error(configOption.getName() + " must be at least " + configOption.getMinValue());
                }
            } catch ( NumberFormatException e ) {
                return ValidationStatus.error(s + " isn't an integer (" + configOption.getNamespace() + ":" + configOption.getName() + ")");
            }
        }

        if ( configOption.getMaxLength() != null ) {
            if ( s.length() > configOption.getMaxLength() ) {
                return ValidationStatus.error(s + " is too long (max length  " + configOption.getMaxLength() + ")");
            }
        }

        if ( configOption.getRegex() != null && s != null && s.length() > 0 ) {
            Pattern regex = Pattern.compile(configOption.getRegex().getPattern());
            if ( !regex.matcher(s).matches() ) {
                return ValidationStatus.error(configOption.getName() + " must match the regular expression "
                        + configOption.getRegex().getPattern());
            }
        }

        return ValidationStatus.ok();
    }

}
