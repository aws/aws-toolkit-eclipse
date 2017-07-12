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

package com.amazonaws.eclipse.ec2.ui.amis;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AccountAndRegionChangeListener;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.ui.StatusBar;
import com.amazonaws.services.ec2.model.Image;

/**
 * Composite combining the AMI selection table with a status bar, filter text
 * box to filter AMI results, and listeners to update the displayed AMIs when
 * relevant preferences change (ex: AWS account preferences, EC2 region
 * preferences, etc).
 */
public class FilteredAmiSelectionTable extends Composite implements IRefreshable {

    /** The AMI selection table users select from. */
    private AmiSelectionTable amiSelectionTable;

    /**
     * Listener for the AMI selection table to display data loading messages,
     * etc.
     */
    private StatusBar statusBar;

    /**
     * Listener for AWS account and region preference changes that require this
     * view to be refreshed.
     */
    private final AccountAndRegionChangeListener accountAndRegionChangeListener = new AccountAndRegionChangeListener() {
        @Override
        public void onAccountOrRegionChange() {
            FilteredAmiSelectionTable.this.refreshData();
        }
    };

    /**
     * Constructs a new filtered AMI selection table in the specified parent.
     *
     * @param parent
     *            The parent composite in which to create the new AMI selection
     *            composite.
     */
    public FilteredAmiSelectionTable(Composite parent) {
        this(parent, null, 0);
    }

    /**
     * Constructs a new table with an in-lined tool bar
     *
     * @param numButtons
     *            The number of widgets in the toolbar, for layout purposes.
     *
     * @see FilteredAmiSelectionTable#FilteredAmiSelectionTable(Composite)
     */
    public FilteredAmiSelectionTable(Composite parent, ToolBarManager toolbar, int numButtons) {
        super(parent, SWT.BORDER);

        // Start listening to preference changes
        AwsToolkitCore.getDefault().getAccountManager().addAccountInfoChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().getAccountManager().addDefaultAccountChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().addDefaultRegionChangeListener(accountAndRegionChangeListener);

        GridLayout gridLayout = new GridLayout(2 + numButtons, false);
        gridLayout.marginTop = 4;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 3;
        setLayout(gridLayout);

        statusBar = new StatusBar(this);
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusBar.setRecordLabel("Displayed AMIs: ");

        if ( toolbar != null ) {
            toolbar.createControl(this);
        }

        final Text text = new Text(this, SWT.SEARCH | SWT.CANCEL);
        GridData gridData = new GridData();
        gridData.widthHint = 150;
        text.setLayoutData(gridData);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (amiSelectionTable != null)
                    amiSelectionTable.filterImages(text.getText());
            }
        });

        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.detail != SWT.CANCEL && amiSelectionTable != null) {
                    amiSelectionTable.filterImages(text.getText());
                }
            }
        });

        amiSelectionTable = new AmiSelectionTable(this, statusBar);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2 + numButtons;
        gridData.heightHint = 250;
        gridData.minimumWidth = 350;
        amiSelectionTable.setLayoutData(gridData);
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        AwsToolkitCore.getDefault().getAccountManager().removeAccountInfoChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().getAccountManager().removeDefaultAccountChangeListener(accountAndRegionChangeListener);
        AwsToolkitCore.getDefault().removeDefaultRegionChangeListener(accountAndRegionChangeListener);

        if (statusBar != null) statusBar.dispose();
        if (amiSelectionTable != null) amiSelectionTable.dispose();

        super.dispose();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.core.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        amiSelectionTable.getRefreshAction().run();
    }

    /**
     * Adds the specified selection listener to this control.
     *
     * @param listener
     *            The selection listener to add to this control.
     *
     * @see AmiSelectionTable#addSelectionListener(SelectionListener)
     */
    public void addSelectionListener(SelectionListener listener) {
        amiSelectionTable.addSelectionListener(listener);
    }

    /**
     * Returns the selected Amazon Machine Image (AMI), or null if there
     * isn't one selected.
     *
     * @return The selected AMI, or null if there isn't one selected.
     *
     * @see AmiSelectionTable#getSelectedImage()
     */
    public Image getSelectedImage() {
        return amiSelectionTable.getSelectedImage();
    }

    /**
     * Returns the AMI selection table contained in this filtered AMI selection
     * table.
     *
     * @return The AMI selection table contained in this filtered AMI selection
     *         table.
     */
    public AmiSelectionTable getAmiSelectionTable() {
        return amiSelectionTable;
    }

}
