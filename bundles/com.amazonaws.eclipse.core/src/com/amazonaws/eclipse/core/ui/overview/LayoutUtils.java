/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.overview;

import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * General utilities for SWT layouts.
 */
public class LayoutUtils {
    
    /**
     * Creates a new TableWrapLayout with no margins or padding anywhere.
     * 
     * @param numColumns
     *            The number of columns for the new TableWrapLayout.
     *            
     * @return The new TableWrapLayout.
     */
    public static TableWrapLayout newSlimTableWrapLayout(int numColumns) {
        TableWrapLayout tableWrapLayout = new TableWrapLayout();
        tableWrapLayout.numColumns = numColumns;
        tableWrapLayout.bottomMargin = 0;
        tableWrapLayout.horizontalSpacing = 0;
        tableWrapLayout.leftMargin = 0;
        tableWrapLayout.rightMargin = 0;
        tableWrapLayout.topMargin = 0;
        tableWrapLayout.verticalSpacing = 0;
        return tableWrapLayout;
    }

}
