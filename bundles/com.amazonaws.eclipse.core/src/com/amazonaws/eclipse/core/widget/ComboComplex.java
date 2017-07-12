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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;
import static com.amazonaws.util.ValidationUtils.assertNotEmpty;
import static com.amazonaws.util.ValidationUtils.assertNotNull;
import static com.amazonaws.util.ValidationUtils.assertStringNotEmpty;

import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.ComboBoxItemData;

/**
 * A complex Combo widget including a Label, DataBinding. The generic type T must be a #{@link com.amazonaws.eclipse.core.model.ComboBoxItemData} whose
 * getName() method returns the text shown in the combo.
 */
public class ComboComplex<T extends ComboBoxItemData> {

    private Combo combo;
    private ISWTObservableValue swtObservableValue;

    private ComboComplex(
            Composite composite,
            DataBindingContext dataBindingContext,
            IObservableValue pojoObservableValue,
            String label,
            Collection<T> items,
            T defaultItem,
            SelectionListener selectionListener,
            int comboColSpan) {
        newLabel(composite, label);
        combo = newCombo(composite, comboColSpan);
        for (T type : items) {
            combo.add(type.getComboBoxItemLabel());
            combo.setData(type.getComboBoxItemLabel(), type);
        }
        int defaultIndex = defaultItem == null ? 0 : combo.indexOf(defaultItem.getComboBoxItemLabel());
        if (defaultIndex < 0) defaultIndex = 0; // when defaultItem is not in List.
        combo.select(defaultIndex);

        swtObservableValue = SWTObservables.observeText(combo);
        dataBindingContext.bindValue(swtObservableValue, pojoObservableValue);
        swtObservableValue.setValue(combo.getText());
        if (selectionListener != null) combo.addSelectionListener(selectionListener);
    }

    public Combo getCombo() {
        return combo;
    }

    public static <T extends ComboBoxItemData> ComboComplexBuilder<T> builder() {
        return new ComboComplexBuilder<>();
    }

    public static class ComboComplexBuilder<T extends ComboBoxItemData> {
        private Composite composite;
        private DataBindingContext dataBindingContext;
        private IObservableValue pojoObservableValue;
        private String labelValue;
        private Collection<T> items;
        private T defaultItem;
        private String defaultItemName;
        private SelectionListener selectionListener;
        private int comboColSpan = 1;

        public ComboComplex<T> build() {
            validateParameters();
            if (defaultItem == null && defaultItemName != null) {
                defaultItem = findItemByName(defaultItemName);
            }
            return new ComboComplex<>(composite, dataBindingContext, pojoObservableValue,
                    labelValue, items, defaultItem, selectionListener, comboColSpan);
        }

        public ComboComplexBuilder<T> composite(Composite composite) {
            this.composite = composite;
            return this;
        }

        public ComboComplexBuilder<T> dataBindingContext(DataBindingContext dataBindingContext) {
            this.dataBindingContext = dataBindingContext;
            return this;
        }

        public ComboComplexBuilder<T> pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return this;
        }

        public ComboComplexBuilder<T> labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public ComboComplexBuilder<T> items(Collection<T> items) {
            this.items = items;
            return this;
        }

        public ComboComplexBuilder<T> defaultItem(T defaultItem) {
            this.defaultItem = defaultItem;
            this.defaultItemName = null;
            return this;
        }

        public ComboComplexBuilder<T> defaultItemName(String defaultItemName) {
            this.defaultItemName = defaultItemName;
            this.defaultItem = null;
            return this;
        }

        public ComboComplexBuilder<T> selectionListener(SelectionListener selectionListener) {
            this.selectionListener = selectionListener;
            return this;
        }

        public ComboComplexBuilder<T> comboColSpan(int comboColSpan) {
            this.comboColSpan = comboColSpan;
            return this;
        }

        private T findItemByName(String itemName) {
            if (items != null) {
                for (T item : items) {
                    if (itemName.equals(item.getComboBoxItemLabel())) {
                        return item;
                    }
                }
            }
            return null;
        }

        private void validateParameters() {
            assertNotNull(composite, "Composite");
            assertNotNull(dataBindingContext, "DataBindingContext");
            assertNotNull(pojoObservableValue, "PojoObservableValue");
            assertStringNotEmpty(labelValue, "LabelValue");
            assertNotEmpty(items, "ComboBox items");
        }
    }
}
