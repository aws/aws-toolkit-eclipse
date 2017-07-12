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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * MultiValidator subclass that observes a value (and an optional enabler value
 * that controls whether this validator should currently report errors or not),
 * and runs a chain of IValidators on it. If any of the IValidators return an
 * error status, this MultiValidator will immediately return that error status,
 * otherwise this MultiValidator returns an OK status.
 *
 * @param <T>
 *            The type of the model object being observed.
 */
public class ChainValidator<T> extends MultiValidator {
    private IObservableValue model;
    private List<IValidator> validators = new ArrayList<>();
    private final IObservableValue enabler;

    public ChainValidator(IObservableValue model, IValidator... validators) {
        this(model, null, validators);
    }

    public ChainValidator(IObservableValue model, IObservableValue enabler, IValidator... validators) {
        this(model, enabler, Arrays.asList(validators));
    }

    public ChainValidator(IObservableValue model, IObservableValue enabler, List<IValidator> validators) {
        this.model = model;
        this.enabler = enabler;
        this.validators = validators;
    }

    @Override
    protected IStatus validate() {
        @SuppressWarnings("unchecked")
        T value = (T) model.getValue();

        if (enabler != null) {
            boolean isEnabled = enabler.getValue() != null && (Boolean) enabler.getValue();
            if (!isEnabled) return ValidationStatus.ok();
        }

        for (IValidator validator : validators) {
            IStatus status = validator.validate(value);
            if (!status.isOK()) return status;
        }

        return ValidationStatus.ok();
    }
}
