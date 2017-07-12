/*
 * Copyright 2008-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

/**
 * An IValueChangeListener that watches the validation status for a
 * particular control and adds an error decoration whenever validation
 * fails.
 */
public class ErrorDecorator implements IValueChangeListener {

    private final ControlDecoration decoration;

    /**
     * Create a new Error decorator.
     *
     * @param control the control to decorate
     */
    public ErrorDecorator(final Control control) {
        decoration = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        decoration.setImage(FieldDecorationRegistry
            .getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
            .getImage()
        );
    }

    /**
     * Create a new ErrorDecorator attached to the given control and
     * bind it to an IObservableValue tracking an IStatus indicating whether
     * the value in the control is valid.
     *
     * @param control the control to decorate
     * @param status the status to observe
     */
    public static void bind(final Control control,
                            final IObservableValue status) {

        ErrorDecorator decorator = new ErrorDecorator(control);

        status.addValueChangeListener(decorator);

        // Update the decoration with the current status value.
        decorator.update((IStatus) status.getValue());
    }

    /**
     * React to a change in the validation status by updating the decoration.
     *
     * @param event the value change event
     */
    @Override
    public void handleValueChange(final ValueChangeEvent event) {
        update((IStatus) event.getObservableValue().getValue());
    }

    /**
     * Update the decoration based on the current validation status of the
     * control. If the input value is valid, hide the decoration. If not,
     * show the decoration and display the error text to the user.
     *
     * @param status the current validation status
     */
    private void update(final IStatus status) {
        if (status.isOK()) {
            decoration.hide();
        } else {
            decoration.setDescriptionText(status.getMessage());
            decoration.show();
        }
    }
}
