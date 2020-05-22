package com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.datatools.sqltools.sqlbuilder.views.source.SQLEditorDocumentProvider;
import org.eclipse.datatools.sqltools.sqlbuilder.views.source.SQLSourceEditingEnvironment;
import org.eclipse.datatools.sqltools.sqlbuilder.views.source.SQLSourceViewerConfiguration;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
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
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.ui.AbstractTableContentProvider;
import com.amazonaws.eclipse.core.ui.AbstractTableLabelProvider;
import com.amazonaws.eclipse.datatools.enablement.simpledb.driver.SimpleDBItemName;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;

/**
 * Editor to run queries and edit results
 */
public class QueryEditor extends EditorPart {

    public static final String ID = "com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor.queryEditor";

    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s+"); //$NON-NLS-1$
    private static final Pattern PATTERN_FROM_CLAUSE = Pattern.compile("from\\s+[\\S&&[^,]]+"); //$NON-NLS-1$
    public static final char DELIMITED_IDENTIFIER_QUOTE = '`';

    private DomainEditorInput domainEditorInput;

    private TableViewer viewer;

    boolean dirty;
    private Map<String, Collection<EditedAttributeValue>> editedCells = new HashMap<>();
    private String resultDomain;

    private ToolBarManager toolBarManager;
    private ToolBar toolBar;
    private SourceViewer sqlSourceViewer;
    private Composite sqlSourceViewerComposite;
    private IDocument sqlSourceDocument;

    private ExportAsCSVAction exportAsCSV;

    private ContentProvider contentProvider;

    @Override
    public void doSave(final IProgressMonitor monitor) {
        AmazonSimpleDB simpleDBClient = AwsToolkitCore.getClientFactory(this.domainEditorInput.getAccountId())
                .getSimpleDBClient();

        String domain = this.resultDomain;

        ArrayList<ReplaceableItem> items = new ArrayList<>();
        for ( String itemName : QueryEditor.this.editedCells.keySet() ) {

            ReplaceableItem replaceableItem = new ReplaceableItem();
            replaceableItem.setName(itemName);
            Collection<ReplaceableAttribute> attributes = new LinkedList<>();

            for ( EditedAttributeValue editedValue : QueryEditor.this.editedCells.get(itemName) ) {
                if ( editedValue.newValue != null ) {
                    for ( String v : editedValue.newValue ) {
                        ReplaceableAttribute attr = new ReplaceableAttribute().withName(editedValue.name)
                                .withReplace(true).withValue(v);
                        attributes.add(attr);
                    }
                }
            }

            if ( !attributes.isEmpty() ) {
                items.add(replaceableItem);
                replaceableItem.setAttributes(attributes);
            }
        }

        if ( !items.isEmpty() ) {
            simpleDBClient.batchPutAttributes(new BatchPutAttributesRequest(domain, items));
        }

        for ( String itemName : QueryEditor.this.editedCells.keySet() ) {
            for ( EditedAttributeValue editedValue : QueryEditor.this.editedCells.get(itemName) ) {
                QueryEditor.this.viewer.getTable().getItem(editedValue.row)
                .setForeground(editedValue.col, Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            }
        }

        QueryEditor.this.editedCells.clear();
        this.dirty = false;
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        this.domainEditorInput = (DomainEditorInput) input;
        setPartName(input.getName());
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    private void markDirty() {
        this.dirty = true;
        firePropertyChange(PROP_DIRTY);
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(final Composite composite) {
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

        configureSQLSourceViewer(composite);

        data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(this.sqlSourceViewerComposite, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        this.toolBar.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(this.toolBar, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        this.sqlSourceViewerComposite.setLayoutData(data);

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
    }

    private void configureSQLSourceViewer(final Composite composite) {
        int VERTICAL_RULER_WIDTH = 12;

        int styles = SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
        ISharedTextColors sharedColors = EditorsPlugin.getDefault().getSharedTextColors();
        CompositeRuler ruler = new CompositeRuler(VERTICAL_RULER_WIDTH);

        try {
            this.sqlSourceDocument = SQLEditorDocumentProvider.createDocument(getDefaultQuery());
        } catch ( CoreException e ) {
            e.printStackTrace();
        }

        AnnotationModel annotationModel = new AnnotationModel();
        annotationModel.connect(this.sqlSourceDocument);

        this.sqlSourceViewerComposite = new Composite(composite, SWT.BORDER);
        this.sqlSourceViewerComposite.setLayout(new FillLayout());
        this.sqlSourceViewer = new SourceViewer(this.sqlSourceViewerComposite, ruler, null, true, styles);

        SQLSourceEditingEnvironment.connect();
        SQLSourceViewerConfiguration configuration = new SQLSourceViewerConfiguration();
        configuration.getCompletionProcessor().setCompletionProposalAutoActivationCharacters(new char[0]);
        this.sqlSourceViewer.configure(configuration);
        this.sqlSourceViewer.setDocument(this.sqlSourceDocument, annotationModel);
    }

    /**
     * Creates actions for this editor
     */
    private void createActions() {

        this.toolBarManager.add(new OpenSelectSyntaxDocumentationAction());

        this.toolBarManager.add(new Action() {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_START);
            }

            @Override
            public String getText() {
                return "Run query";
            }

            @Override
            public String getToolTipText() {
                return getText();
            }

            @Override
            public void run() {
                runQuery(QueryEditor.this.sqlSourceDocument.get());
            }

            @Override
            public int getAccelerator() {
                return SWT.CONTROL | SWT.ALT | 'x';
            }

        });

        this.exportAsCSV = new ExportAsCSVAction();
        this.exportAsCSV.setEnabled(false);
        this.toolBarManager.add(this.exportAsCSV);

        this.toolBarManager.update(true);
    }

    private static final String[] exportExtensions = new String[] { "*.csv" };

    private class ExportAsCSVAction extends Action {

        @Override
        public ImageDescriptor getImageDescriptor() {
            return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_EXPORT);
        }

        @Override
        public String getText() {
            return "Export as CSV";
        }

        @Override
        public String getToolTipText() {
            return "Export as comma-separated-value";
        }

        @Override
        public void run() {
            FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
            dialog.setOverwrite(true);
            dialog.setFilterExtensions(exportExtensions);
            String csvFile = dialog.open();
            if (csvFile != null) {
                writeCsvFile(csvFile);
            }
        }

        private void writeCsvFile(final String csvFile) {
            try {
                try (RandomAccessFile raf = new RandomAccessFile(new File(csvFile), "rw")) {
                    raf.setLength(0L);
                }

                List<SimpleDBItem> items = new LinkedList<>();
                Set<String> columns = new LinkedHashSet<>();


                for ( TableItem tableItem : QueryEditor.this.viewer.getTable().getItems() ) {
                    SimpleDBItem e = (SimpleDBItem) tableItem.getData();
                    columns.addAll(e.columns);
                    items.add(e);
                }

                try (BufferedWriter out = new BufferedWriter(new FileWriter(csvFile))) {
                    out.write(SimpleDBItemName.ITEM_HEADER);
                    for (String col : columns) {
                        out.write(",");
                        out.write(col);
                    }
                    out.write("\n");

                    for ( SimpleDBItem item : items ) {
                        out.write(item.itemName);
                        for (String col : columns) {
                            out.write(",");
                            Collection<String> values = item.attributes.get(col);
                            if (values != null) {
                                String value = join(values);
                                // For csv files, we need to quote all values and escape all quotes
                                value = value.replaceAll("\"", "\"\"");
                                value = "\"" + value + "\"";
                                out.write(value);
                            }
                        }
                        out.write("\n");
                    }

                }

            } catch (Exception e) {
                AwsToolkitCore.getDefault().logError("Couldn't save CSV file", e);
            }
        }
    }

    private static class OpenSelectSyntaxDocumentationAction extends Action {
        public static final String SIMPLEDB_SELECT_SYNTAX_DOCUMENTATION_URL =
                "http://docs.amazonwebservices.com/AmazonSimpleDB/latest/DeveloperGuide/index.html?UsingSelect.html";

        public OpenSelectSyntaxDocumentationAction() {
            this.setText("SimpleDB Select Syntax Documentation");
            this.setToolTipText("View Amazon SimpleDB select query syntax documentation");
            this.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_LCL_LINKTO_HELP));
        }

        @Override
        public void run() {
            BrowserUtils.openExternalBrowser(SIMPLEDB_SELECT_SYNTAX_DOCUMENTATION_URL);
        }
    }

    private static String join(final Collection<String> values) {
        return join(values, ",");
    }

    /**
     * Joins a collection of attribute values, correctly handling single-value
     * attributes and empty sets.
     */
    private static String join(final Collection<String> values, final String separator) {
        if ( values == null || values.isEmpty() ) {
            return "";
        }
        if ( values.size() == 1 ) {
            return values.iterator().next();
        }

        StringBuilder builder = new StringBuilder("[");
        boolean seenOne = false;
        for ( String s : values ) {
            if ( seenOne ) {
                builder.append(separator);
            } else {
                seenOne = true;
            }
            builder.append(s);
        }
        builder.append("]");

        return builder.toString();
    }

    /**
     * Updates the query results asynchronously. Must be called from the UI
     * thread.
     */
    private void runQuery(final String query) {

        // Clear out the existing table
        this.viewer.getTable().setEnabled(false);
        this.exportAsCSV.setEnabled(false);
        for ( TableColumn col : this.viewer.getTable().getColumns() ) {
            col.dispose();
        }

        new Thread() {

            @Override
            public void run() {
                SelectResult select = null;
                try {
                    select = AwsToolkitCore.getClientFactory(QueryEditor.this.domainEditorInput.getAccountId()).getSimpleDBClient()
                            .select(new SelectRequest(query));
                } catch ( Exception e ) {
                    AwsToolkitCore.getDefault().reportException(e.getMessage(), e);
                    return;
                }
                final SelectResult result = select;
                QueryEditor.this.resultDomain = getDomainName(query);
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        QueryEditor.this.viewer.setInput(result);
                        QueryEditor.this.viewer.getTable().setEnabled(true);
                        QueryEditor.this.exportAsCSV.setEnabled(true);
                        QueryEditor.this.viewer.getTable().getParent().layout();
                    }
                });
            }

        }.start();
    }

    private String convertSQLIdentifierToCatalogFormat(final String sqlIdentifier, final char idDelimiterQuote) {
        String catalogIdentifier = sqlIdentifier;

        if ( sqlIdentifier != null ) {
            String delimiter = String.valueOf(idDelimiterQuote);

            boolean isDelimited = sqlIdentifier.startsWith(delimiter) && sqlIdentifier.endsWith(delimiter);
            boolean containsQuotedDelimiters = sqlIdentifier.indexOf(delimiter + delimiter) > -1;

            if ( isDelimited ) {
                catalogIdentifier = sqlIdentifier.substring(1, sqlIdentifier.length() - 1);

                if ( containsQuotedDelimiters ) {
                    catalogIdentifier = catalogIdentifier.replaceAll(delimiter + delimiter, delimiter);
                }
            } else {
                catalogIdentifier = sqlIdentifier;
            }
        }

        return catalogIdentifier;
    }

    /**
     * Returns the domain name from a select query.
     */
    public String getDomainName(final String sql) {
        Matcher m = PATTERN_FROM_CLAUSE.matcher(sql);
        if ( m.find() ) {
            String fromExpression = sql.substring(m.start(), m.end());
            m = PATTERN_WHITESPACE.matcher(fromExpression);
            if ( m.find() ) {
                String domainName = convertSQLIdentifierToCatalogFormat(fromExpression.substring(m.end()),
                        DELIMITED_IDENTIFIER_QUOTE);
                return domainName;
            }
        }
        return null;
    }

    /**
     * Creates the results table
     */
    private void createResultsTable(final Composite resultsComposite) {
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

        TextCellEditorListener listener = new TextCellEditorListener(table, editor);
        table.addListener(SWT.MouseUp, listener);
        table.addListener(SWT.FocusOut, listener);
    }

    private String getDefaultQuery() {
        return "select * from `" + this.domainEditorInput.getDomainName() + "`";
    }

    @Override
    public void setFocus() {
    }

    /**
     * Listener to respond to clicks in a cell, invoking a cell editor
     */
    private final class TextCellEditorListener implements Listener {

        private final Table table;
        private final TableEditor editor;
        private Composite editorComposite;
        private Text editorText;
        private Button button;

        private TextCellEditorListener(final Table table, final TableEditor editor) {
            this.table = table;
            this.editor = editor;
        }

        @Override
        public void handleEvent(final Event event) {
            if ( event.type == SWT.FocusOut && this.editorComposite != null && !this.editorComposite.isDisposed() ) {
                Control focus = Display.getCurrent().getFocusControl();
                if ( focus != this.editorComposite && focus != this.editorText && focus != this.table ) {
                    this.editorComposite.dispose();
                }
            }

            Rectangle clientArea = this.table.getClientArea();
            Point pt = new Point(event.x, event.y);
            int row = this.table.getTopIndex();
            while ( row < this.table.getItemCount() ) {
                boolean visible = false;
                final TableItem item = this.table.getItem(row);

                // We don't care about clicks in the first column since they
                // are read-only
                for ( int col = 1; col < this.table.getColumnCount(); col++ ) {
                    Rectangle rect = item.getBounds(col);
                    if ( rect.contains(pt) ) {
                        if ( this.editorComposite != null && !this.editorComposite.isDisposed() ) {
                            this.editorComposite.dispose();
                        }

                        // If this is a multi-value item, don't allow textual
                        // editing
                        SimpleDBItem simpleDBItem = (SimpleDBItem) item.getData();
                        final String attributeName = item.getParent().getColumn(col).getText();
                        if ( simpleDBItem.attributes.containsKey(attributeName)
                                && simpleDBItem.attributes.get(attributeName).size() > 1 ) {
                            invokeMultiValueDialog(item, attributeName, col, row);
                            return;
                        }


                        createEditor();

                        final int column = col;
                        final int rowNum = row;
                        this.editor.setEditor(this.editorComposite, item, col);
                        this.editorText.setText(item.getText(col));

                        this.editorText.addModifyListener(new ModifyListener() {

                            @Override
                            public void modifyText(final ModifyEvent e) {
                                markModified(item, column, rowNum, TextCellEditorListener.this.editorText);
                            }
                        });
                        this.editorText.addTraverseListener(new TraverseListener() {

                            @Override
                            public void keyTraversed(final TraverseEvent e) {
                                TextCellEditorListener.this.editorComposite.dispose();
                            }
                        });
                        this.button.addSelectionListener(new SelectionAdapter() {

                            @Override
                            public void widgetSelected(final SelectionEvent e) {
                                invokeMultiValueDialog(item, attributeName, column, rowNum);
                                TextCellEditorListener.this.editorComposite.dispose();
                            }
                        });

                        this.editorText.selectAll();
                        this.editorText.setFocus();
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

        private void createEditor() {
            this.editorComposite = new Composite(this.table, SWT.None);
            this.editorComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            GridLayout layout = new GridLayout(2, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.verticalSpacing = 0;
            layout.horizontalSpacing = 0;
            this.editorComposite.setLayout(layout);

            this.editorText = new Text(this.editorComposite, SWT.None);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).grab(true, true).indent(2, 2)
            .applyTo(this.editorText);

            this.button = new Button(this.editorComposite, SWT.None);
            this.button.setText("...");
            GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.TOP).grab(false, true).applyTo(this.button);
            this.button.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        }

        private void invokeMultiValueDialog(final TableItem item,
                                            final String attributeName,
                                            final int column,
                                            final int row) {
            SimpleDBItem simpleDBItem = (SimpleDBItem) item.getData();
            MultiValueAttributeEditorDialog multiValueEditorDialog = new MultiValueAttributeEditorDialog(Display.getDefault()
                    .getActiveShell(), simpleDBItem, attributeName);
            int returnValue = multiValueEditorDialog.open();
            if ( returnValue == 0 ) {
                markModified(item, column, row, multiValueEditorDialog.getValues());
            }
        }
    }

    private class LabelProvider extends AbstractTableLabelProvider {

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            SimpleDBItem item = (SimpleDBItem) element;
            if ( columnIndex == 0 ) {
                return item.itemName;
            }

            // Column index is offset by one to make room for item name
            String column = QueryEditor.this.contentProvider.getColumns()[columnIndex - 1];
            Collection<String> values = item.attributes.get(column);
            return join(values);
        }
    }

    private class ContentProvider extends AbstractTableContentProvider {

        private SelectResult input;
        private Object[] elements;
        private String[] columns;

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
            this.input = (SelectResult) newInput;
            this.elements = null;
            initializeElements();

            if ( this.input != null ) {
                Table table = (Table) viewer.getControl();
                TableColumnLayout layout = (TableColumnLayout) table.getParent().getLayout();

                TableColumn column = new TableColumn(table, SWT.NONE);
                column.setText(SimpleDBItemName.ITEM_HEADER);
                layout.setColumnData(column, new ColumnWeightData(10));

                for ( String col : this.columns ) {
                    column = new TableColumn(table, SWT.NONE);
                    column.setText(col);
                    layout.setColumnData(column, new ColumnWeightData(10));
                }
            }
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            initializeElements();
            return this.elements;
        }

        private synchronized void initializeElements() {
            if ( this.elements == null && this.input != null ) {
                List<SimpleDBItem> items = new LinkedList<>();
                Set<String> columns = new LinkedHashSet<>();

                for ( Item item : this.input.getItems() ) {
                    SimpleDBItem e = new SimpleDBItem(item);
                    columns.addAll(e.columns);
                    items.add(e);
                }
                this.elements = items.toArray();
                this.columns = columns.toArray(new String[columns.size()]);
            }
        }

        private synchronized String[] getColumns() {
            return this.columns;
        }
    }

    /**
     * Container for edited attributes
     */
    private class EditedAttributeValue {

        private String name;
        private Collection<String> newValue;
        int row;
        int col;

        @Override
        public String toString() {
            return "name: " + this.name + "; new: " + this.newValue;
        }
    }

    /**
     * Marks the given tree item and column modified.
     */
    protected void markModified(final TableItem item, final int column, final int row, final Text text) {
        List<String> values = new LinkedList<>();
        values.add(text.getText());
        markModified(item, column, row, values);
    }

    /**
     * Marks the given tree item and column modified.
     */
    protected void markModified(final TableItem item, final int column, final int row, final Collection<String> newValue) {

        item.setText(column, join(newValue));

        SimpleDBItem simpleDBItem = (SimpleDBItem) item.getData();
        String itemName = simpleDBItem.itemName;
        String attributeName = item.getParent().getColumn(column).getText();

        // Update the data model with this new value
        simpleDBItem.attributes.put(attributeName, newValue);

        // Bootstrap new items
        if ( !this.editedCells.containsKey(itemName) ) {
            this.editedCells.put(itemName, new LinkedList<EditedAttributeValue>());
        }

        // Find this value in the list and update it
        EditedAttributeValue value = null;
        for ( EditedAttributeValue v : this.editedCells.get(itemName) ) {
            if ( v.name.equals(attributeName) ) {
                value = v;
                break;
            }
        }

        // Create the edited value if this is the first time we've edited it
        if ( value == null ) {
            value = new EditedAttributeValue();
            value.name = attributeName;
            value.col = column;
            value.row = row;
            this.editedCells.get(itemName).add(value);
        }
        value.newValue = newValue;

        // Finally update the table UI to reflect the edit
        item.setForeground(column, Display.getDefault().getSystemColor(SWT.COLOR_RED));
        markDirty();
    }

}
