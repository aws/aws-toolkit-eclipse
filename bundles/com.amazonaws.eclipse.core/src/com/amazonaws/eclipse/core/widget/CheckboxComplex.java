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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCheckbox;
import static com.amazonaws.util.ValidationUtils.assertNotNull;
import static com.amazonaws.util.ValidationUtils.assertStringNotEmpty;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A complex Checkbox widget including a Label and DataBinding.
 */
public class CheckboxComplex {

    private final Button checkbox;
    private ISWTObservableValue swtObservableValue;

    private CheckboxComplex(
            Composite composite,
            DataBindingContext dataBindingContext,
            IObservableValue pojoObservableValue,
            SelectionListener selectionListener,
            String labelValue,
            boolean defaultValue,
            int colSpan) {

        checkbox = newCheckbox(composite, labelValue, colSpan);
        swtObservableValue = SWTObservables.observeSelection(checkbox);
        dataBindingContext.bindValue(swtObservableValue, pojoObservableValue);
        if (selectionListener != null) checkbox.addSelectionListener(selectionListener);
        swtObservableValue.setValue(defaultValue);
    }

    public Button getCheckbox() {
        return checkbox;
    }

    public static CheckboxComplexBuilder builder() {
        return new CheckboxComplexBuilder();
    }

    public static class CheckboxComplexBuilder {

        private Composite composite;
        private DataBindingContext dataBindingContext;
        private IObservableValue pojoObservableValue;
        private SelectionListener selectionListener;
        private String labelValue;
        private boolean defaultValue = false;
        private int colSpan = 1;

        public CheckboxComplex build() {
            validateParameters();
            return new CheckboxComplex(composite, dataBindingContext, pojoObservableValue,
                    selectionListener, labelValue, defaultValue, colSpan);
        }

        public CheckboxComplexBuilder composite(Composite composite) {
            this.composite = composite;
            return this;
        }

        public CheckboxComplexBuilder dataBindingContext(DataBindingContext dataBindingContext) {
            this.dataBindingContext = dataBindingContext;
            return this;
        }

        public CheckboxComplexBuilder pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return this;
        }

        public CheckboxComplexBuilder selectionListener(SelectionListener selectionListener) {
            this.selectionListener = selectionListener;
            return this;
        }

        public CheckboxComplexBuilder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public CheckboxComplexBuilder defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public CheckboxComplexBuilder colSpan(int colSpan) {
            this.colSpan = colSpan;
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
