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

package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.explorer;

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.virtual.IColumnNode;
import org.eclipse.datatools.connectivity.sqm.server.internal.ui.explorer.providers.SQLModelLabelProviderExtension;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.tables.Table;

import com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.Messages;

public class SimpleDBLabelProviderExtension extends SQLModelLabelProviderExtension {

    public SimpleDBLabelProviderExtension() {
    }

    @Override
    public String getText(final Object element) {
        if (element instanceof IColumnNode) {
            IColumnNode node = (IColumnNode) element;
            try {
                Table table = (Table) node.getParent();
                Database db = table.getSchema().getDatabase() == null ? table.getSchema().getCatalog().getDatabase() : table
                        .getSchema().getDatabase();
                if (SimpleDBContentProviderExtension.DB_DEFINITION_VENDOR.equals(db.getVendor())) {
                    return Messages.itemName_and_attributes;
                }
            } catch (Exception e) {
                // ignore - some other tree
            }
        }
        return super.getText(element);
    }

}
