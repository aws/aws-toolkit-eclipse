/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.identitymanagement;

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
