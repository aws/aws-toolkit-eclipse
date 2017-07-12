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

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;

/**
 * Simple listener that registers itself with an observable value that wraps an
 * IStatus object, then shows or hides a specified control decoration and
 * updates its description text when the wrapped IStatus object changes.
 */
public final class DecorationChangeListener implements IValueChangeListener {
    private final ControlDecoration decoration;

    public DecorationChangeListener(ControlDecoration decoration, IObservableValue observableValue) {
        this.decoration = decoration;
        observableValue.addValueChangeListener(this);
        updateDecoration((IStatus)observableValue.getValue());
    }

    @Override
    public void handleValueChange(ValueChangeEvent event) {
        IStatus status = (IStatus)event.getObservableValue().getValue();
        updateDecoration(status);
    }

    private void updateDecoration(IStatus status) {
        if (status.isOK()) {
            decoration.hide();
        } else {
            decoration.setDescriptionText(status.getMessage());
            decoration.show();
        }
    }
}
