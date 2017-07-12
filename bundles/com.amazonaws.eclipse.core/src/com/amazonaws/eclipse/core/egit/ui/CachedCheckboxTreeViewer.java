/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *******************************************************************************/
package com.amazonaws.eclipse.core.egit.ui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

/**
 * Copy of ContainerCheckedTreeViewer which is specialized for use
 * with {@link FilteredCheckboxTree}.  This container caches
 * the check state of leaf nodes in the tree.  When a filter
 * is applied the cache stores which nodes are checked.  When
 * a filter is removed the viewer can be told to restore check
 * state from the cache.  This viewer updates the check state of
 * parent items the same way as {@link CachedCheckboxTreeViewer}
 * <p>
 * Note: If duplicate items are added to the tree the cache will treat them
 * as a single entry.
 * </p>
 */
public class CachedCheckboxTreeViewer extends ContainerCheckedTreeViewer {

    private final Set<Object> checkState = new HashSet<>();

    /**
     * Constructor for ContainerCheckedTreeViewer.
     * @see CheckboxTreeViewer#CheckboxTreeViewer(Tree)
     */
    protected CachedCheckboxTreeViewer(Tree tree) {
        super(tree);
        addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                updateCheckState(event.getElement(), event.getChecked());
            }
        });
        setUseHashlookup(true);
    }

    /**
     * @param element
     * @param state
     */
    protected void updateCheckState(final Object element, final boolean state) {
        if (state) {
            // Add the item (or its children) to the cache

            ITreeContentProvider contentProvider = null;
            if (getContentProvider() instanceof ITreeContentProvider) {
                contentProvider = (ITreeContentProvider) getContentProvider();
            }

            if (contentProvider != null) {
                Object[] children = contentProvider.getChildren(element);
                if (children != null && children.length > 0) {
                    for (int i = 0; i < children.length; i++) {
                        updateCheckState(children[i], state);
                    }
                } else {
                    checkState.add(element);
                }
            } else {
                checkState.add(element);
            }
        } else if (checkState != null) {
            // Remove the item (or its children) from the cache
            ITreeContentProvider contentProvider = null;
            if (getContentProvider() instanceof ITreeContentProvider) {
                contentProvider = (ITreeContentProvider) getContentProvider();
            }

            if (contentProvider != null) {
                Object[] children = contentProvider.getChildren(element);
                if (children !=null && children.length > 0) {
                    for (int i = 0; i < children.length; i++) {
                        updateCheckState(children[i], state);
                    }

                }
            }
            checkState.remove(element);
        }
    }

    /**
     * Restores the checked state of items based on the cached check state.  This
     * will only check leaf nodes, but parent items will be updated by the container
     * viewer.  No events will be fired.
     */
    public void restoreLeafCheckState() {
        if (checkState == null)
            return;

        getTree().setRedraw(false);
        // Call the super class so we don't mess up the cache
        super.setCheckedElements(new Object[0]);
        setGrayedElements(new Object[0]);
        // Now we are only going to set the check state of the leaf nodes
        // and rely on our container checked code to update the parents properly.
        Iterator iter = checkState.iterator();
        Object element = null;
        if (iter.hasNext())
            expandAll();
        while (iter.hasNext()) {
            element = iter.next();
            // Call the super class as there is no need to update the check state
            super.setChecked(element, true);
        }
        getTree().setRedraw(true);
    }

    /**
     * Returns the contents of the cached check state.  The contents will be all
     * checked leaf nodes ignoring any filters.
     *
     * @return checked leaf elements
     */
    public Object[] getCheckedLeafElements() {
        if (checkState == null) {
            return new Object[0];
        }
        return checkState.toArray(new Object[checkState.size()]);
    }

    /**
     * Returns the number of leaf nodes checked.  This method uses its internal check
     * state cache to determine what has been checked, not what is visible in the viewer.
     * The cache does not count duplicate items in the tree.
     *
     * @return number of leaf nodes checked according to the cached check state
     */
    public int getCheckedLeafCount() {
        if (checkState == null) {
            return 0;
        }
        return checkState.size();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ICheckable#setChecked(java.lang.Object, boolean)
     */
    @Override
    public boolean setChecked(Object element, boolean state) {
        updateCheckState(element, state);
        return super.setChecked(element, state);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setCheckedElements(java.lang.Object[])
     */
    @Override
    public void setCheckedElements(Object[] elements) {
        super.setCheckedElements(elements);
            checkState.clear();

        ITreeContentProvider contentProvider = null;
        if (getContentProvider() instanceof ITreeContentProvider) {
            contentProvider = (ITreeContentProvider) getContentProvider();
        }

        for (int i = 0; i < elements.length; i++) {
            Object[] children = contentProvider != null ? contentProvider.getChildren(elements[i]) : null;
            if (!getGrayed(elements[i]) && (children == null || children.length == 0)) {
                if (!checkState.contains(elements[i])) {
                    checkState.add(elements[i]);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setAllChecked(boolean)
     */
    @Override
    public void setAllChecked(boolean state) {
        for (TreeItem item: super.getTree().getItems())
            item.setChecked(state);
        if (state) {

            // Find all visible children, add only the visible leaf nodes to the check state cache
            Object[] visible = getFilteredChildren(getRoot());

            ITreeContentProvider contentProvider = null;
            if (getContentProvider() instanceof ITreeContentProvider) {
                contentProvider = (ITreeContentProvider) getContentProvider();
            }

            if (contentProvider == null) {
                for (int i = 0; i < visible.length; i++) {
                    checkState.add(visible[i]);
                }
            } else {
                Set<Object> toCheck = new HashSet<>();
                for (int i = 0; i < visible.length; i++) {
                    addFilteredChildren(visible[i], contentProvider, toCheck);
                }
                checkState.addAll(toCheck);
            }
        } else {
            // Remove any item in the check state that is visible (passes the filters)
            if (checkState != null) {
                Object[] visible = filter(checkState.toArray());
                for (int i = 0; i < visible.length; i++) {
                    checkState.remove(visible[i]);
                }
            }
        }
    }

    /**
     * If the element is a leaf node, it is added to the result collection.  If the element has
     * children, this method will recursively look at the children and add any visible leaf nodes
     * to the collection.
     *
     * @param element element to check
     * @param contentProvider tree content provider to check for children
     * @param result collection to collect leaf nodes in
     */
    private void addFilteredChildren(Object element, ITreeContentProvider contentProvider, Collection<Object> result) {
        if (!contentProvider.hasChildren(element)) {
            result.add(element);
        } else {
            Object[] visibleChildren = getFilteredChildren(element);
            for (int i = 0; i < visibleChildren.length; i++) {
                addFilteredChildren(visibleChildren[i], contentProvider, result);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object[])
     */
    @Override
    public void remove(Object[] elementsOrTreePaths) {
        for (int i = 0; i < elementsOrTreePaths.length; i++) {
            updateCheckState(elementsOrTreePaths[i], false);
        }
        super.remove(elementsOrTreePaths);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object)
     */
    @Override
    public void remove(Object elementsOrTreePaths) {
        updateCheckState(elementsOrTreePaths, false);
        super.remove(elementsOrTreePaths);
    }

}