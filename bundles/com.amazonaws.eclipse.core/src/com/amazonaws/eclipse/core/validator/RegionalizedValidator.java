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

import com.amazonaws.eclipse.core.regions.Region;

/**
 * Validator that asks for a Region when validating data.
 */
public abstract class RegionalizedValidator implements IValidator {
    protected Region region;
    // Any additional error message to be shown.
    protected String errorMessage;

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
