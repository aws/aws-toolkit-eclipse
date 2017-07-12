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

import java.util.Arrays;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

    /** Widget used as data-binding targets **/
    private Text attributeNameText;
    private Text indexNameText;
    private Combo attributeTypeCombo;
    private Combo projectionTypeCombo;
    private Button addAttributeButton;
    private Button okButton;
    
    /** The data objects that will be used to generate the service request **/
    private final LocalSecondaryIndex localSecondaryIndex;
    private final AttributeDefinition indexRangeKeyAttributeDefinition;

    private final DataBindingContext bindingContext = new DataBindingContext();
    
    /** The model value objects for data-binding **/
    private final IObservableValue indexNameModel;
    private final IObservableValue indexRangeKeyNameInKeySchemaDefinitionModel;
    private final IObservableValue indexRangeKeyNameInAttributeDefinitionsModel;
    private final IObservableValue indexRangeKeyAttributeTypeModel;
    private final IObservableValue projectionTypeModel;
    
    private final String primaryRangeKeyName;
    private final int primaryRangeKeyTypeComboIndex;

    private static final String[] DATA_TYPE_STRINGS = new String[] { "String", "Number", "Binary" };
    private static final String[] PROJECTED_ATTRIBUTES = new String[] { "All Attributes", "Table and Index Keys", "Specify Attributes" };

    public AddLSIDialog(Shell parentShell, CreateTableDataModel dataModel) {
        super(parentShell);
        // Initialize the variable necessary for data-binding
        localSecondaryIndex = new LocalSecondaryIndex();
        // The index range key to be defined by the user
        KeySchemaElement rangeKeySchemaDefinition = new KeySchemaElement()
                .withAttributeName(null) 
                .withKeyType(KeyType.RANGE);
        localSecondaryIndex.withKeySchema(
                new KeySchemaElement()
                                .withAttributeName(dataModel.getHashKeyName())
                                .withKeyType(KeyType.HASH),
                rangeKeySchemaDefinition);
        localSecondaryIndex.setProjection(new Projection());
        // The attribute definition for the index range key
        indexRangeKeyAttributeDefinition = new AttributeDefinition();
        
        // Initialize IObservableValue objects that keep track of data variables
        indexNameModel = PojoObservables.observeValue(localSecondaryIndex, "indexName");
        indexRangeKeyNameInKeySchemaDefinitionModel = PojoObservables.observeValue(rangeKeySchemaDefinition, "attributeName");
        indexRangeKeyAttributeTypeModel = PojoObservables.observeValue(indexRangeKeyAttributeDefinition, "attributeType");
        indexRangeKeyNameInAttributeDefinitionsModel = PojoObservables.observeValue(indexRangeKeyAttributeDefinition, "attributeName");
        projectionTypeModel = PojoObservables.observeValue(localSecondaryIndex.getProjection(), "projectionType");
        
        // Get the information of the primary range key
        if (dataModel.getEnableRangeKey()) {
            primaryRangeKeyName = dataModel.getRangeKeyName();
            primaryRangeKeyTypeComboIndex = Arrays.<String>asList(DATA_TYPE_STRINGS).indexOf(dataModel.getRangeKeyType());
        } else {
            primaryRangeKeyName = null;
            primaryRangeKeyTypeComboIndex = -1;
        }

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

        // Index range key attribute name
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Attribute to Index:");
        attributeNameText = new Text(composite, SWT.BORDER);
        bindingContext.bindValue(SWTObservables.observeText(attributeNameText, SWT.Modify), indexRangeKeyNameInKeySchemaDefinitionModel);
        ChainValidator<String> attributeNameValidationStatusProvider = new ChainValidator<>(indexRangeKeyNameInKeySchemaDefinitionModel, new NotEmptyValidator("Please provide an attribute name"));
        bindingContext.addValidationStatusProvider(attributeNameValidationStatusProvider);
        bindingContext.bindValue(SWTObservables.observeText(attributeNameText, SWT.Modify), indexRangeKeyNameInAttributeDefinitionsModel);
        attributeNameText.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                if (attributeNameText.getText().equals(primaryRangeKeyName)
                        && attributeTypeCombo != null
                        && primaryRangeKeyTypeComboIndex > -1) {
                    attributeTypeCombo.select(primaryRangeKeyTypeComboIndex);
                    attributeTypeCombo.setEnabled(false);
                } else if (attributeTypeCombo != null) {
                    attributeTypeCombo.setEnabled(true);
                }
                
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(attributeNameText);

        // Index range key attribute type
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Attribute Type:");
        attributeTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        attributeTypeCombo.setItems(DATA_TYPE_STRINGS);
        attributeTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(attributeTypeCombo), indexRangeKeyAttributeTypeModel);

        // Index name
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Index Name:");
        indexNameText = new Text(composite, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indexNameText);
        bindingContext.bindValue(SWTObservables.observeText(indexNameText, SWT.Modify), indexNameModel);
        ChainValidator<String> indexNameValidationStatusProvider = new ChainValidator<>(indexNameModel, new NotEmptyValidator("Please provide an index name"));
        bindingContext.addValidationStatusProvider(indexNameValidationStatusProvider);

        // Projection type
        new Label(composite, SWT.NONE | SWT.READ_ONLY).setText("Projected Attributes:");
        projectionTypeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        projectionTypeCombo.setItems(PROJECTED_ATTRIBUTES);
        projectionTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(projectionTypeCombo), projectionTypeModel);
        projectionTypeCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (projectionTypeCombo.getSelectionIndex() == 2) {
                    // Enable the list for adding non-key attributes to the projection
                    addAttributeButton.setEnabled(true);
                } else {
                    addAttributeButton.setEnabled(false);
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        // Non-key attributes in the projection
        final AttributeList attributeList = new AttributeList(composite);
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(attributeList);
        addAttributeButton = new Button(composite, SWT.PUSH);
        addAttributeButton.setText("Add Attribute");
        addAttributeButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        addAttributeButton.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        addAttributeButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                AddNewAttributeDialog newAttributeTable = new AddNewAttributeDialog();
                if (newAttributeTable.open() == 0) {
                    // lazy-initialize the list
                    if (null == localSecondaryIndex.getProjection().getNonKeyAttributes()) {
                        localSecondaryIndex.getProjection().setNonKeyAttributes(new LinkedList<String>());
                    }
                    localSecondaryIndex.getProjection().getNonKeyAttributes().add(newAttributeTable.getNewAttributeName());
                    attributeList.refresh();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        addAttributeButton.setEnabled(false);

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false)
                    return;
                IStatus status = (IStatus) value;
                if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                    if (okButton != null) {
                        okButton.setEnabled(false);
                    }
                } else {
                    setErrorMessage(null);
                    if (okButton != null) {
                        okButton.setEnabled(true);
                    }
                }
            }
        });

        bindingContext.updateModels();
        return composite;
    }

    public LocalSecondaryIndex getLocalSecondaryIndex() {
        return localSecondaryIndex;
    }

    /**
     * Get the AttributeDefinition of the index range key as specified in this dialog.
     */
    public AttributeDefinition getIndexRangeKeyAttributeDefinition() {
        return indexRangeKeyAttributeDefinition;
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

    /** The list widget for adding projected non-key attributes. **/
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

                @Override
                public void menuAboutToShow(IMenuManager manager) {
                    if (viewer.getList().getSelectionCount() > 0) {

                        manager.add(new Action() {

                            @Override
                            public ImageDescriptor getImageDescriptor() {
                                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                            }

                            @Override
                            public void run() {
                                // In theory, this should never be null.
                                if (null != localSecondaryIndex.getProjection().getNonKeyAttributes()) {
                                    localSecondaryIndex.getProjection().getNonKeyAttributes().remove(viewer.getList().getSelectionIndex());
                                }
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
            return localSecondaryIndex.getProjection().getNonKeyAttributes() != null ? 
                        localSecondaryIndex.getProjection().getNonKeyAttributes().toArray()
                        : new String[] {};
        }
    }
}
