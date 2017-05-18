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
package com.amazonaws.eclipse.core.widget;

import static com.amazonaws.util.ValidationUtils.assertNotNull;
import static com.amazonaws.util.ValidationUtils.assertStringNotEmpty;

import java.util.Iterator;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A complex Radio Button widget including DataBinding, a Label and a Selection Listener.
 */
public class RadioButtonComplex {
    private Button radio;
    private ISWTObservableValue swtObservableValue;

    public RadioButtonComplex(
            final Composite composite,
            final DataBindingContext dataBindingContext,
            final IObservableValue pojoObservableValue,
            final String labelValue,
            final boolean defaultValue,
            final SelectionListener selectionListener
            ) {
        radio = new Button(composite, SWT.RADIO);
        radio.setText(labelValue);
        if (selectionListener != null) radio.addSelectionListener(selectionListener);
        // When one Radio button is selected, Data Binding doesn't update other Radio buttons' status,
        // although other Radio buttons would be automatically deselected. Add this listener to update
        // Data Binding explicitly.
        radio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Iterator<?> iterator = dataBindingContext.getBindings().iterator();
                while (iterator.hasNext()) {
                    Binding binding = (Binding)iterator.next();
                    binding.updateTargetToModel();
                }
            }
        });
        swtObservableValue = SWTObservables.observeSelection(radio);
        dataBindingContext.bindValue(swtObservableValue, pojoObservableValue);
        swtObservableValue.setValue(defaultValue);
    }

    public void setValue(boolean value) {
        swtObservableValue.setValue(value);
    }

    public Button getRadioButton() {
        return radio;
    }

    public static RadioButtonComplexBuilder builder() {
        return new RadioButtonComplexBuilder();
    }

    public static class RadioButtonComplexBuilder {
        private Composite composite;
        private DataBindingContext dataBindingContext;
        private IObservableValue pojoObservableValue;
        private String labelValue;
        private boolean defaultValue;
        private SelectionListener selectionListener;

        public RadioButtonComplex build() {
            validateParameters();
            return new RadioButtonComplex(
                    composite, dataBindingContext, pojoObservableValue,
                    labelValue, defaultValue, selectionListener);
        }

        public RadioButtonComplexBuilder composite(Composite composite) {
            this.composite = composite;
            return this;
        }

        public RadioButtonComplexBuilder dataBindingContext(DataBindingContext dataBindingContext) {
            this.dataBindingContext = dataBindingContext;
            return this;
        }

        public RadioButtonComplexBuilder pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return this;
        }

        public RadioButtonComplexBuilder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public RadioButtonComplexBuilder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public RadioButtonComplexBuilder selectionListener(SelectionListener selectionListener) {
            this.selectionListener = selectionListener;
            return this;
        }

        private void validateParameters() {
            assertNotNull(composite, "Composite");
            assertNotNull(dataBindingContext, "DataBindingContext");
            assertNotNull(pojoObservableValue, "PojoObservableValue");
            assertStringNotEmpty(labelValue, "LabelValue");
        }
    }
}
