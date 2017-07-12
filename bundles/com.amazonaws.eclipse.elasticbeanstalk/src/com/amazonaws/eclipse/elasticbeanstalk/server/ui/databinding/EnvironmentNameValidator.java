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
import org.eclipse.core.runtime.IStatus;

/**
 * IValidator implementation that tests if the environment name matches the
 * specification in http://docs.aws.amazon.com/elasticbeanstalk/latest/api/
 * API_CreateEnvironment.html.
 */
public class EnvironmentNameValidator implements IValidator {

    private static final String regex = "^[\\p{Alnum}]+[\\p{Alnum}-]+[\\p{Alnum}]$";

    private static final int MIN_LENGTH = 4;

    private static final int MAX_LENGTH = 40;

    private final MinMaxLengthValidator lengthValidator = new MinMaxLengthValidator(
            "Environment Name", MIN_LENGTH, MAX_LENGTH);

    private final RegexValidator regexValidator = new RegexValidator(
            "Environment name can contain only letters, alphabets and hyphens and cannot start/end with an hyphen.",
            regex);

    @Override
    public IStatus validate(Object value) {

        IStatus status = lengthValidator.validate(value);

        if (status.getSeverity() == IStatus.OK) {
            status = regexValidator.validate(value);
        }

        return status;
    }
}
