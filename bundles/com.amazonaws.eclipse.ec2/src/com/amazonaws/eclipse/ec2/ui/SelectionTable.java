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

package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.ec2.AmazonEC2;

/**
 * A generic table supporting basic operations (column creation, context menu
 * creation, etc) suitable to use as a base for more complex tables.
 */
public abstract class SelectionTable extends Composite {

    /** The internal table viewer control */
    protected TreeViewer viewer;

    /** An optional listener to notify before and after loading AMIs */
    protected SelectionTableListener selectionTableListener;

    /** The shared user account info */
    private AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountInfo();

    /** True if this selection table allows multiple items to be selected */
    private final boolean allowMultipleSelections;

    /** True if this table should be virtual because of data sizes */
    private final boolean virtualTable;

    /** An optional comparator to control sorting by columns */
    protected SelectionTableComparator comparator;

    /** An optional account ID to use in EC2 requests, otherwise the default will be used */
    protected String accountIdOverride;

    /**
     * Specifying an EC2 region override will cause the EC2 client for this
     * selection table to always use this overridden endpoint, instead of using
     * the user's currently selected endpoint from the system preferences.
     */
    protected Region ec2RegionOverride;

    /**
     * Creates a new selection table with the specified parent.
     *
     * @param parent
     *            The composite object to contain this new selection table.
     */
    public SelectionTable(Composite parent) {
        this(parent, false, false);
    }

    /**
     * Creates a new selection table with the specified parent.
     *
     * @param parent
     *            The composite object to contain this new selection table.
     * @param allowMultipleSelections
     *            True if this new selection table should allow multiple rows to
     *            be selected at once, otherwise false if only one row should be
     *            selectable at a time.
     * @param virtualTable
     *            True if this new selection table should use the SWT VIRTUAL
     *            style, indicating that it will supply a lazy content provider.
     */
    public SelectionTable(Composite parent, boolean allowMultipleSelections, boolean virtualTable) {
        super(parent, SWT.NONE);
        this.allowMultipleSelections = allowMultipleSelections;
        this.virtualTable = virtualTable;

        createControl();
    }

    /**
     * Adds a selection listener to this selection table.
     *
     * @param listener The selection listener to add to this table.
     */
    public void addSelectionListener(SelectionListener listener) {
        viewer.getTree().addSelectionListener(listener);
    }

    /**
     * Clears the selection in this selection table.
     */
    public void clearSelection() {
        viewer.getTree().setSelection(new TreeItem[0]);
    }

    /**
     * Sets the optional SelectionTableListener that will be notified before
     * and after data is loaded. Loading data can be a long operation, so
     * providing a listener allows another class to receive events as the
     * loading status changes so it can update itself appropriately.
     *
     * @param listener
     *            The listener to notify before and after loading data.
     */
    public void setListener(SelectionTableListener listener) {
        this.selectionTableListener = listener;
    }

    /**
     * Sets the optional EC2 region override for this selection table. Setting
     * this value will cause the EC2 client returned by the getClient method to
     * *always* use this EC2 region, and will completely ignore the EC2 region
     * selected in the system preferences.
     *
     * @param region
     *            The EC2 region to use instead of defaulting to the currently
     *            selected EC2 region in the system preferences.
     */
    public void setEc2RegionOverride(Region region) {
        this.ec2RegionOverride = region;
    }

    /**
     * Sets the optional EC2 account ID override for this selection table.
     * Setting this will cause the EC2 client to authenticate requests with the
     * specified account, otherwise the default account is used.
     *
     * @param accountId
     *            The account ID to use for making requests in this selection
     *            table.
     */
    public void setAccountIdOverride(String accountId) {
        this.accountIdOverride = accountId;
    }

    /**
     * Returns the viewer used in this table.
     */
    protected Viewer getViewer() {
        return viewer;
    }

    /**
     * Returns a ready-to-use EC2 client using the currently selected AWS
     * account.
     *
     * @return A fully configured AWS EC2 client.
     */
    protected AmazonEC2 getAwsEc2Client() {
        if (accountIdOverride != null) return getAwsEc2Client(accountIdOverride);
        else return getAwsEc2Client(null);
    }

    /**
     * Returns a ready-to-use EC2 client for the account ID given.
     *
     * TODO: move the account ID into a member variable, deprecate this method
     */
    protected AmazonEC2 getAwsEc2Client(String accountId) {
        if ( ec2RegionOverride != null ) {
            return AwsToolkitCore.getClientFactory(accountId).getEC2ClientByEndpoint(ec2RegionOverride.getServiceEndpoint(ServiceAbbreviations.EC2));
        }

        Region defaultRegion = RegionUtils.getCurrentRegion();
        String regionEndpoint = defaultRegion.getServiceEndpoints().get(ServiceAbbreviations.EC2);
        return AwsToolkitCore.getClientFactory(accountId).getEC2ClientByEndpoint(regionEndpoint);
    }

    /**
     * Returns the current selection in this table.
     *
     * @return The current selection in this table.
     */
    protected Object getSelection() {
        StructuredSelection selection = (StructuredSelection)viewer.getSelection();
        return selection.getFirstElement();
    }

    /**
     * Packs the columns in this selection table.
     */
    protected void packColumns() {
        if (viewer == null) return;

        Tree table = viewer.getTree();
        for (TreeColumn col : table.getColumns()) {
            col.pack();
        }
    }

    /**
     * Creates a new column with the specified text and weight.
     *
     * @param columnText
     *            The text for the column header.
     * @param weight
     *            The weight of the new column, relative to the other columns.
     *
     * @return The new TableColum that was created.
     */
    protected TreeColumn newColumn(String columnText, int weight) {
        Tree table = viewer.getTree();
        TreeColumn column = new TreeColumn(table, SWT.NONE);
        column.setText(columnText);

        TreeColumnLayout tableColumnLayout = (TreeColumnLayout)getLayout();
        tableColumnLayout.setColumnData(column, new ColumnWeightData(weight));

        return column;
    }

    /**
     * Creates and configures the actual tree control.
     */
    protected void createControl() {
        TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
        setLayout(treeColumnLayout);

        int style = SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;

        if (allowMultipleSelections) {
            style |= SWT.MULTI;
        } else {
            style |= SWT.SINGLE;
        }

        if (virtualTable) {
            style |= SWT.VIRTUAL;
        }

        viewer = new TreeViewer(this, style);

        if (virtualTable) viewer.setUseHashlookup(true);

        Tree tree = viewer.getTree();
        tree.setLinesVisible(true);
        tree.setHeaderVisible(true);

        createColumns();
        makeActions();
        hookContextMenu();
    }

    /**
     * Subclasses must implement this to set up the table's columns.
     */
    protected abstract void createColumns();

    /**
     * Subclasses must implement this to create any needed actions.
     */
    protected abstract void makeActions();

    /**
     * Subclasses must implement this to provide any context menu contributions.
     *
     * @param manager
     *            The manager for the table's context menu.
     */
    protected abstract void fillContextMenu(IMenuManager manager);

    /**
     * Hooks a context (popup) menu for the table control.
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new AcountValidatingMenuListener());
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);

        menuMgr.createContextMenu(this);
    }

    /**
     * Sets the comparator used to control sorting by columns in this selection
     * table and adds a selection listener to each column in this table that will
     * update the sorting information on the comparator.
     *
     * @param comparator
     *            The comparator used to control sorting by columns in this
     *            selection table.
     */
    protected void setComparator(SelectionTableComparator comparator) {
        this.comparator = comparator;

        viewer.setComparator(comparator);
        viewer.getTree().setSortColumn(viewer.getTree().getColumn(comparator.getColumn()));
        viewer.getTree().setSortDirection(comparator.getDirection());

        int i = 0;
        for (TreeColumn column : viewer.getTree().getColumns()) {
            column.addSelectionListener(new SelectionTableColumnClickListener(i++, viewer, comparator));
        }
    }

    /**
     * A MenuListener that checks to see if the user's AWS account information has
     * been entered. If it hasn't, the user will be directed to the plugin
     * preferences so they can enter it and start using the tools.
     */
    private final class AcountValidatingMenuListener implements IMenuListener {

        /* (non-Javadoc)
         * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
         */
        @Override
        public void menuAboutToShow(IMenuManager manager) {
            if (accountInfo != null && accountInfo.isValid()) {
                fillContextMenu(manager);
                return;
            }

            manager.add(new SetupAwsAccountAction());
        }
    }

    /**
     * A simple interface that objects can implement if they want to register
     * themselves as a listener for data loading events in this SelectionTable.
     */
    public interface SelectionTableListener {
        /**
         * This method is called to notify the listener that this selection
         * table is currently reloading its data.
         */
        public void loadingData();

        /**
         * This method is called to notify the listener that this selection
         * table has finished loading all data. This doesn't necessarily mean
         * that they were loaded successfully, just that this selection table is
         * no longer querying for data.
         * Will also pass along the number of records that got loaded
         */
        public void finishedLoadingData(int recordCount);
    }
}
