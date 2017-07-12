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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.databinding.RangeValidator;
import com.amazonaws.eclipse.dynamodb.AbstractAddNewAttributeDialog;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

public class AddGSIDialog extends TitleAreaDialog {

    /** Widget used as data-binding targets **/
    private Text indexHashKeyNameText;
    private Combo indexHashKeyAttributeTypeCombo;
    private Button enableIndexRangeKeyButton;
    private Text indexRangeKeyNameText;
    private Combo indexRangeKeyAttributeTypeCombo;
    private Text indexNameText;
    private Combo projectionTypeCombo;
    private Button addAttributeButton;
    private Button okButton;
    
    /** The data objects that will be used to generate the service request **/
    private final GlobalSecondaryIndex globalSecondaryIndex;
    private final KeySchemaElement indexRangeKeySchemaDefinition = new KeySchemaElement()
            .withAttributeName(null).withKeyType(KeyType.RANGE);
    private final AttributeDefinition indexHashKeyAttributeDefinition = new AttributeDefinition();
    private final AttributeDefinition indexRangeKeyAttributeDefinition = new AttributeDefinition();
    
    private boolean enableIndexRangeKey = false;

    private final DataBindingContext bindingContext = new DataBindingContext();
    
    /** The model value objects for data-binding **/
    private final IObservableValue indexNameModel;
    private final IObservableValue indexHashKeyNameInKeySchemaDefinitionModel;
    private final IObservableValue indexHashKeyNameInAttributeDefinitionsModel;
    private final IObservableValue indexHashKeyAttributeTypeModel;
    private final IObservableValue enableIndexRangeKeyModel;
    private final IObservableValue indexRangeKeyNameInKeySchemaDefinitionModel;
    private final IObservableValue indexRangeKeyNameInAttributeDefinitionsModel;
    private final IObservableValue indexRangeKeyAttributeTypeModel;
    private final IObservableValue projectionTypeModel;
    private final IObservableValue readCapacityModel;
    private final IObservableValue writeCapacityModel;
    
    /** The map from each primary key name to the combo index of its attribute type **/
    private final Map<String, Integer> primaryKeyTypes = new HashMap<>();

    private Font italicFont;
    private static final long CAPACITY_UNIT_MINIMUM = 1;
    private static final String[] DATA_TYPE_STRINGS = new String[] { "String", "Number", "Binary" };
    private static final String[] PROJECTED_ATTRIBUTES = new String[] { "All Attributes", "Table and Index Keys", "Specify Attributes" };

    public AddGSIDialog(Shell parentShell, CreateTableDataModel dataModel) {
        super(parentShell);
        // Initialize the variable necessary for data-binding
        globalSecondaryIndex = new GlobalSecondaryIndex();
        // The index hash key to be defined by the user
        KeySchemaElement indexHashKeySchemaDefinition = new KeySchemaElement()
                .withAttributeName(null).withKeyType(KeyType.HASH);
        globalSecondaryIndex.withKeySchema(indexHashKeySchemaDefinition);
        globalSecondaryIndex.setProjection(new Projection());
        globalSecondaryIndex.setProvisionedThroughput(new ProvisionedThroughput());
        
        // Initialize IObservableValue objects that keep track of data variables
        indexNameModel = PojoObservables.observeValue(globalSecondaryIndex, "indexName");
        indexHashKeyNameInKeySchemaDefinitionModel = PojoObservables.observeValue(indexHashKeySchemaDefinition, "attributeName");
        indexHashKeyAttributeTypeModel = PojoObservables.observeValue(indexHashKeyAttributeDefinition, "attributeType");
        indexHashKeyNameInAttributeDefinitionsModel = PojoObservables.observeValue(indexHashKeyAttributeDefinition, "attributeName");
        
        enableIndexRangeKeyModel = PojoObservables.observeValue(this, "enableIndexRangeKey");
        indexRangeKeyNameInKeySchemaDefinitionModel = PojoObservables.observeValue(indexRangeKeySchemaDefinition, "attributeName");
        indexRangeKeyAttributeTypeModel = PojoObservables.observeValue(indexRangeKeyAttributeDefinition, "attributeType");
        indexRangeKeyNameInAttributeDefinitionsModel = PojoObservables.observeValue(indexRangeKeyAttributeDefinition, "attributeName");
        projectionTypeModel = PojoObservables.observeValue(globalSecondaryIndex.getProjection(), "projectionType");
        
        readCapacityModel = PojoObservables.observeValue(globalSecondaryIndex.getProvisionedThroughput(), "readCapacityUnits");
        writeCapacityModel = PojoObservables.observeValue(globalSecondaryIndex.getProvisionedThroughput(), "writeCapacityUnits");
        
        // Get the information of the primary keys
        String primaryHashKeyName = dataModel.getHashKeyName();
        int primaryHashKeyTypeComboIndex = Arrays.<String>asList(DATA_TYPE_STRINGS).indexOf(dataModel.getHashKeyType());
        primaryKeyTypes.put(primaryHashKeyName, primaryHashKeyTypeComboIndex);
        if (dataModel.getEnableRangeKey()) {
            String primaryRangeKeyName = dataModel.getRangeKeyName();
            int primaryRangeKeyTypeComboIndex = Arrays.<String>asList(DATA_TYPE_STRINGS).indexOf(dataModel.getRangeKeyType());
            primaryKeyTypes.put(primaryRangeKeyName, primaryRangeKeyTypeComboIndex);
        }

        setShellStyle(SWT.RESIZE);

    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle("Add Global Secondary Index");
        setTitleImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_LOGO));
        okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(false);
        return contents;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Add Global Secondary Index");
        shell.setMinimumSize(400, 700);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout());
        composite = new Composite(composite, SWT.NULL);

        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout(2, false));

        // Index hash key attribute name
        Group indexHashKeyGroup = CreateTablePageUtil.newGroup(composite, "Index Hash Key", 2);
        new Label(indexHashKeyGroup, SWT.NONE | SWT.READ_ONLY).setText("Index Hash Key Name:");
        indexHashKeyNameText = new Text(indexHashKeyGroup, SWT.BORDER);
        bindingContext.bindValue(SWTObservables.observeText(indexHashKeyNameText, SWT.Modify), indexHashKeyNameInKeySchemaDefinitionModel);
        ChainValidator<String> indexHashKeyNameValidationStatusProvider = new ChainValidator<>(indexHashKeyNameInKeySchemaDefinitionModel, new NotEmptyValidator("Please provide the index hash key name"));
        bindingContext.addValidationStatusProvider(indexHashKeyNameValidationStatusProvider);
        bindingContext.bindValue(SWTObservables.observeText(indexHashKeyNameText, SWT.Modify), indexHashKeyNameInAttributeDefinitionsModel);
        indexHashKeyNameText.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                if (primaryKeyTypes.containsKey(indexHashKeyNameText.getText())
                        && indexHashKeyAttributeTypeCombo != null
                        && primaryKeyTypes.get(indexHashKeyNameText.getText()) > -1) {
                    indexHashKeyAttributeTypeCombo.select(primaryKeyTypes.get(indexHashKeyNameText.getText()));
                    indexHashKeyAttributeTypeCombo.setEnabled(false);
                } else if (indexHashKeyAttributeTypeCombo != null) {
                    indexHashKeyAttributeTypeCombo.setEnabled(true);
                }
                
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indexHashKeyNameText);

        // Index hash key attribute type
        new Label(indexHashKeyGroup, SWT.NONE | SWT.READ_ONLY).setText("Index Hash Key Type:");
        indexHashKeyAttributeTypeCombo = new Combo(indexHashKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        indexHashKeyAttributeTypeCombo.setItems(DATA_TYPE_STRINGS);
        indexHashKeyAttributeTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(indexHashKeyAttributeTypeCombo), indexHashKeyAttributeTypeModel);
        
        Group indexRangeKeyGroup = CreateTablePageUtil.newGroup(composite, "Index Range Key", 2);
        // Enable index range key button
        enableIndexRangeKeyButton = new Button(indexRangeKeyGroup, SWT.CHECK);
        enableIndexRangeKeyButton.setText("Enable Index Range Key");
        GridDataFactory.fillDefaults().span(2, 1).applyTo(enableIndexRangeKeyButton);
        bindingContext.bindValue(SWTObservables.observeSelection(enableIndexRangeKeyButton), enableIndexRangeKeyModel);
        
        // Index range key attribute name
        final Label indexRangeKeyAttributeLabel = new Label(indexRangeKeyGroup, SWT.NONE | SWT.READ_ONLY);
        indexRangeKeyAttributeLabel.setText("Index Range Key Name:");
        indexRangeKeyNameText = new Text(indexRangeKeyGroup, SWT.BORDER);
        bindingContext.bindValue(SWTObservables.observeText(indexRangeKeyNameText, SWT.Modify), indexRangeKeyNameInKeySchemaDefinitionModel);
        ChainValidator<String> indexRangeKeyNameValidationStatusProvider = new ChainValidator<>(
                indexRangeKeyNameInKeySchemaDefinitionModel,
                enableIndexRangeKeyModel,
                new NotEmptyValidator("Please provide the index range key name"));
        bindingContext.addValidationStatusProvider(indexRangeKeyNameValidationStatusProvider);
        bindingContext.bindValue(SWTObservables.observeText(indexRangeKeyNameText, SWT.Modify), indexRangeKeyNameInAttributeDefinitionsModel);
        indexRangeKeyNameText.addModifyListener(new ModifyListener() {
            
            @Override
            public void modifyText(ModifyEvent e) {
                if (primaryKeyTypes.containsKey(indexRangeKeyNameText.getText())
                        && indexRangeKeyAttributeTypeCombo != null
                        && primaryKeyTypes.get(indexRangeKeyNameText.getText()) > -1) {
                    indexRangeKeyAttributeTypeCombo.select(primaryKeyTypes.get(indexRangeKeyNameText.getText()));
                    indexRangeKeyAttributeTypeCombo.setEnabled(false);
                } else if (indexRangeKeyAttributeTypeCombo != null) {
                    indexRangeKeyAttributeTypeCombo.setEnabled(true);
                }
                
            }
        });
        GridDataFactory.fillDefaults().grab(true, false).applyTo(indexRangeKeyNameText);

        // Index range key attribute type
        final Label indexRangeKeyTypeLabel = new Label(indexRangeKeyGroup, SWT.NONE | SWT.READ_ONLY);
        indexRangeKeyTypeLabel.setText("Index Range Key Type:");
        indexRangeKeyAttributeTypeCombo = new Combo(indexRangeKeyGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        indexRangeKeyAttributeTypeCombo.setItems(DATA_TYPE_STRINGS);
        indexRangeKeyAttributeTypeCombo.select(0);
        bindingContext.bindValue(SWTObservables.observeSelection(indexRangeKeyAttributeTypeCombo), indexRangeKeyAttributeTypeModel);

        enableIndexRangeKeyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                enableIndexRangeKey = enableIndexRangeKeyButton.getSelection();
                indexRangeKeyAttributeLabel.setEnabled(enableIndexRangeKey);
                indexRangeKeyNameText.setEnabled(enableIndexRangeKey);
                indexRangeKeyTypeLabel.setEnabled(enableIndexRangeKey);
                indexRangeKeyAttributeTypeCombo.setEnabled(enableIndexRangeKey);
            }
        });
        enableIndexRangeKeyButton.setSelection(false);
        indexRangeKeyAttributeLabel.setEnabled(false);
        indexRangeKeyNameText.setEnabled(false);
        indexRangeKeyTypeLabel.setEnabled(false);
        indexRangeKeyAttributeTypeCombo.setEnabled(false);
        
        // Index name
        Label indexNameLabel = new Label(composite, SWT.NONE | SWT.READ_ONLY);
        indexNameLabel.setText("Index Name:");
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
                    if (null == globalSecondaryIndex.getProjection().getNonKeyAttributes()) {
                        globalSecondaryIndex.getProjection().setNonKeyAttributes(new LinkedList<String>());
                    }
                    globalSecondaryIndex.getProjection().getNonKeyAttributes().add(newAttributeTable.getNewAttributeName());
                    attributeList.refresh();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        addAttributeButton.setEnabled(false);
        
        // GSI throughput
        FontData[] fontData = indexNameLabel.getFont().getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        
        Group throughputGroup = CreateTablePageUtil.newGroup(composite, "Global Secondary Index Throughput", 3);
        new Label(throughputGroup, SWT.READ_ONLY).setText("Read Capacity Units:");
        final Text readCapacityText = CreateTablePageUtil.newTextField(throughputGroup);
        readCapacityText.setText("" + CAPACITY_UNIT_MINIMUM);
        bindingContext.bindValue(SWTObservables.observeText(readCapacityText, SWT.Modify), readCapacityModel);
        ChainValidator<Long> readCapacityValidationStatusProvider = new ChainValidator<>(
                readCapacityModel, new RangeValidator(
                        "Please enter a read capacity of " + CAPACITY_UNIT_MINIMUM + " or more.", CAPACITY_UNIT_MINIMUM,
                        Long.MAX_VALUE));
        bindingContext.addValidationStatusProvider(readCapacityValidationStatusProvider);

        Label minimumReadCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
        minimumReadCapacityLabel.setText("(Minimum capacity " + CAPACITY_UNIT_MINIMUM + ")");
        minimumReadCapacityLabel.setFont(italicFont);

        new Label(throughputGroup, SWT.READ_ONLY).setText("Write Capacity Units:");
        final Text writeCapacityText = CreateTablePageUtil.newTextField(throughputGroup);
        writeCapacityText.setText("" + CAPACITY_UNIT_MINIMUM);
        Label minimumWriteCapacityLabel = new Label(throughputGroup, SWT.READ_ONLY);
        minimumWriteCapacityLabel.setText("(Minimum capacity " + CAPACITY_UNIT_MINIMUM + ")");
        minimumWriteCapacityLabel.setFont(italicFont);
        bindingContext.bindValue(SWTObservables.observeText(writeCapacityText, SWT.Modify), writeCapacityModel);
        ChainValidator<Long> writeCapacityValidationStatusProvider = new ChainValidator<>(
                writeCapacityModel, new RangeValidator(
                        "Please enter a write capacity of " + CAPACITY_UNIT_MINIMUM + " or more.", CAPACITY_UNIT_MINIMUM,
                        Long.MAX_VALUE));
        bindingContext.addValidationStatusProvider(writeCapacityValidationStatusProvider);

        final Label throughputCapacityLabel = new Label(throughputGroup, SWT.WRAP);
        throughputCapacityLabel
                .setText("This throughput is separate from and in addition to the primary table's throughput.");
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 3;
        gridData.widthHint = 200;
        throughputCapacityLabel.setLayoutData(gridData);
        throughputCapacityLabel.setFont(italicFont);

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

    /** This method should only be called once by the parent wizard. **/
    public GlobalSecondaryIndex getGlobalSecondaryIndex() {
        if (enableIndexRangeKey) {
            globalSecondaryIndex.getKeySchema().add(indexRangeKeySchemaDefinition);
        }
        return globalSecondaryIndex;
    }

    /**
     * Returns an unmodifiable list of all the AttributeDefinition of the index keys associated with this GSI.
     */
    public List<AttributeDefinition> getIndexKeyAttributeDefinitions() {
        List<AttributeDefinition> keyAttrs = new LinkedList<>();
        keyAttrs.add(indexHashKeyAttributeDefinition);
        if (isEnableIndexRangeKey() ) {
            keyAttrs.add(indexRangeKeyAttributeDefinition);
        }
        return Collections.unmodifiableList(keyAttrs);
    }

    public boolean isEnableIndexRangeKey() {
        return enableIndexRangeKey;
    }

    public void setEnableIndexRangeKey(boolean enableIndexRangeKey) {
        this.enableIndexRangeKey = enableIndexRangeKey;
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
                                if (null != globalSecondaryIndex.getProjection().getNonKeyAttributes()) {
                                    globalSecondaryIndex.getProjection().getNonKeyAttributes().remove(viewer.getList().getSelectionIndex());
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
            return globalSecondaryIndex.getProjection().getNonKeyAttributes() != null ? 
                        globalSecondaryIndex.getProjection().getNonKeyAttributes().toArray()
                        : new String[] {};
        }
    }
}
