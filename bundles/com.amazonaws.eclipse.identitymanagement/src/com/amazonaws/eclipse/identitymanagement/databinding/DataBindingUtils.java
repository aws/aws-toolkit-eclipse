/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.identitymanagement.databinding;

import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;

import com.amazonaws.eclipse.databinding.DecorationChangeListener;

public class DataBindingUtils {

    /**
     * Adds a control status decorator for the control given.
     */
    public static void addStatusDecorator(final Control control, ValidationStatusProvider validationStatusProvider) {
        ControlDecoration decoration = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText("Invalid value");
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());
    }
}
