/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.ec2.ui.ebs;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.amazonaws.eclipse.core.AccountAndRegionChangeListener;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.StatusBar;

/**
 * View for working with Elastic Block Storage volumes and snapshots.
 */
public class ElasticBlockStorageView extends ViewPart implements IRefreshable {

    /** The snapshot selection table  */
    private SnapshotSelectionTable snapshotSelectionTable;

    /** The EBS volume selection table */
    private VolumeSelectionTable volumeSelectionTable;

    /** The Action object for refreshing this view's data */
    private Action refreshAction;

    /**
     * Listener for AWS account and region preference changes that require this
     * view to be refreshed.
     */
    AccountAndRegionChangeListener accountAndRegionChangeListener = new AccountAndRegionChangeListener() {
        @Override
        public void onAccountOrRegionChange() {
            ElasticBlockStorageView.this.refreshData();
        }
    };


    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 2;
        parent.setLayout(layout);

        StatusBar statusBar = new StatusBar(parent);
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        volumeSelectionTable = new VolumeSelectionTable(sashForm);
        snapshotSelectionTable = new SnapshotSelectionTable(sashForm);
        volumeSelectionTable.setSnapshotSelectionTable(snapshotSelectionTable);

        volumeSelectionTable.setListener(statusBar);

        contributeToActionBars();
    }

    @Override
    public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
        super.init(aSite, aMemento);
         AwsToolkitCore.getDefault().getAccountManager().addAccountInfoChangeListener(accountAndRegionChangeListener);
         AwsToolkitCore.getDefault().getAccountManager().addDefaultAccountChangeListener(accountAndRegionChangeListener);
         AwsToolkitCore.getDefault().addDefaultRegionChangeListener(accountAndRegionChangeListener);
    }

    /*
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        AwsToolkitCore.getDefault().getAccountManager().removeAccountInfoChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().getAccountManager().removeDefaultAccountChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().removeDefaultRegionChangeListener(accountAndRegionChangeListener);

        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        volumeSelectionTable.setFocus();
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        refreshAction = new Action() {
            @Override
            public void run() {
                if (volumeSelectionTable != null)
                    volumeSelectionTable.refreshVolumes();

                if (snapshotSelectionTable != null)
                    snapshotSelectionTable.refreshSnapshots();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh EBS volumes and snapshots");
        refreshAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));

        manager.add(refreshAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        if (refreshAction != null)
            refreshAction.run();
    }

}
