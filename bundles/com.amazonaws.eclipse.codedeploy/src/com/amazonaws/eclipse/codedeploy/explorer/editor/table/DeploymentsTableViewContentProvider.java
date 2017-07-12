package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import org.eclipse.jface.viewers.ILazyTreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

class DeploymentsTableViewContentProvider implements ILazyTreePathContentProvider {

    private TreePathContentProvider input;
    private TreeViewer viewer;

    public DeploymentsTableViewContentProvider(TreeViewer viewer,
            TreePathContentProvider input) {
        this.viewer = viewer;
        this.input = input;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput == null) {
            return;
        }

        if ( !(newInput instanceof TreePathContentProvider) ) {
            throw new IllegalStateException(
                    "The new input passed to the DeploymentsTableViewContentProvider " +
                    "is not a TreePathContentProvider!");
        }

        this.viewer = (TreeViewer) viewer;
        this.input = (TreePathContentProvider) newInput;
    }

    @Override
    public void updateElement(TreePath parentPath, int index) {
        Object[] children = input.getChildren(parentPath);

        if (index >= children.length) {
            return;
        }

        viewer.replace(parentPath, index, children[index]);
        updateHasChildren(parentPath.createChildPath(children[index]));
    }

    @Override
    public void updateChildCount(TreePath treePath, int currentChildCount) {
        Object[] children = input.getChildren(treePath);
        viewer.setChildCount(treePath, children.length);
    }

    @Override
    public void updateHasChildren(TreePath path) {
        Object[] children = input.getChildren(path);
        viewer.setHasChildren(path, children.length != 0);
    }

    @Override
    public TreePath[] getParents(Object element) {
        return null;
    }

}
