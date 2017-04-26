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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.popup.actions;

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.popup.AbstractAction;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

public class LoadDataAction extends AbstractAction {
    private static final String TEXT = Messages.loadDataMenu;

    protected Table table = null;

    @Override
    protected void initialize() {
        initializeAction(null, null, TEXT, TEXT);
    }

    @Override
    public void run() {
    }

    @Override
    public void selectionChanged(final SelectionChangedEvent event) {
        this.table = null;
        if (event.getSelection() instanceof IStructuredSelection) {
            if (((IStructuredSelection) event.getSelection()).getFirstElement() instanceof Table) {
                this.table = (Table) ((IStructuredSelection) event.getSelection()).getFirstElement();
            }
        }
        if (isEnabled() && this.table != null) {
        }
        setEnabled(false);
    }

}
