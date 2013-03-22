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
package com.amazonaws.eclipse.ec2.ui.keypair;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;

/**
 * Small wrapper around a {@link KeyPairSelectionTable} that adds a toolbar.
 */
public class KeyPairComposite extends Composite {
    private KeyPairSelectionTable keyPairSelectionTable;
    private ToolBar toolBar;
    private ToolBarManager toolBarManager;
    private final String accountId;

    /**
     * Constructs a key pair composite that uses the current AWS account.
     */
    public KeyPairComposite(Composite parent) {
        this(parent, AwsToolkitCore.getDefault().getCurrentAccountId(), null);
    }

    /**
     * Constructs a key pair composite that uses the account id given.
     */
    public KeyPairComposite(Composite parent, String accountId) {
         this(parent, accountId, null);
    }

    /**
     * Constructs a key pair composite that uses the both account id and endpoint given.
     */
    public KeyPairComposite(Composite parent, String accountId, Region ec2RegionOverride) {
        super(parent, SWT.NONE);

        this.accountId = accountId;

        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.marginTop = 1;
        setLayout(layout);

        toolBarManager = new ToolBarManager();
        toolBar = toolBarManager.createControl(this);
        toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        keyPairSelectionTable = new KeyPairSelectionTable(this, this.accountId, ec2RegionOverride);
        keyPairSelectionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        toolBarManager.add(keyPairSelectionTable.refreshAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(keyPairSelectionTable.createNewKeyPairAction);
        toolBarManager.add(keyPairSelectionTable.deleteKeyPairAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(keyPairSelectionTable.registerKeyPairAction);
        toolBarManager.update(true);
    }


    public void setEc2EndpointOverride(Region region) {
        keyPairSelectionTable.setEc2RegionOverride(region);
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
