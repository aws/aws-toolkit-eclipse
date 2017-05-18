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

import com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel;
import com.amazonaws.services.s3.model.Bucket;

public class SelectBucketValidator extends RegionalizedValidator {

    /* (non-Javadoc)
     * @see org.eclipse.core.databinding.validation.IValidator#validate(java.lang.Object)
     */
    @Override
    public IStatus validate(Object value) {
        Bucket bucket = (Bucket) value;

        if (bucket == SelectOrCreateBucketDataModel.LOADING) {
            return ValidationStatus.error(
                    region == null ? "Loading bucket." : "Loading bucket from " + region.getName());
        } else if (bucket == SelectOrCreateBucketDataModel.NONE_FOUND) {
            return ValidationStatus.error(
                    region == null ? "Can't find bucket." : "Can't find bucket in " + region.getName());
        }
        return ValidationStatus.ok();
    }
}
