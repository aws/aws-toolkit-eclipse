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

package com.amazonaws.eclipse.ec2.ui.securitygroups;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
import com.amazonaws.eclipse.ec2.ui.StatusBar;

/**
 * View to display security groups and their permission.
 */
public class SecurityGroupView extends ViewPart implements IRefreshable {

    private SecurityGroupSelectionComposite securityGroupSelectionComposite;
    private PermissionsComposite permissionsComposite;

    /**
     * Listener for AWS account and region preference changes that require this
     * view to be refreshed.
     */
    AccountAndRegionChangeListener accountAndRegionChangeListener = new AccountAndRegionChangeListener() {
        @Override
        public void onAccountOrRegionChange() {
            SecurityGroupView.this.refreshData();
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

        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        securityGroupSelectionComposite = new SecurityGroupSelectionComposite(sashForm);
        permissionsComposite = new PermissionsComposite(sashForm);

        securityGroupSelectionComposite.setListener(statusBar);

        /*
         * The security group table and the permissions table need to communicate with
         * each other when certain events happen.  It'd be a little nicer to have them
         * isolated from each other with some sort of event/listener, but for now we
         * can simply have them be aware of each other.
         */
        securityGroupSelectionComposite.setPermissionsComposite(permissionsComposite);
        permissionsComposite.setSecurityGroupComposite(securityGroupSelectionComposite);

        sashForm.setWeights(new int[] {50, 50});

        securityGroupSelectionComposite.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}

            @Override
            public void widgetSelected(SelectionEvent e) {
                permissionsComposite.refreshPermissions();
            }
        });

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

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(securityGroupSelectionComposite.getRefreshSecurityGroupsAction());
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {}

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        if (securityGroupSelectionComposite != null) {
            securityGroupSelectionComposite.getRefreshSecurityGroupsAction().run();
        }
    }

}
