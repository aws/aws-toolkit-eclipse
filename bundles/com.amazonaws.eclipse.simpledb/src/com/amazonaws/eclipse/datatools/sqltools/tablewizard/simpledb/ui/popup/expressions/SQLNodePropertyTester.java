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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.popup.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.datatools.connectivity.sqm.core.ui.explorer.virtual.IVirtualNode;
import org.eclipse.datatools.modelbase.sql.schema.Catalog;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;

public class SQLNodePropertyTester extends PropertyTester {

    @Override
    public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
        if (!"vendor".equalsIgnoreCase(property)) { //$NON-NLS-1$
            return false;
        }
        if (receiver instanceof IVirtualNode) {
            IVirtualNode node = (IVirtualNode) receiver;
            Database db = node.getParentConnection().getSharedDatabase();
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        if (receiver instanceof Table) {
            Table table = (Table) receiver;
            Database db = table.getSchema().getDatabase() == null ? table.getSchema().getCatalog().getDatabase() : table
                    .getSchema().getDatabase();
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        if (receiver instanceof Column) {
            Column column = (Column) receiver;
            Schema schema = column.getTable().getSchema();
            Database db = schema.getDatabase() == null ? schema.getCatalog().getDatabase() : schema.getDatabase();
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        if (receiver instanceof Catalog) {
            Catalog catalog = (Catalog) receiver;
            Database db = catalog.getDatabase();
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        if (receiver instanceof Schema) {
            Schema schema = (Schema) receiver;
            Database db = schema.getDatabase() == null ? schema.getCatalog().getDatabase() : schema.getDatabase();
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        if (receiver instanceof Database) {
            Database db = (Database) receiver;
            return expectedValue.toString().equalsIgnoreCase(db.getVendor());
        }
        return false;
    }

}
