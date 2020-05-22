/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.editor;

import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueEditor.NUMBER;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueEditor.STRING;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.N;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.NS;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.S;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.SS;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.format;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.getDataType;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.getValuesFromAttribute;
import static com.amazonaws.eclipse.dynamodb.editor.AttributeValueUtil.setAttribute;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.ui.AbstractTableLabelProvider;
import com.amazonaws.eclipse.dynamodb.AbstractAddNewAttributeDialog;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;

/**
 * Scan editor for DynamoDB tables.
 */
public class DynamoDBTableEditor extends EditorPart {

    private static final String[] exportExtensions = new String[] { "*.csv" };

    /*
     * SWT editor glue
     */
    public static final String ID = "com.amazonaws.eclipse.dynamodb.editor.tableEditor";
    private TableEditorInput tableEditorInput;
    private boolean dirty;

    /*
     * Large-scale UI elements
     */
    private ToolBarManager toolBarManager;
    private ToolBar toolBar;
    private TableViewer viewer;
    private ContentProvider contentProvider;

    /*
     * Data model for UI: list of scan conditions assembled by the user and a
     * set of items they've edited, added and deleted.
     */
    private final List<ScanConditionRow> scanConditions = new LinkedList<>();
    private final EditedItems editedItems = new EditedItems();
    private final Collection<Map<String, AttributeValue>> deletedItems = new LinkedList<>();
    private final Collection<Map<String, AttributeValue>> addedItems = new LinkedList<>();

    /*
     * Table info that we fetch and store
     */
    private KeySchemaWithAttributeType tableKey;
    final Set<String> knownAttributes = new HashSet<>();
    private ScanResult scanResult = new ScanResult();

    /*
     * Actions to enable and disable
     */
    private Action runScanAction;
    private Action saveAction;
    private Action nextPageResultsAction;
    private Action exportAsCSVAction;
    private Action addNewAttributeAction;

    @Override
    public void doSave(IProgressMonitor monitor) {

        monitor.beginTask("Saving changes", editedItems.size() + deletedItems.size());

        try{
            AmazonDynamoDB dynamoDBClient = AwsToolkitCore.getClientFactory(tableEditorInput.getAccountId())
                    .getDynamoDBV2Client();

            /*
             * Save all edited items, only touching edited attributes.
             */
            if ( !editedItems.isEmpty() ) {
                for ( Iterator<Entry<Map<String, AttributeValue>, EditedItem>> iter = editedItems.iterator(); iter.hasNext(); ) {
                    Entry<Map<String, AttributeValue>, EditedItem> editedItem = iter.next();

                    try {
                        /*
                         * Due to a bug in Dynamo, updateItem will not create a
                         * new item when only the key is specified. Therefore,
                         * we need two code paths here, as in
                         * DynamoDBMapper.save().
                         */
                        if ( editedItem.getValue().getEditedAttributes().isEmpty() ) {
                            PutItemRequest rq = new PutItemRequest().withTableName(tableEditorInput.getTableName());
                            rq.setItem(editedItem.getValue().getAttributes());
                            Map<String, ExpectedAttributeValue> expected = new HashMap<>();
                            for ( String attr : editedItem.getValue().getAttributes().keySet() ) {
                                expected.put(attr, new ExpectedAttributeValue().withExists(false));
                            }
                            rq.setExpected(expected);

                            dynamoDBClient.putItem(rq);

                        } else {
                            UpdateItemRequest rq = new UpdateItemRequest().withTableName(tableEditorInput
                                    .getTableName());
                            rq.setKey(editedItem.getKey());
                            Map<String, AttributeValueUpdate> values = new HashMap<>();
                            for ( String attributeName : editedItem.getValue().getEditedAttributes() ) {
                                AttributeValueUpdate update = new AttributeValueUpdate();
                                AttributeValue attributeValue = editedItem.getValue().getAttributes()
                                        .get(attributeName);
                                if ( attributeValue == null ) {
                                    update.setAction(AttributeAction.DELETE);
                                } else {
                                    update.setAction(AttributeAction.PUT);
                                    update.setValue(attributeValue);
                                }
                                values.put(attributeName, update);
                            }

                            rq.setAttributeUpdates(values);

                            dynamoDBClient.updateItem(rq);
                        }

                        for ( int col = 0; col < viewer.getTable().getColumnCount(); col++ ) {
                            editedItem.getValue().getTableItem()
                                    .setForeground(col, Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
                        }
                        iter.remove();
                        monitor.worked(1);

                    } catch ( AmazonClientException e ) {
                        StatusManager.getManager().handle(
                                new Status(IStatus.ERROR, DynamoDBPlugin.PLUGIN_ID, "Error saving item with key "
                                        + editedItem.getKey() + ": " + e.getMessage()), StatusManager.SHOW);
                        return;
                    }
                }
            }

            /*
             * Delete all deleted items.
             */
            if ( !deletedItems.isEmpty() ) {
                for ( Iterator<Map<String, AttributeValue>> iter = deletedItems.iterator(); iter.hasNext(); ) {
                    Map<String, AttributeValue> deletedItem = iter.next();
                    try {
                        dynamoDBClient.deleteItem(new DeleteItemRequest()
                                .withTableName(tableEditorInput.getTableName()).withKey(deletedItem));
                    } catch ( AmazonClientException e ) {
                        StatusManager.getManager().handle(
                                new Status(IStatus.ERROR, DynamoDBPlugin.PLUGIN_ID, "Error deleting item with key "
                                        + deletedItem + ": " + e.getMessage()), StatusManager.SHOW);
                        return;
                    }
                    iter.remove();
                    monitor.worked(1);
                }
            }

        /*
         * Exception handling: if we fail to execute any action above, the
         * editor is left in a sane state -- we clean up edited state as we
         * make each service call, so all we have to do is notify of the
         * exception and return without updating the editor's dirty state.
         */
        } finally {
            monitor.done();
        }

        dirty = false;
        this.saveAction.setEnabled(false);
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    public void doSaveAs() {
        // unsupported
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        this.tableEditorInput = (TableEditorInput) input;
        setPartName(input.getName());
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        dirty = true;
        saveAction.setEnabled(true);
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite composite) {
        composite.setLayout(new FormLayout());

        // Create the sash first, so the other controls
        // can be attached to it.
        final Sash sash = new Sash(composite, SWT.HORIZONTAL);
        FormData data = new FormData();
        // Initial position is a quarter of the way down the composite
        data.top = new FormAttachment(25, 0);
        // And filling 100% of horizontal space
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);

        sash.setLayoutData(data);
        sash.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                // Move the sash to its new position and redraw it
                ((FormData) sash.getLayoutData()).top = new FormAttachment(0, event.y);
                sash.getParent().layout();
            }
        });

        this.toolBarManager = new ToolBarManager(SWT.LEFT);
        this.toolBar = this.toolBarManager.createControl(composite);

        Composite scanEditor = createScanEditor(composite);

        data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(scanEditor, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        this.toolBar.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(this.toolBar, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        scanEditor.setLayoutData(data);

        // Results table is attached to the top of the sash
        Composite resultsComposite = new Composite(composite, SWT.BORDER);
        data = new FormData();
        data.top = new FormAttachment(sash, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        resultsComposite.setLayoutData(data);

        createResultsTable(resultsComposite);

        createActions();

        // initialize the table with results
        runScan();
    }

    /**
     * Creates the composite to edit scan requests
     */
    private Composite createScanEditor(Composite composite) {
        final Composite scanEditor = new Composite(composite, SWT.None);
        GridLayoutFactory.fillDefaults().applyTo(scanEditor);

        final Button addCondition = new Button(scanEditor, SWT.PUSH);
        addCondition.setToolTipText("Add scan condition");
        addCondition.setText("Add scan condition");
        addCondition.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ADD));
        GridDataFactory.swtDefaults().indent(5, 0).applyTo(addCondition);

        // Selection listener creates a new row in the editor, then moves the
        // button below this new row.
        addCondition.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                scanEditor.getParent().setRedraw(false);
                ScanConditionRow scanConditionRow = new ScanConditionRow(scanEditor, knownAttributes);
                scanConditions.add(scanConditionRow);
                addCondition.moveBelow(scanConditionRow);
                scanEditor.pack(true);
                scanEditor.getParent().layout(true);
                scanEditor.getParent().setRedraw(true);
            }
        });

        return scanEditor;
    }

    private void createActions() {
        runScanAction = new AwsAction(AwsToolkitMetricType.EXPLORER_DYNAMODB_RUN_SCAN) {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_START);
            }

            @Override
            public String getText() {
                return "Run scan";
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            protected void doRun() {
                runScan();
                actionFinished();
            }

            @Override
            public String getActionDefinitionId() {
                return "com.amazonaws.eclipse.dynamodb.editor.runScan";
            }
        };

        IHandler handler = new ActionHandler(runScanAction);
        IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
        handlerService.activateHandler(runScanAction.getActionDefinitionId(), handler);

        saveAction = new AwsAction(AwsToolkitMetricType.EXPLORER_DYNAMODB_SAVE) {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT);
            }

            @Override
            public String getText() {
                return "Save changes to Dynamo";
            }

            @Override
            protected void doRun() {
                getEditorSite().getPage().saveEditor(DynamoDBTableEditor.this, false);
            }

            @Override
            public String getActionDefinitionId() {
                return "org.eclipse.ui.file.save";
            }
        };

        handlerService.activateHandler(saveAction.getActionDefinitionId(), new ActionHandler(saveAction));

        nextPageResultsAction = new AwsAction() {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return DynamoDBPlugin.getDefault().getImageRegistry().getDescriptor(DynamoDBPlugin.IMAGE_NEXT_RESULTS);
            }

            @Override
            public String getText() {
                return "Next page of results";
            }

            @Override
            protected void doRun() {
                getNextPageResults();
                actionFinished();
            }

        };

        exportAsCSVAction = new AwsAction(AwsToolkitMetricType.EXPLORER_DYNAMODB_EXPORT_AS_CSV) {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_EXPORT);
            }

            @Override
            public String getText() {
                return "Export as CSV";
            }

            @Override
            protected void doRun() {
                FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
                dialog.setOverwrite(true);
                dialog.setFilterExtensions(exportExtensions);
                String csvFile = dialog.open();
                if (csvFile != null) {
                    writeCsvFile(csvFile);
                } else {
                    actionCanceled();
                }
                actionFinished();
            }

            private void writeCsvFile(final String csvFile) {
                try {
                    // truncate file before writing
                    try (RandomAccessFile raf = new RandomAccessFile(new File(csvFile), "rw")) {
                        raf.setLength(0L);
                    }

                    List<Map<String, AttributeValue>> items = new LinkedList<>();

                    for ( TableItem tableItem : viewer.getTable().getItems() ) {
                        @SuppressWarnings("unchecked")
                        Map<String, AttributeValue> e = (Map<String, AttributeValue>) tableItem.getData();
                        items.add(e);
                    }

                    try (BufferedWriter out = new BufferedWriter(new FileWriter(csvFile))) {
                        boolean seenOne = false;
                        for (String col : contentProvider.getColumns()) {
                            if ( seenOne ) {
                                out.write(",");
                            } else {
                                seenOne = true;
                            }
                            out.write(col);
                        }
                        out.write("\n");

                        for ( Map<String, AttributeValue> item : items ) {
                            seenOne = false;
                            for (String col : contentProvider.getColumns()) {
                                if (seenOne) {
                                    out.write(",");
                                } else {
                                    seenOne = true;
                                }
                                AttributeValue values = item.get(col);
                                if (values != null) {
                                    String value = format(values);
                                    // For csv files, we need to quote all values and escape all quotes
                                    value = value.replaceAll("\"", "\"\"");
                                    value = "\"" + value + "\"";
                                    out.write(value);
                                }
                            }
                            out.write("\n");
                        }
                    }
                    actionSucceeded();

                } catch (Exception e) {
                    actionFailed();
                    AwsToolkitCore.getDefault().logError("Couldn't save CSV file", e);
                }
            }

        };

        addNewAttributeAction = new AwsAction(AwsToolkitMetricType.EXPLORER_DYNAMODB_ADD_NEW_ATTRIBUTE) {

            @Override
            public ImageDescriptor getImageDescriptor() {
                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD);
            }

            @Override
            public String getText() {
                return "Add attribute column";
            }

            @Override
            protected void doRun() {
                NewAttributeDialog dialog = new NewAttributeDialog();
                if ( dialog.open() == 0 ) {
                    contentProvider.columns.add(dialog.getNewAttributeName());
                    contentProvider.createColumn(viewer.getTable(), (TableColumnLayout) viewer.getTable().getParent()
                            .getLayout(), dialog.getNewAttributeName());
                    viewer.getTable().getParent().layout();
                    actionSucceeded();
                } else {
                    actionCanceled();
                }
                actionFinished();
            }

            final class NewAttributeDialog extends AbstractAddNewAttributeDialog {

                @Override
                public void validate() {
                    if ( getButton(0) == null )
                        return;
                    if ( getNewAttributeName().length() == 0 || contentProvider.columns.contains(getNewAttributeName()) ) {
                        getButton(0).setEnabled(false);
                        return;
                    }

                    getButton(0).setEnabled(true);
                    return;
                }

            }
        };

        runScanAction.setEnabled(false);
        saveAction.setEnabled(false);
        nextPageResultsAction.setEnabled(false);
        exportAsCSVAction.setEnabled(false);
        addNewAttributeAction.setEnabled(false);

        toolBarManager.add(runScanAction);
        toolBarManager.add(nextPageResultsAction);
        toolBarManager.add(saveAction);
        toolBarManager.add(exportAsCSVAction);
        toolBarManager.add(addNewAttributeAction);
        toolBarManager.update(true);
    }

    private void createResultsTable(Composite resultsComposite) {
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        resultsComposite.setLayout(tableColumnLayout);

        this.viewer = new TableViewer(resultsComposite);
        this.viewer.getTable().setLinesVisible(true);
        this.viewer.getTable().setHeaderVisible(true);

        this.contentProvider = new ContentProvider();
        this.viewer.setContentProvider(this.contentProvider);
        this.viewer.setLabelProvider(new LabelProvider());

        final Table table = this.viewer.getTable();

        final TableEditor editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;

        final TextCellEditorListener listener = new TextCellEditorListener(table, editor);
        table.addListener(SWT.MouseUp, listener);
        table.addListener(SWT.FocusOut, listener);
        table.addListener(SWT.KeyDown, listener);

        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if ( table.getSelectionCount() > 0 ) {

                    manager.add(new Action() {

                        @Override
                        public ImageDescriptor getImageDescriptor() {
                            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE);
                        }

                        @Override
                        public void run() {
                            listener.deleteItems();
                        }

                        @Override
                        public String getText() {
                            if ( table.getSelectionCount() == 1 ) {
                                return "Delete selected item";
                            } else {
                                return "Delete selected items";
                            }
                        }

                    });
                }
            }
        });
        table.setMenu(menuManager.createContextMenu(table));
    }

    @Override
    public void setFocus() {
        // no-op
    }

    /**
     * Updates the query results asynchronously. Must be called from the UI
     * thread.
     */
    private void runScan() {

        // Clear out the existing table and edit states
        this.viewer.getTable().setEnabled(false);
        runScanAction.setEnabled(false);
        nextPageResultsAction.setEnabled(false);
        exportAsCSVAction.setEnabled(false);
        addNewAttributeAction.setEnabled(false);
        editedItems.clear();
        deletedItems.clear();

        // this.exportAsCSV.setEnabled(false);
        for ( TableColumn col : this.viewer.getTable().getColumns() ) {
            col.dispose();
        }

        new Thread() {

            @Override
            public void run() {
                if ( tableKey == null ) {
                    DescribeTableResult describeTable = AwsToolkitCore
                            .getClientFactory(DynamoDBTableEditor.this.tableEditorInput.getAccountId())
                            .getDynamoDBV2Client()
                            .describeTable(new DescribeTableRequest().withTableName(tableEditorInput.getTableName()));
                    TableDescription tableDescription = describeTable.getTable();
                    tableKey = convertToKeySchemaWithAttributeType(tableDescription);
                }

                scanResult = new ScanResult();
                try {
                    ScanRequest scanRequest = new ScanRequest().withTableName(tableEditorInput.getTableName());
                    scanRequest.setScanFilter(new HashMap<String, Condition>());
                    for ( ScanConditionRow row : scanConditions ) {
                        if ( row.shouldExecute() ) {
                            scanRequest.getScanFilter().put(row.getAttributeName(), row.getScanCondition());
                        }
                    }
                    scanResult = AwsToolkitCore.getClientFactory(DynamoDBTableEditor.this.tableEditorInput.getAccountId())
                            .getDynamoDBV2Client().scan(scanRequest);
                } catch ( Exception e ) {
                    DynamoDBPlugin.getDefault().reportException(e.getMessage(), e);
                    return;
                }

                final ScanResult result = scanResult;
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        viewer.setInput(result.getItems());
                        viewer.getTable().setEnabled(true);
                        viewer.getTable().getParent().layout();
                        runScanAction.setEnabled(true);
                        nextPageResultsAction.setEnabled(scanResult.getLastEvaluatedKey() != null);
                        exportAsCSVAction.setEnabled(true);
                        addNewAttributeAction.setEnabled(true);
                    }
                });
            }

        }.start();
    }

    /**
     * Fetches the next page of results from the scan and updates the table with them.
     */
    private void getNextPageResults() {

        this.viewer.getTable().setEnabled(false);
        runScanAction.setEnabled(false);
        nextPageResultsAction.setEnabled(false);
        exportAsCSVAction.setEnabled(false);

        new Thread() {

            @Override
            public void run() {

                try {
                    ScanRequest scanRequest = new ScanRequest().withTableName(tableEditorInput.getTableName());
                    scanRequest.setScanFilter(new HashMap<String, Condition>());
                    for ( ScanConditionRow row : scanConditions ) {
                        if ( row.shouldExecute() ) {
                            scanRequest.getScanFilter().put(row.getAttributeName(), row.getScanCondition());
                        }
                    }
                    scanRequest.setExclusiveStartKey(scanResult.getLastEvaluatedKey());
                    scanResult = AwsToolkitCore.getClientFactory(DynamoDBTableEditor.this.tableEditorInput.getAccountId())
                            .getDynamoDBV2Client().scan(scanRequest);
                } catch ( Exception e ) {
                    DynamoDBPlugin.getDefault().reportException(e.getMessage(), e);
                    return;
                }

                final ScanResult result = scanResult;
                Display.getDefault().asyncExec(new Runnable() {

                    @Override
                    public void run() {
                        contentProvider.addItems(result.getItems());
                        viewer.refresh();
                        DynamoDBTableEditor.this.viewer.getTable().setEnabled(true);
                        DynamoDBTableEditor.this.viewer.getTable().getParent().layout();
                        runScanAction.setEnabled(true);
                        nextPageResultsAction.setEnabled(scanResult.getLastEvaluatedKey() != null);
                        exportAsCSVAction.setEnabled(true);
                    }
                });
            }

        }.start();
    }

    /**
     * Content provider creates columns for the table and keeps track of them
     * for other parts of the UI.
     */
    private class ContentProvider implements IStructuredContentProvider {

        private List<Map<String, AttributeValue>> input;
        private final List<Map<String, AttributeValue>> elementList = new ArrayList<>();
        private final List<String> columns = new ArrayList<>();

        /**
         * Adds a single item to the table.
         */
        void addItem(Map<String, AttributeValue> item) {
            elementList.set(elementList.size() - 1, item);
            elementList.add(new HashMap<String, AttributeValue>());
            viewer.refresh();
        }

        /**
         * Adds a list of new items to the table.
         */
        public void addItems(List<Map<String, AttributeValue>> items) {
            // Remove the (possible) empty row and add it to the end
            if ( !elementList.isEmpty() && elementList.get(elementList.size() - 1).isEmpty() ) {
                elementList.remove(elementList.size() - 1);
            }

            elementList.addAll(items);

            // expand columns if necessary
            List<String> columns = new LinkedList<>();
            for ( Map<String, AttributeValue> item : items ) {
                columns.addAll(item.keySet());
            }

            Table table = (Table) viewer.getControl();
            TableColumnLayout layout = (TableColumnLayout) table.getParent().getLayout();
            for ( String column : columns ) {
                if ( !this.columns.contains(column) ) {
                    this.columns.add(column);
                    createColumn(table, layout, column);
                }
            }

            // empty row for adding new rows
            elementList.add(new HashMap<String, AttributeValue>());
        }

        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            this.input = (List<Map<String, AttributeValue>>) newInput;
            this.elementList.clear();
            this.columns.clear();

            initializeElements();

            if ( this.input != null ) {
                Table table = (Table) viewer.getControl();
                TableColumnLayout layout = (TableColumnLayout) table.getParent().getLayout();

                for ( String col : this.columns ) {
                    createColumn(table, layout, col);
                }
            }
        }

        private void createColumn(Table table, TableColumnLayout layout, String col) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(col);
            layout.setColumnData(column, new ColumnWeightData(10));
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            initializeElements();
            return this.elementList.toArray();
        }

        private synchronized void initializeElements() {
            if ( elementList.isEmpty() && input != null ) {

                Set<String> columns = new HashSet<>();

                for ( Map<String, AttributeValue> item : input ) {
                    columns.addAll(item.keySet());
                }
                // We add the hash and range keys back in at the beginning, so
                // remove them for now
                columns.remove(tableKey.getHashKeyAttributeName());
                if ( tableKey.hasRangeKey() ) {
                    columns.remove(tableKey.getRangeKeyAttributeName());
                }

                List<String> sortedColumns = new ArrayList<>();
                sortedColumns.addAll(columns);
                Collections.sort(sortedColumns);

                sortedColumns.add(0, tableKey.getHashKeyAttributeName());
                if ( tableKey.hasRangeKey() ) {
                    sortedColumns.add(1, tableKey.getRangeKeyAttributeName());
                }

                synchronized (knownAttributes) {
                    knownAttributes.addAll(sortedColumns);
                }

                elementList.addAll(input);
                // empty row at the end for adding new rows
                elementList.add(new HashMap<String, AttributeValue>());

                this.columns.addAll(sortedColumns);
            }
        }

        private synchronized List<String> getColumns() {
            return this.columns;
        }
    }

    private class LabelProvider extends AbstractTableLabelProvider {

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            @SuppressWarnings("unchecked")
            Map<String, AttributeValue> item = (Map<String, AttributeValue>) element;

            String column = DynamoDBTableEditor.this.contentProvider.getColumns().get(columnIndex);
            AttributeValue values = item.get(column);
            return format(values);
        }
    }

    /**
     * CreateNewItemDialog now extends AttributeValueInputDialog, which is a
     * more generic class that includes basic dialog template and value
     * validation.
     */
    private final class CreateNewItemDialog extends AttributeValueInputDialog {
        @SuppressWarnings("serial")
        private CreateNewItemDialog() {
            super("Create new item",
                  "Enter the key for the new item",
                  true,
                  new ArrayList<String>()
                  {{
                      add(tableKey.getHashKeyAttributeName());
                      if (tableKey.getRangeKeyAttributeName() != null) {
                          add(tableKey.getRangeKeyAttributeName());
                      }
                  }},
                  new HashMap<String, Integer>()
                  {{
                      put(tableKey.getHashKeyAttributeName(), getDataType(tableKey.getHashKeyAttributeType()));
                       if (tableKey.getRangeKeyAttributeName() != null) {
                           put(tableKey.getRangeKeyAttributeName(), getDataType(tableKey.getRangeKeyAttributeType()));
                       }
                  }},
                  null);
        }

        Map<String, AttributeValue> getNewItem() {
            String hashKey = attributeValues.get(tableKey.getHashKeyAttributeName());
            String rangeKey = attributeValues.get(tableKey.getRangeKeyAttributeName());
            Map<String, AttributeValue> item = new HashMap<>();
            AttributeValue hashKeyAttribute = new AttributeValue();
            setAttribute(hashKeyAttribute, Arrays.asList(hashKey), tableKey.getHashKeyAttributeType());
            item.put(tableKey.getHashKeyAttributeName(), hashKeyAttribute);
            if ( rangeKey != null && rangeKey.length() > 0 ) {
                AttributeValue rangeKeyAttribute = new AttributeValue();
                setAttribute(rangeKeyAttribute, Arrays.asList(rangeKey), tableKey.getRangeKeyAttributeType());
                item.put(tableKey.getRangeKeyAttributeName(), rangeKeyAttribute);
            }
            return item;
        }
    }

    /**
     * Listener to respond to clicks in a cell, invoking a cell editor
     */
    private final class TextCellEditorListener implements Listener {

        private final Table table;
        private final TableEditor editor;
        private AttributeValueEditor editorComposite;

        private TextCellEditorListener(final Table table, final TableEditor editor) {
            this.table = table;
            this.editor = editor;
        }

        @Override
        public void handleEvent(final Event event) {
            if ( event.type == SWT.FocusOut && this.editorComposite != null && !this.editorComposite.isDisposed() ) {
                Control focus = Display.getCurrent().getFocusControl();
                if ( focus != this.editorComposite && focus != this.editorComposite.editorText && focus != this.table ) {
                    this.editorComposite.dispose();
                }
            } else if (event.type == SWT.KeyDown && event.keyCode == SWT.DEL) {
                deleteItems();
                return;
            } else if (event.type != SWT.MouseUp || event.button != 1) {
                return;
            }

            Rectangle clientArea = this.table.getClientArea();
            Point pt = new Point(event.x, event.y);
            int row = table.getTopIndex();
            while ( row < table.getItemCount() ) {
                boolean visible = false;
                final TableItem item = this.table.getItem(row);

                // We don't care about clicks in the first 1 or 2 columns since
                // they are read-only, except in the last row.
                final boolean isLastRow = row == table.getItemCount() - 1;
                int numKeyColumns = tableKey.hasRangeKey() ? 2 : 1;
                for ( int col = 0; col < table.getColumnCount(); col++ ) {

                    Rectangle rect = item.getBounds(col);
                    if ( rect.contains(pt) ) {
                        if ( editorComposite != null && !editorComposite.isDisposed() ) {
                            editorComposite.dispose();
                        }

                        if ( isLastRow ) {
                            invokeNewItemDialog(item, row);
                            return;
                        } else if ( col < numKeyColumns ) {
//                            table.select(row);
                            return;
                        }

                        final int column = col;
                        final int rowNum = row;

                        final String attributeName = item.getParent().getColumn(col).getText();
                        @SuppressWarnings("unchecked")
                        Map<String, AttributeValue> dynamoDbItem = (Map<String, AttributeValue>) item.getData();
                        final AttributeValue attributeValue = dynamoDbItem.containsKey(attributeName) ? dynamoDbItem
                                .get(attributeName) : new AttributeValue();

                        // If this is a binary value, don't allow editing
                        if ( attributeValue != null && (attributeValue.getBS() != null || attributeValue.getB() != null) ) {
                            invokeBinaryValueDialog(item, attributeName, column, rowNum);
                            return;
                        }

                        // Don't support editing new data types
                        if ( attributeValue != null &&
                                (attributeValue.getBOOL() != null ||
                                 attributeValue.getNULL() != null ||
                                 attributeValue.getM() != null ||
                                 attributeValue.getL() != null) ) {
                            Dialog dialog = new MessageDialog(
                                    Display.getDefault().getActiveShell(),
                                    "Attribute edit not supported",
                                    AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                                    "Editing BOOL/NULL/Map/List attributes is currently not supported.",
                                    MessageDialog.NONE, new String[] { "OK" }, 0);
                            dialog.open();
                            return;
                        }

                        configureCellEditor(item, column, rowNum, attributeName, attributeValue);

                        // If this is a multi-value item, don't allow textual editing
                        if ( attributeValue != null
                                && (attributeValue.getSS() != null || attributeValue.getNS() != null || attributeValue.getBS() != null) ) {
                            editorComposite.editorText.setEditable(false);
                            editorComposite.editorText.addMouseListener(new MouseAdapter() {
                                @Override
                                public void mouseUp(MouseEvent e) {
                                    invokeMultiValueDialog(item, attributeName, column, rowNum,
                                            editorComposite.dataTypeCombo.getSelectionIndex());
                                    editorComposite.dispose();
                                }
                            });
                            return;
                        }

                        editorComposite.editorText.selectAll();
                        editorComposite.editorText.setFocus();
                        return;
                    }
                    if ( !visible && rect.intersects(clientArea) ) {
                        visible = true;
                    }
                }
                if ( !visible ) {
                    return;
                }
                row++;
            }
        }

        /**
         * Invokes a dialog showing a binary value or set of values.
         */
        private void invokeBinaryValueDialog(TableItem item, String attributeName, int column, int rowNum) {
            Dialog dialog = new MessageDialog(
                    Display.getDefault().getActiveShell(),
                    "Binary attribute",
                    AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                    "This is a binary attribute.  Editing binary attributes is unsupported, " +
                    "but you can copy the base64 encoding of this attribute to the clipboard.",
                    MessageDialog.NONE, new String[] { "Copy to clipboard", "Cancel" }, 0);
            int result = dialog.open();
            if ( result == 0 ) {
                Clipboard cb = new Clipboard(Display.getDefault());
                String data = item.getText(column);
                TextTransfer textTransfer = TextTransfer.getInstance();
                cb.setContents(new Object[] { data }, new Transfer[] { textTransfer });
            }
        }

        /**
         * Deletes all selected items from the table.
         */
        private void deleteItems() {
            List<Integer> selectionIndices = new ArrayList<>();
            for (int i : table.getSelectionIndices()) {
                selectionIndices.add(i);
            }

            // Remove all these indices from the data model of the content
            // provider. We go through them backwards to avoid having to
            // recalculate offsets caused by the list shifting to fill in the
            // gaps.
            Collections.sort(selectionIndices);
            for (int i = selectionIndices.size() - 1; i >= 0; i--) {
                Integer selectionIndex = selectionIndices.get(i);
                contentProvider.elementList.remove(selectionIndex.intValue());

                Map<String, AttributeValue> key = getKey(table.getItem(selectionIndex));
                editedItems.remove(key);
                // If this is a newly-added item, don't try to issue a delete
                // request for it.
                if ( addedItems.contains(key) ) {
                    addedItems.remove(key);
                } else {
                    deletedItems.add(key);
                }
            }

            markDirty();

            viewer.refresh();
        }

        /**
         * Configures the member cell editor for the table item and column
         * given.
         */
        private void configureCellEditor(final TableItem item,
                                         final int column,
                                         final int rowNum,
                                         final String attributeName,
                                         final AttributeValue attributeValue) {
            this.editorComposite = new AttributeValueEditor(this.table, SWT.None, editor,
                    table.getItemHeight(), attributeValue);

            editor.setEditor(this.editorComposite, item, column);
            editorComposite.editorText.setText(item.getText(column));

            editorComposite.editorText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    Text text = editorComposite.editorText;
                    int dataType = editorComposite.getSelectedDataType(false);
                    markModified(item, text, rowNum, column, Arrays.asList(text.getText()), dataType);
                }
            });

            /*
             * We validate the user input of the scalar value when the text
             * editor is being disposed. (For set type, the validation happens in MultiValueAttributeEditorDialog.)
             */
            editorComposite.editorText.addDisposeListener(new DisposeListener() {
                @Override
                @SuppressWarnings({ "serial", "unchecked" })
                public void widgetDisposed(DisposeEvent e) {
                    AttributeValue updateAttributeValue = ( (Map<String, AttributeValue>)item.getData() ).get(attributeName);
                    boolean isScalarAttribute = updateAttributeValue != null &&
                                                    ( updateAttributeValue.getN() != null
                                                        || updateAttributeValue.getS() != null
                                                        || updateAttributeValue.getB() != null);
                    /* Only do validation when it is a scalar type. */
                    if ( isScalarAttribute ) {
                        final String attributeInput = editorComposite.editorText.getText();
                        final int dataType = editorComposite.getSelectedDataType(false);
                        if ( !AttributeValueUtil.validateScalarAttributeInput(attributeInput, dataType, false) ) {
                            /* Open up a non-cancelable input dialog */
                            AttributeValueInputDialog attributeValueInputDialog = new AttributeValueInputDialog(
                                    "Invalid attribute value",
                                    "Please provide a valid value for the following attribute",
                                    false,
                                    Arrays.asList(attributeName),
                                    new HashMap<String, Integer>()
                                    {{
                                        put(attributeName, dataType);
                                    }},
                                    new HashMap<String, String>()
                                    {{
                                        put(attributeName, attributeInput);
                                    }});
                            attributeValueInputDialog.open();
                            /* Update the attribute editor and markModified */
                            Text text = editorComposite.editorText;
                            String validatedValue = attributeValueInputDialog.getInputValue(attributeName);
                            text.setText(validatedValue);
                            markModified(item, text, rowNum, column, Arrays.asList(text.getText()), dataType);
                        }
                    }
                }
            });
            editorComposite.editorText.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(final TraverseEvent e) {
                    TextCellEditorListener.this.editorComposite.dispose();
                }
            });

            editorComposite.multiValueEditorButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    invokeMultiValueDialog(item, attributeName, column, rowNum,
                            editorComposite.dataTypeCombo.getSelectionIndex());
                    editorComposite.dispose();
                }
            });

            // Changing the data type must be marked as a change
            editorComposite.dataTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Collection<String> values = getValuesFromAttribute(attributeValue);
                    int dataType;
                    switch (editorComposite.dataTypeCombo.getSelectionIndex()) {
                    case STRING:
                        if ( attributeValue.getS() != null || attributeValue.getSS() != null )
                            return;
                        if ( values.size() > 1 ) {
                            dataType = SS;
                        } else {
                            dataType = S;
                        }
                        break;
                    case NUMBER:
                        if ( attributeValue.getN() != null || attributeValue.getNS() != null )
                            return;
                        if ( values.size() > 1 ) {
                            dataType = NS;
                        } else {
                            dataType = N;
                        }
                        break;
                    default:
                        throw new RuntimeException("Unexpected selection index "
                                + editorComposite.dataTypeCombo.getSelectionIndex());
                    }
                    markModified(item, editorComposite.editorText, rowNum, column, values, dataType);
                }
            });
        }

        private void invokeMultiValueDialog(final TableItem item,
                                            final String attributeName,
                                            final int column,
                                            final int row,
                                            final int selectedType) {
            @SuppressWarnings("unchecked")
            Map<String, AttributeValue> dynamoDbItem = (Map<String, AttributeValue>) item.getData();
            MultiValueAttributeEditorDialog multiValueEditorDialog = new MultiValueAttributeEditorDialog(Display
                    .getDefault().getActiveShell(), dynamoDbItem.get(attributeName), selectedType);

            int returnValue = multiValueEditorDialog.open();
            /* Save set */
            if ( returnValue == 0 ) {
                int dataType = editorComposite.getSelectedDataType(true);
                markModified(item, editorComposite.editorText, row, column, multiValueEditorDialog.getValues(), dataType);
            }
            /* Save single value */
            else if ( returnValue == 1 ) {
                int dataType = editorComposite.getSelectedDataType(false);
                markModified(item, editorComposite.editorText, row, column, multiValueEditorDialog.getValues(), dataType);
            }
            /* Don't do anything when the user pressed Cancel */
        }
    }

    /**
     * Invokes a new dialog to allow creation of a new item in the table.
     */
    private void invokeNewItemDialog(TableItem tableItem, int row) {
        CreateNewItemDialog dialog = new CreateNewItemDialog();

        int result = dialog.open();
        if ( result == 0 ) {
            Map<String, AttributeValue> newItem = dialog.getNewItem();
            contentProvider.addItem(newItem);
            Map<String, AttributeValue> key = getKey(tableItem);
            addedItems.add(key);
            markModified(tableItem, null, row, 0,
                    getValuesFromAttribute(newItem.get(tableKey.getHashKeyAttributeName())),
                    getDataType(tableKey.getHashKeyAttributeType()));
            if ( tableKey.hasRangeKey() ) {
                markModified(tableItem, null, row, 1,
                        getValuesFromAttribute(newItem.get(tableKey.getRangeKeyAttributeName())),
                        getDataType(tableKey.getRangeKeyAttributeType()));
            }
        }
    }

    /**
     * Marks the given tree item and column modified.
     *
     * TODO: type checking for numbers
     */
    protected void markModified(final TableItem item, final Text editorControl, final int row, final int column, final Collection<String> newValue, int dataType) {
        final String attributeName = item.getParent().getColumn(column).getText();
        @SuppressWarnings("unchecked")
        Map<String, AttributeValue> dynamoDbItem = (Map<String, AttributeValue>) item.getData();
        AttributeValue attributeValue = dynamoDbItem.get(attributeName);
        if ( attributeValue == null ) {
            attributeValue = new AttributeValue();
            dynamoDbItem.put(attributeName, attributeValue);
        }

        setAttribute(attributeValue, newValue, dataType);

        Map<String, AttributeValue> editedItemKey = getKey(item);
        if ( !editedItems.containsKey(editedItemKey) ) {
            editedItems.add(editedItemKey, new EditedItem(dynamoDbItem, item));
        }

        // Don't add key attributes to the list of edited attributes
        if ( !attributeName.equals(tableKey.getHashKeyAttributeName())
                && ( !tableKey.hasRangeKey() || !attributeName.equals(tableKey.getRangeKeyAttributeName())) ) {
            editedItems.get(editedItemKey).markAttributeEdited(attributeName);
        }

        // We may already have another entry here, but since we're updating the
        // data model as we go, we can overwrite as many times as we want.
        editedItems.update(editedItemKey, dynamoDbItem);

        item.setText(column, format(attributeValue));

        // Treat the empty string as a null for easier saving
        if ( newValue.size() == 1 && newValue.iterator().next().length() == 0 ) {
            dynamoDbItem.remove(attributeName);
        }

        item.setForeground(column, Display.getDefault().getSystemColor(SWT.COLOR_RED));
        if ( editorControl != null )
            editorControl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        markDirty();
    }

    /**
     * Returns a key for recording a change to the item given, reusing the key
     * if it exists or returning a new one otherwise.
     */
    private Map<String, AttributeValue> getKey(final TableItem item) {
        @SuppressWarnings("unchecked")
        Map<String, AttributeValue> dynamoDbItem = (Map<String, AttributeValue>) item.getData();

        Map<String, AttributeValue> keyAttributes = new HashMap<>();

        String hashKeyAttributeName = tableKey.getHashKeyAttributeName();
        keyAttributes.put(hashKeyAttributeName, dynamoDbItem.get(hashKeyAttributeName));

        if ( tableKey.hasRangeKey() ) {
            String rangeKeyAttributeName = tableKey.getRangeKeyAttributeName();
            keyAttributes.put(rangeKeyAttributeName, dynamoDbItem.get(rangeKeyAttributeName));
        }

        return keyAttributes;
    }

    /**
     * Use DynamoDB V2 to get the attribtue names and types of both hash and range keys
     * of the table, and then save them in a KeySchemaWithAttributeType object.
     */
    private static KeySchemaWithAttributeType convertToKeySchemaWithAttributeType(
            TableDescription table) {
        KeySchemaWithAttributeType keySchema = new KeySchemaWithAttributeType();
        String hashKeyAttributeName = null;
        String rangeKeyAttributeName = null;
        for (KeySchemaElement key : table.getKeySchema()) {
            if (key.getKeyType().equals(KeyType.HASH.toString())) {
                hashKeyAttributeName = key.getAttributeName();
            } else if (key.getKeyType().equals(KeyType.RANGE.toString())) {
                rangeKeyAttributeName = key.getAttributeName();
            }
        }
        for (AttributeDefinition attribute : table.getAttributeDefinitions()) {
            if (hashKeyAttributeName.equals(attribute.getAttributeName())) {
                keySchema.setHashKeyElement(hashKeyAttributeName,
                        attribute.getAttributeType());
            }
            if (rangeKeyAttributeName != null
                    && rangeKeyAttributeName.equals(attribute
                            .getAttributeName())) {
                keySchema.setRangeKeyElement(rangeKeyAttributeName,
                        attribute.getAttributeType());
            }
        }

        return keySchema;
    }

    /**
     * DynamoDB v2 no longer returns AttributeType as part of KeySchemaElement,
     * so we use this class to record the attribute names and types of both
     * hash key and range key.
     *
     */
    private static class KeySchemaWithAttributeType {

        private String hashKeyAttributeName;
        private String hashKeyAttributeType;
        private String rangeKeyAttributeName;
        private String rangeKeyAttributeType;

        public void setHashKeyElement(String attributeName, String attributeType) {
            hashKeyAttributeName = attributeName;
            hashKeyAttributeType = attributeType;
        }

        public void setRangeKeyElement(String attributeName,
                String attributeType) {
            rangeKeyAttributeName = attributeName;
            rangeKeyAttributeType = attributeType;
        }

        public boolean hasRangeKey() {
            return rangeKeyAttributeName != null
                    && rangeKeyAttributeType != null;
        }

        public String getHashKeyAttributeName() {
            return hashKeyAttributeName;
        }

        public String getHashKeyAttributeType() {
            return hashKeyAttributeType;
        }

        public String getRangeKeyAttributeName() {
            return rangeKeyAttributeName;
        }

        public String getRangeKeyAttributeType() {
            return rangeKeyAttributeType;
        }
    }
}
