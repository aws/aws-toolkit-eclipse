package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import org.eclipse.jface.viewers.TreePath;

class LoadingContentProvider implements TreePathContentProvider {

    static final Object LOADING = new Object();

    @Override
    public Object[] getChildren(TreePath parent) {
        if (parent.getSegmentCount() == 0) {
            return new Object[] {LOADING};
        } else {
            return new Object[0];
        }
    }

    @Override
    public void refresh() {
    }

}
