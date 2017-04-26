/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jmock.integration.junit3.MockObjectTestCase;


public class EditorTest extends MockObjectTestCase {

    private Composite c;
    private IWorkbench w;

    //  private IWorkbenchPage aPage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.w = PlatformUI.getWorkbench();
        //    this.aPage = this.w.getActiveWorkbenchWindow().getActivePage();
    }

    public void testGetTableEditor() throws Exception {

        //        Shell shell = this.w.getActiveWorkbenchWindow().getShell();
        //        this.c = new Composite(shell, SWT.None);
        //
        //        final String mockName = "testTable";
        //        final String colName = "colName";
        //        final TableDataEditor t = new TableDataEditor();
        //        final IEditorSite editorSite = mock(IEditorSite.class);
        //        final TableForMock table = mock(TableForMock.class);
        //        final Connection con = new JdbcConnection(new JdbcDriver(null), "", "", "") {
        //
        //            @Override
        //            public AmazonSimpleDB getClient() {
        //                return null;// new AmazonSimpleDBMock();
        //            }
        //
        //        };
        //        final BasicEList<Column> cols = new BasicEList<Column>();
        //        final Database db = mock(Database.class);
        //        final Schema sc = mock(Schema.class);
        //        final Column col1 = mock(ColumnForMock.class);
        //
        //        cols.add(col1);
        //        cols.add(col1);
        //
        //        checking(new Expectations() {
        //            {
        //                allowing(table).getName();
        //                will(returnValue(mockName));
        //                allowing(table).getColumns();
        //                will(returnValue(cols));
        //                allowing(table).getSchema();
        //                will(returnValue(sc));
        //                allowing(table).getConnection();
        //                will(returnValue(con));
        //                allowing(table);
        //
        //                allowing(sc).getCatalog();
        //                will(returnValue(null));
        //                allowing(sc).getDatabase();
        //                will(returnValue(db));
        //                allowing(sc);
        //
        //                allowing(editorSite);
        //
        //                allowing(col1).getTable();
        //                will(returnValue(table));
        //                allowing(col1).getName();
        //                will(returnValue(colName));
        //                allowing(col1);
        //
        //                allowing(db).getVendor();
        //                will(returnValue("SimpleDB"));
        //                allowing(db).getVersion();
        //                will(returnValue("1.0"));
        //                allowing(db);
        //            }
        //        });
        //
        //        t.init(editorSite, new TableDataEditorInput(table));
        //        t.createPartControl(this.c);
        //        assertEquals(mockName, t.getSqlTable().getName());
        //
        //        ITableData tableData = t.getTableData();
        //
        //        assertEquals(2, tableData.getColumnCount());
        //        assertEquals(Types.VARCHAR, tableData.getColumnType(0));
        //        assertEquals(Types.VARCHAR, tableData.getColumnType(1));
        //
        //        Class<?> c = Class.forName(TableDataTableCursorExternalEditingSupport.class.getCanonicalName());
        //        TableDataTableCursorExternalEditingSupport cursor = (TableDataTableCursorExternalEditingSupport) t.getCursor();
        //        Field field = c.getDeclaredField("cellEditors");
        //        field.setAccessible(true);
        //        IExternalTableDataEditor[] edit = (IExternalTableDataEditor[]) field.get(cursor);
        //        assertEquals(SDBTextEditor.class, edit[0].getClass());
        //
        //        //    assertEquals(SimpleDBDataAccessor.class, tableData.getColumnDataAccessor(1).getClass());

    }
}
