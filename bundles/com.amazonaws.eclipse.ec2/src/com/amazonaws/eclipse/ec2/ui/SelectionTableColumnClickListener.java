/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * SelectionListener for SelectionTable columns to control sorting for the
 * associated comparator.
 */
public class SelectionTableColumnClickListener extends SelectionAdapter {
    /** The index of the column associated with this listener */
    private final int columnIndex;
    
    /** The comparator controlling sorting on the associated table */
    private final SelectionTableComparator comparator;

    /** The viewer associated with this column click listener */
    private final TreeViewer viewer;

    /**
     * Creates a new SelectionTableColumnClickListener associated with the
     * specified column and comparator.
     * 
     * @param columnIndex
     *            The index of the column associated with this listener.
     * @param comparator
     *            The comparator that controls sorting on the associated
     *            selection table.
     */
    public SelectionTableColumnClickListener(int columnIndex, TreeViewer viewer, SelectionTableComparator comparator) {
        this.columnIndex = columnIndex;
        this.viewer = viewer;
        this.comparator = comparator;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    @Override
    public void widgetSelected(SelectionEvent e) {
        TreeColumn treeColumn = (TreeColumn)e.getSource();
        viewer.getTree().setSortColumn(treeColumn);

        // If we're already sorting in this column and the user
        // clicks on it, we want to reverse the sort direction.
        if (comparator.getColumn() == columnIndex) {
            comparator.reverseDirection();
        }
        comparator.setColumn(columnIndex);

        viewer.getTree().setSortDirection(comparator.getDirection());
        viewer.refresh();
    }
}
