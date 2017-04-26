package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import org.eclipse.jface.viewers.TreePath;

interface TreePathContentProvider {

    Object[] getChildren(TreePath parent);

    void refresh();

}
