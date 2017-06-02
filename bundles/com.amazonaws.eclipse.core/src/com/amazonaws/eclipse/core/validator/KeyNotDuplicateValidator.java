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

import java.util.List;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;

public class KeyNotDuplicateValidator implements IValidator {
    private final List<Pair> pairSet;
    private final Pair pair;
    private final String dupErrorMessage;

    public KeyNotDuplicateValidator(List<Pair> pairSet, Pair pair, String dupErrorMessage) {
        this.pairSet = pairSet;
        this.pair = pair;
        this.dupErrorMessage = dupErrorMessage;
    }

    @Override
    public IStatus validate(Object value) {
        String keyValue = (String) value;

        for (Pair pairInSet : pairSet) {
            if (pair != pairInSet && pairInSet.getKey().equals(keyValue)) {
                return ValidationStatus.error(dupErrorMessage);
            }
        }
        return ValidationStatus.ok();
    }
}
