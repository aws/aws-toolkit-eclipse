/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.ui;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.amazonaws.eclipse.core.model.MultipleSelectionListDataModel;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;

/**
 * A reusable Composite for multiple selection using check box.
 */
public class MultipleSelectionListComposite<T> extends Composite {
    private final MultipleSelectionListDataModel<T> dataModel;
    private final ListSelectionNotEmptyValidator<T> validator;
    private Table table;

    public MultipleSelectionListComposite(
            Composite parent,
            DataBindingContext context,
            MultipleSelectionListDataModel<T> dataModel,
            List<T> itemSet,
            List<T> defaultSelected,
            String selectedItemsEmptyErrorMessage) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.dataModel = dataModel;
        this.validator = new ListSelectionNotEmptyValidator<>(
                dataModel.getSelectedList(), selectedItemsEmptyErrorMessage);

        createControl(context, itemSet, defaultSelected);
    }

    private void createControl(DataBindingContext context, List<T> itemSet, List<T> defaultSelected) {
        table = new Table(this, SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
        table.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        for (T item : itemSet) {
            TableItem tableItem = new TableItem(table, SWT.NONE);
            tableItem.setData(item);
            tableItem.setText(item.toString());
            if (defaultSelected != null && defaultSelected.contains(item)) {
                tableItem.setChecked(true);
            }
        }

        ControlDecoration controlDecoration = newControlDecoration(table, "");
        context.addValidationStatusProvider(validator);
        new DecorationChangeListener(controlDecoration, validator.getValidationStatus());

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTableItemSelected();
            }
        });

        onTableItemSelected();
    }

    private void onTableItemSelected() {
        List<T> selectedItems  = dataModel.getSelectedList();
        selectedItems.clear();
        Arrays.stream(table.getItems())
                .filter(TableItem::getChecked)
                .forEach(tt -> selectedItems.add((T)tt.getData()));
        validator.revalidateDelegate();
    }

    private static class ListSelectionNotEmptyValidator<T> extends MultiValidator {
        private final List<T> model;
        private final String errorMessage;

        public ListSelectionNotEmptyValidator(List<T> selectedItems, String emptyErrorMessage) {
            this.model = selectedItems;
            this.errorMessage = emptyErrorMessage;
        }

        @Override
        protected IStatus validate() {
            if (model == null || model.isEmpty()) {
                return ValidationStatus.error(errorMessage);
            }
            return ValidationStatus.ok();
        }

        // We need to expose the protected revalidate method to explicitly refresh the validation status.
        public void revalidateDelegate() {
            revalidate();
        }
    }
}