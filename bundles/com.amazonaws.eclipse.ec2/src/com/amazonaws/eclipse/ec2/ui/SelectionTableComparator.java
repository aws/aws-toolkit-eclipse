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
package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;

/**
 * Extension of ViewerComparator that knows which column it is sorting on, which
 * direction, etc. Subclasses must implement the actual comparison logic.
 */
public abstract class SelectionTableComparator extends ViewerComparator {
    
    /** The column on which this comparator is currently sorting */
    protected int sortColumn;
    
    /** The direction in which this comparator is currently sorting */
    protected int sortDirection = SWT.UP;

    
    /**
     * Creates a new SelectionTableComparator set up to sort on the
     * specified default sort column.
     * 
     * @param defaultColumn
     *            The default column on which to sort.
     */
    public SelectionTableComparator(int defaultColumn) {
        sortColumn = defaultColumn;
    }

    /**
     * Reverses the direction that this comparator is currently sorting.
     */
    public void reverseDirection() {
        sortDirection = (sortDirection == SWT.UP) ? SWT.DOWN : SWT.UP;
    }

    /**
     * Returns the column currently being sorted on.
     * 
     * @return The column currently being sorted on.
     */
    public int getColumn() {
        return sortColumn;
    }

    /**
     * Returns the direction this comparator is currently sorting (SWT.UP or
     * SWT.DOWN).
     * 
     * @return The direction this comparator is currently sorting (SWT.UP or
     *         SWT.DOWN).
     */
    public int getDirection() {
        return sortDirection;
    }

    /**
     * Sets the column on which this comparator sorts.
     * 
     * @param sortColumn
     *            The column on which this comparator sorts.
     */
    public void setColumn(int sortColumn) {
        this.sortColumn = sortColumn;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        int comparison = compareIgnoringDirection(viewer, e1, e2);
        
        return (sortDirection == SWT.UP) ? comparison : -comparison;
    }

    /**
     * Returns a negative, zero, or positive number depending on whether the
     * first element is less than, equal to, or greater than the second
     * element. Subclasses must implement this method to do their actually
     * comparison logic, but don't need to worry about the direction of this
     * comparator since the compare(Viewer, Object, Object) method will take
     * that into account before it returns the value produced by this
     * method.
     * 
     * @param viewer
     *            The viewer containing the data being compared.
     * @param o1
     *            The first object being compared.
     * @param o2
     *            The second object being compared.
     * 
     * @return a negative number if the first element is less than the
     *         second element; the value <code>0</code> if the first element
     *         is equal to the second element; and a positive number if the
     *         first element is greater than the second element.
     */
    protected abstract int compareIgnoringDirection(Viewer viewer, Object o1, Object o2);
    
}
