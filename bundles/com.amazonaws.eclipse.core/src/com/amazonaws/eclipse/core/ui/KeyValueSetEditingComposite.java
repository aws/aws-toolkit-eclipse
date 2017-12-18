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
package com.amazonaws.eclipse.core.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;
import com.amazonaws.eclipse.core.validator.KeyNotDuplicateValidator;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;

public class KeyValueSetEditingComposite extends Composite {

    private final KeyValueSetDataModel dataModel;

    private TableViewer viewer;
    private Button addButton;
    private Button editButton;
    private Button removeButton;

    private Button saveButton;
    private final KeyValueEditingUiText uiText;

    private String addButtonText = "Add";
    private String editButtonText = "Edit";
    private String removeButtonText = "Remove";
    private String saveButtonText = "Save";

    private String keyColLabel = "name";
    private String valueColLabel = "value";

    private final List<IValidator> keyValidators;
    private final List<IValidator> valueValidators;
    private final SelectionListener saveListener;

    private KeyValueSetEditingComposite(Composite parent,
            KeyValueSetDataModel dataModel,
            List<IValidator> keyValidators,
            List<IValidator> valueValidators,
            SelectionListener saveListener,
            KeyValueEditingUiText uiText) {
        super(parent, SWT.BORDER);
        this.dataModel = dataModel;

        this.keyValidators = keyValidators;
        this.valueValidators = valueValidators;
        this.saveListener = saveListener;
        this.uiText = uiText;

        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        setLayout(new GridLayout());
        createControl();
        initButtonsStatus();
    }

    private void createControl() {
        createTagsTableViewerSection();
        createButtonsSection();
    }

    private void createTagsTableViewerSection() {
        Composite container = new Composite(this, SWT.NONE);
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        container.setLayout(tableColumnLayout);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer = new TableViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        viewer.getControl().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        viewer.setContentProvider(new ArrayContentProvider());
        createColumns(viewer, tableColumnLayout);
        viewer.setInput(dataModel.getPairSet());
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent e) {
                onEditButton();
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent arg0) {
                onSelectionChanged();
            }
        });
    }

    private void createButtonsSection() {
        Composite buttonPanel = new Composite(this, SWT.NONE);
        buttonPanel.setLayout(new GridLayout(4, false));
        buttonPanel.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        addButton = new Button(buttonPanel, SWT.PUSH);
        addButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Pair newPair = new Pair("", "");
                if (Window.OK == new KeyValueEditingDialog(getShell(), dataModel.getPairSet(), newPair,
                        uiText.getAddDialogTitle(), uiText.getAddDialogMessage(),
                        uiText.getKeyLabelText(), uiText.getValueLabelText()).open()) {
                    dataModel.getPairSet().add(newPair);
                    refreshOnEditing();
                }
            }
        });
        addButton.setText(addButtonText());

        editButton = new Button(buttonPanel, SWT.PUSH);
        editButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_EDIT));
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onEditButton();
            }
        });
        editButton.setText(editButtonText);

        removeButton = new Button(buttonPanel, SWT.PUSH);
        removeButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_REMOVE));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (Pair pair : getSelectedObjects()) {
                    dataModel.getPairSet().remove(pair);
                }
                refreshOnEditing();
            }
        });
        removeButton.setText(removeButtonText);

        saveButton = new Button(buttonPanel, SWT.PUSH);
        saveButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_SAVE));
        if (saveListener != null) {
            saveButton.addSelectionListener(saveListener);
        }
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveButton.setEnabled(false);
            }
        });
        saveButton.setText(saveButtonText);
    }

    private void onEditButton() {
        Collection<Pair> selectedPairs = getSelectedObjects();
        for (Pair pair : selectedPairs) {
            String oldKey = pair.getKey();
            String oldValue = pair.getValue();
            if (Window.OK != new KeyValueEditingDialog(getShell(), dataModel.getPairSet(), pair,
                    uiText.getEditDialogTitle(), uiText.getEditDialogMessage(),
                    uiText.getKeyLabelText(), uiText.getValueLabelText()).open()) {
                pair.setKey(oldKey);
                pair.setValue(oldValue);
            }
            refreshOnEditing();
        }
    }

    public void refresh() {
        updateUI(true);
    }

    private void updateUI(boolean initialRefresh) {
        viewer.refresh();
        addButton.setText(addButtonText());
        addButton.setEnabled(dataModel.isUnlimitedPairs() || dataModel.getPairSet().size() < dataModel.getMaxPairs());
        saveButton.setEnabled(!initialRefresh);
    }

    private void refreshOnEditing() {
        updateUI(false);
    }

    private String addButtonText() {
        return dataModel.isUnlimitedPairs() ? addButtonText :
            String.format("%s (%d left)", addButtonText, dataModel.getMaxPairs() - dataModel.getPairSet().size());
    }

    private void initButtonsStatus() {
        editButton.setEnabled(false);
        removeButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void onSelectionChanged() {
        boolean objectSelected = getSelectedObjects().size() > 0;
        editButton.setEnabled(objectSelected);
        removeButton.setEnabled(objectSelected);
    }

    private void createColumns(TableViewer viewer, TableColumnLayout layout) {
        createColumn(viewer, 0, keyColLabel, layout);
        createColumn(viewer, 1, valueColLabel, layout);
    }

    private Collection<Pair> getSelectedObjects() {
        IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
        List<Pair> pairs = new LinkedList<>();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (next instanceof Pair)
                pairs.add((Pair)next);
        }
        return pairs;
    }

    private TableViewerColumn createColumn(final TableViewer viewer, final int index, final String text, final TableColumnLayout layout) {
        TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        TableColumn column = viewerColumn.getColumn();
        column.setText(text);
        viewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                Pair pair = (Pair) element;
                switch (index) {
                case 0: return pair.getKey();
                case 1: return pair.getValue();
                }
                return element.toString();
            }
        });
        layout.setColumnData(column, new ColumnWeightData(50, 200, true));
        return viewerColumn;
    }

    public static class KeyValueEditingUiText {
        private String addDialogTitle = "Add Tag";
        private String addDialogMessage = "Add a new tag";
        private String editDialogTitle = "Edit Tag";
        private String editDialogMessage = "Edit the existing tag";
        private String keyLabelText = "Tag Name:";
        private String valueLabelText = "Tag Value:";

        public KeyValueEditingUiText() {}

        public KeyValueEditingUiText(String addDialogTitle, String addDialogMessage, String editDialogTitle,
                String editDialogMessage, String keyLabelText, String valueLabelText) {
            this.addDialogTitle = addDialogTitle;
            this.addDialogMessage = addDialogMessage;
            this.editDialogTitle = editDialogTitle;
            this.editDialogMessage = editDialogMessage;
            this.keyLabelText = keyLabelText;
            this.valueLabelText = valueLabelText;
        }

        public String getAddDialogTitle() {
            return addDialogTitle;
        }

        public void setAddDialogTitle(String addDialogTitle) {
            this.addDialogTitle = addDialogTitle;
        }

        public String getAddDialogMessage() {
            return addDialogMessage;
        }

        public void setAddDialogMessage(String addDialogMessage) {
            this.addDialogMessage = addDialogMessage;
        }

        public String getEditDialogTitle() {
            return editDialogTitle;
        }

        public void setEditDialogTitle(String editDialogTitle) {
            this.editDialogTitle = editDialogTitle;
        }

        public String getEditDialogMessage() {
            return editDialogMessage;
        }

        public void setEditDialogMessage(String editDialogMessage) {
            this.editDialogMessage = editDialogMessage;
        }

        public String getKeyLabelText() {
            return keyLabelText;
        }

        public void setKeyLabelText(String keyLabelText) {
            this.keyLabelText = keyLabelText;
        }

        public String getValueLabelText() {
            return valueLabelText;
        }

        public void setValueLabelText(String valueLabelText) {
            this.valueLabelText = valueLabelText;
        }
    }

    public static class KeyValueSetEditingCompositeBuilder {
        private List<IValidator> keyValidators = new ArrayList<>();
        private List<IValidator> valueValidators = new ArrayList<>();
        private SelectionListener saveListener;
        private KeyValueEditingUiText uiText = new KeyValueEditingUiText();

        public KeyValueSetEditingComposite build(Composite parent, KeyValueSetDataModel dataModel) {
            return new KeyValueSetEditingComposite(parent, dataModel, keyValidators, valueValidators, saveListener, uiText);
        }

        public KeyValueSetEditingCompositeBuilder addKeyValidator(IValidator validator) {
            this.keyValidators.add(validator);
            return this;
        }

        public KeyValueSetEditingCompositeBuilder addValueValidator(IValidator validator) {
            this.valueValidators.add(validator);
            return this;
        }

        public KeyValueSetEditingCompositeBuilder saveListener(SelectionListener saveListener) {
            this.saveListener = saveListener;
            return this;
        }

        public KeyValueSetEditingCompositeBuilder uiText(KeyValueEditingUiText uiText) {
            this.uiText = uiText;
            return this;
        }
    }

    private class KeyValueEditingDialog extends TitleAreaDialog {

        private final DataBindingContext dataBindingContext = new DataBindingContext();
        private final AggregateValidationStatus aggregateValidationStatus;
        private final List<Pair> pairSet;
        private final Pair pairModel;

        private TextComplex keyText;
        private TextComplex valueText;

        private final String title;
        private final String message;
        private final String keyLabel;
        private final String valueLabel;

        public KeyValueEditingDialog(Shell parent, List<Pair> pairSet, Pair model, String title, String message, String keyLabel, String valueLabel) {
            super(parent);
            this.pairSet = pairSet;
            this.pairModel = model;
            this.aggregateValidationStatus = new AggregateValidationStatus(
                    dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
            this.title = title;
            this.message = message;
            this.keyLabel = keyLabel;
            this.valueLabel = valueLabel;
        }

        @Override
        public void create() {
            super.create();
            setTitle(title);
            setMessage(message);
            aggregateValidationStatus.addChangeListener(new IChangeListener() {
                @Override
                public void handleChange(ChangeEvent event) {
                    populateValidationStatus();
                }
            });
            populateValidationStatus();
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite area = (Composite) super.createDialogArea(parent);
            Composite container = new Composite(area, SWT.NONE);
            container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            GridLayout layout = new GridLayout(2, false);
            container.setLayout(layout);

            keyText = TextComplex.builder(container, dataBindingContext, PojoObservables.observeValue(pairModel, Pair.P_KEY))
                    .labelValue(keyLabel)
                    .defaultValue(pairModel.getKey())
                    .addValidator(new NotEmptyValidator("The key name cannot be empty."))
                    .addValidator(new KeyNotDuplicateValidator(pairSet, pairModel, "This field must not contain duplicate items."))
                    .addValidators(keyValidators)
                    .build();

            valueText = TextComplex.builder(container, dataBindingContext, PojoObservables.observeValue(pairModel, Pair.P_VALUE))
                    .labelValue(valueLabel)
                    .defaultValue(pairModel.getValue())
                    .addValidators(valueValidators)
                    .build();

            return area;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        private void populateValidationStatus() {

            IStatus status = getValidationStatus();
            if (status == null) return;

            if (status.getSeverity() == IStatus.OK) {
                this.setErrorMessage(null);
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            } else {
                setErrorMessage(status.getMessage());
                getButton(IDialogConstants.OK_ID).setEnabled(false);
            }
        }

        private IStatus getValidationStatus() {
            if (aggregateValidationStatus == null) return null;
            Object value = aggregateValidationStatus.getValue();
            if (!(value instanceof IStatus)) return null;
            return (IStatus)value;
        }
    }
}
