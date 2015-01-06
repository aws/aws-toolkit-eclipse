package com.amazonaws.eclipse.opsworks.explorer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.amazonaws.eclipse.explorer.ExplorerNode;

public class ExplorerSorter extends ViewerSorter {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
        if (e1 instanceof ExplorerNode && e2 instanceof ExplorerNode) {
            ExplorerNode nn1 = (ExplorerNode)e1;
            ExplorerNode nn2 = (ExplorerNode)e2;

            Integer sortPriority1 = nn1.getSortPriority();
            Integer sortPriority2 = nn2.getSortPriority();

            return sortPriority1.compareTo(sortPriority2);
        } else return super.compare(viewer, e1, e2);
    }
}
