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

import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * IValidator implementation that tests if the value being validated is included
 * in the specified observable set. If it does exist in that observable set, a
 * validation status is returned with the message specified in this object's
 * constructor, otherwise if it does not exist in that observable set, then an
 * OK status is returned.
 *
 * @param <T>
 *            The type of the object being validated.
 */
public class NotInListValidator<T> implements IValidator {
    private IObservableSet observableSet;
    private String message;

    public NotInListValidator(IObservableSet observableSet, String message) {
        this.observableSet = observableSet;
        this.message = message;
    }

    @Override
    public IStatus validate(Object object) {
        @SuppressWarnings("unchecked")
        T value = (T)object;
        if (observableSet.contains(value)) return ValidationStatus.info(message);
        return ValidationStatus.ok();
    }
}
