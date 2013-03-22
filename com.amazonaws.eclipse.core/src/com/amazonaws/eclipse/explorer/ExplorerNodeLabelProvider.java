/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class ExplorerNodeLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        return getExplorerNodeText(element);
    }
    
    @Override
    public Image getImage(Object element) {
        if (element instanceof ExplorerNode) {
            ExplorerNode navigatorNode = (ExplorerNode)element;
            return navigatorNode.getImage();
        }
        return getDefaultImage(element);
    }
    
    public String getExplorerNodeText(Object element) {
        if (element instanceof ExplorerNode) {
            ExplorerNode navigatorNode = (ExplorerNode)element;
            return navigatorNode.getName();
        }
        return element.toString();
    }

    /**
     * Subclasses can override this method to provide their own default image to
     * display for nodes in the Explorer view.
     * 
     * @param element
     *            The element whose default image is being requested.
     * 
     * @return The default image to display for nodes in the Explorer view.
     */
    public Image getDefaultImage(Object element) {
        return null;
    }

}
