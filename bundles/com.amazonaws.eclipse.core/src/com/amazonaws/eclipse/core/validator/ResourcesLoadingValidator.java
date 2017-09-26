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

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam;
import com.amazonaws.eclipse.core.model.AwsResourceMetadata;

public class ResourcesLoadingValidator<T, P extends AbstractAwsResourceScopeParam<P>> extends RegionalizedValidator {

    private final AwsResourceMetadata<T, P> dataModel;

    public ResourcesLoadingValidator(AwsResourceMetadata<T, P> loadableResourceModel) {
        this.dataModel = loadableResourceModel;
    }

    @Override
    public IStatus validate(Object value) {
        if (value == dataModel.getLoadingItem()) {
            return ValidationStatus.error(
                    String.format("Loading %s", dataModel.getResourceType().toLowerCase()
                            + (region == null ? "..." : " from " + region.getName())));
        } else if (value == dataModel.getNotFoundItem()) {
            return ValidationStatus.error(
                    String.format("Can't find %s", dataModel.getResourceType().toLowerCase())
                            + (region == null ? "." : " in " + region.getName()));
        } else if (value == dataModel.getErrorItem()) {
            return ValidationStatus.error(
                    String.format("Failed to load %s", dataModel.getResourceType().toLowerCase())
                        + (region == null ? "" : " in " + region.getName())
                        + (errorMessage == null ? "." : ": " + errorMessage + "."));
        }
        return ValidationStatus.ok();
    }
}