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
package com.amazonaws.eclipse.explorer.dynamodb;

import java.util.LinkedList;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.dynamodb.AbstractAddNewAttributeDialog;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.LocalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.Projection;

public class AddLSIDialog extends TitleAreaDialog {

    private Text attributeNameText;
    private Text indexNameText;
    private Combo attributeTypeCombo;
    private Combo projectionTypeCombo;
    private LocalSecondaryIndex localSecondaryIndex;
    private Button okButton;
    private Button addAttributeButton;
    private AttributeDefinition attributeDefinition;

    private IObservableValue indexName;
    private IObservableValue secondaryIndexName;
    private IObservableValue attributeName;
    private IObservableValue attributeType;
    private IObservableValue projectionType;

    private final DataBindingContext bindingContext = new DataBindingContext();
    private static final String[] DATA_TYPE_STRINGS = new String[] { "String", "Number" };
    private static final String[] PROJECTED_ATTRIBUTES = new String[] { "All Attributes", "Table and Index Keys", "Specify Attributes" };

    public AddLSIDialog(Shell parentShell, CreateTableDataModel dataModel) {
        super(parentShell);
        // Initialize the variable to use it for databinding
        localSecondaryIndex = new LocalSecondaryIndex();
        localSecondaryIndex.withKeySchema(new KeySchemaElement().withAttributeName(dataModel.getHashKeyName()).withKeyType(KeyType.HASH));
        localSecondaryIndex.getKeySchema().add(new KeySchemaElement().withKeyType(KeyType.RANGE));
        localSecondaryIndex.setProjection(new Projection().withNonKeyAttributes(new LinkedList<String>()));

        // Generate these IObservableValue
        indexName = PojoObservables.observeValue(localSecondaryIndex, "indexName");
        attributeDefinition = new AttributeDefinition();
        dataModel.getAttributeDefinitions().add(attributeDefinition);
        secondaryIndexName = PojoObservables.observeValue(localSecondaryIndex.getKeySchema().get(1), "attributeName");
        attributeType = PojoObservables.observeValue(attributeDefinition, "attributeType");
        attributeName = PojoObservables.observeValue(attributeDefinition, "attributeName");
        projectionType = PojoObservables.observeValue(localSecondaryIndex.getProjection(), "projectionType");

        setShellStyle(SWT.RESIZE);

    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("Add Local Secondary Index");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(false);
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Add Local Secondary Index");
        shell.setMinimumSize(400, 500);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout());
        composite = new Composite(composite, SWT.NULL);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout(2, false));

        // Attribute name
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Attribute to Index:");
        attributeNameText = new Text(composite, SWT.BORDER);
        bindingContext.bindValue(SWTObservables.observeText(attributeNameText, SWT.Modify), secondaryIndexName);
        ChainValidator<String> attributeNameValidationStatusProvider = new ChainValidator<String>(secondaryIndexName, new NotEmptyValidator("Please provide a attribute name"));
        bindingContext.addValidationStatusProvider(attributeNameValidationStatusProvider);
        bindingContext.bindValue(SWTObservables.observeText(attributeNameText, SWT.Modify), attributeName);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(attributeNameText);

        // Attribute type
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Attribute Type:");
        attributeTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        attributeTypeCombo.setItems(DATA_TYPE_STRINGS);
        attributeTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(attributeTypeCombo), attributeType);

        // Index name
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Index Name:");
        indexNameText = new Text(composite, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indexNameText);
        bindingContext.bindValue(SWTObservables.observeText(indexNameText, SWT.Modify), indexName);
        ChainValidator<String> indexNameValidationStatusProvider = new ChainValidator<String>(indexName, new NotEmptyValidator("Please provide an index name"));
        bindingContext.addValidationStatusProvider(indexNameValidationStatusProvider);

        // Projection type
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Projected Attributes:");
        projectionTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        projectionTypeCombo.setItems(PROJECTED_ATTRIBUTES);
        projectionTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(projectionTypeCombo), projectionType);
        projectionTypeCombo.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                if (projectionTypeCombo.getSelectionIndex() == 2) {
                    addAttributeButton.setEnabled(true);
                } else {
                    addAttributeButton.setEnabled(false);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        // Attribute list in projection
        final AttributeList attributeList = new AttributeList(composite);
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(attributeList);
        addAttributeButton = new Button(composite, SWT.PUSH);
        addAttributeButton.setText("Add Attribute");
        addAttributeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addAttributeButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addAttributeButton.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                AddNewAttributeDialog newAttributeTable = new AddNewAttributeDialog();
                if (newAttributeTable.open() == 0) {
                    localSecondaryIndex.getProjection().getNonKeyAttributes().add(newAttributeTable.getNewAttributeName());
                    attributeList.refresh();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        addAttributeButton.setEnabled(false);

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false)
                    return;
                IStatus status = (IStatus) value;
                if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                    okButton.setEnabled(false);
                } else {
                    setErrorMessage(null);
                    okButton.setEnabled(true);
                }
            }
        });

        bindingContext.updateModels();
        return composite;
    }

    public LocalSecondaryIndex getLocalSecondaryIndex() {
        return localSecondaryIndex;
    }

    private class AddNewAttributeDialog extends AbstractAddNewAttributeDialog {

        @Override
        public void validate() {
            if (getButton(0) == null)
                return;
            if (getNewAttributeName().length() == 0) {
                getButton(0).setEnabled(false);
                return;
            }
            getButton(0).setEnabled(true);
            return;
        }
    }

    // The list for attributes in the projection
    private class AttributeList extends Composite {

        private ListViewer viewer;
        private AttributeListContentProvider attributeListContentProvider;

        public AttributeList(Composite parent) {
            super(parent, SWT.NONE);
            this.setLayout(new GridLayout());
            viewer = new ListViewer(this, SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
            attributeListContentProvider = new AttributeListContentProvider();
            viewer.setContentProvider(attributeListContentProvider);
            viewer.setLabelProvider(new LabelProvider());
            GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getList());
            viewer.getList().setVisible(true);
            MenuManager menuManager = new MenuManager("#PopupMenu");
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener() {

                public void menuAboutToShow(IMenuManager manager) {
                    if (viewer.getList().getSelectionCount() > 0) {

                        manager.add(new Action() {

                            @Override
                            public ImageDescriptor getImageDescriptor() {
                                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                            }

                            @Override
                            public void run() {
                                localSecondaryIndex.getProjection().getNonKeyAttributes().remove(viewer.getList().getSelectionIndex());
                                refresh();
                            }

                            @Override
                            public String getText() {
                                return "Delete Attribute";
                            }
                        });
                    }
                }
            });
            viewer.getList().setMenu(menuManager.createContextMenu(viewer.getList()));
        }

        // Enforce to call getElements to update list
        public void refresh() {
            viewer.setInput(new Object());
        }
    }

    private class AttributeListContentProvider extends ObservableListContentProvider {

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        }

        @Override
        public Object[] getElements(Object inputElement) {
            return localSecondaryIndex.getProjection().getNonKeyAttributes().toArray();
        }
    }
}
