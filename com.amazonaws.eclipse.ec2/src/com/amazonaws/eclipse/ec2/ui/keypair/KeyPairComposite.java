/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.ec2.ui.keypair;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Small wrapper around a {@link KeyPairSelectionTable} that adds a toolbar.
 */
public class KeyPairComposite extends Composite {
    private KeyPairSelectionTable keyPairSelectionTable;
    private ToolBar toolBar;
    private ToolBarManager toolBarManager;

    public KeyPairComposite(Composite parent) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.marginTop = 1;
        setLayout(layout);

        toolBarManager = new ToolBarManager();
        toolBar = toolBarManager.createControl(this);
        toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        keyPairSelectionTable = new KeyPairSelectionTable(this);
        keyPairSelectionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        toolBarManager.add(keyPairSelectionTable.refreshAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(keyPairSelectionTable.createNewKeyPairAction);
        toolBarManager.add(keyPairSelectionTable.deleteKeyPairAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(keyPairSelectionTable.registerKeyPairAction);
        toolBarManager.update(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        keyPairSelectionTable.getViewer().getControl().setEnabled(enabled);

        if (enabled) {
            keyPairSelectionTable.updateActionsForSelection();
        } else {
            keyPairSelectionTable.refreshAction.setEnabled(enabled);
            keyPairSelectionTable.createNewKeyPairAction.setEnabled(enabled);
            keyPairSelectionTable.deleteKeyPairAction.setEnabled(enabled);
            keyPairSelectionTable.registerKeyPairAction.setEnabled(enabled);
        }
    }

    public TreeViewer getViewer() {
        return keyPairSelectionTable.getViewer();
    }

    public KeyPairSelectionTable getKeyPairSelectionTable() {
        return keyPairSelectionTable;
    }
}
