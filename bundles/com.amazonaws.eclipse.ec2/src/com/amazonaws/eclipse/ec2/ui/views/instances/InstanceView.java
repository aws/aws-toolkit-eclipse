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

package com.amazonaws.eclipse.ec2.ui.views.instances;

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
import com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener;
import com.amazonaws.eclipse.ec2.ui.StatusBar;

/**
 * View for working with EC2 instances.
 */
public class InstanceView extends ViewPart implements IRefreshable, SelectionTableListener {

    /** Shared factory for creating EC2 clients */
    private StatusBar statusBar;

    /** Table of running instances */
    private InstanceSelectionTable selectionTable;

    /**
     * Listener for AWS account and region preference changes that require this
     * view to be refreshed.
     */
    AccountAndRegionChangeListener accountAndRegionChangeListener = new AccountAndRegionChangeListener() {
        @Override
        public void onAccountOrRegionChange() {
            InstanceView.this.refreshData();
        }
    };


    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        if (selectionTable != null) {
            selectionTable.refreshInstances();
        }
    }

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

        statusBar = new StatusBar(parent);
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusBar.setRecordLabel("Displayed Instances: ");

        selectionTable = new InstanceSelectionTable(parent);
        selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        selectionTable.setListener(this);

        refreshData();

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

        if (statusBar != null) statusBar.dispose();

        if (selectionTable != null) selectionTable.dispose();

        super.dispose();
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        bars.getToolBarManager().add(selectionTable.getRefreshAction());

        /** Adds Dropdown filter action for Instance States */
        bars.getToolBarManager().add(selectionTable.getInstanceStateFilterDropDownAction());

        /** Adds Dropdown filter action for Security Filter Groups */
        bars.getToolBarManager().add(selectionTable.getSecurityGroupFilterAction());
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        selectionTable.setFocus();
    }


    /*
     * SelectionTableListener Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener#finishedLoadingData()
     */
    @Override
    public void finishedLoadingData(int noOfInstances) {
        statusBar.finishedLoadingData(noOfInstances);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener#loadingData()
     */
    @Override
    public void loadingData() {
        statusBar.loadingData();
    }

}
