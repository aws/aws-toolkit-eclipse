/*
 * Copyright 2008-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2.ui.amis;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.amazonaws.eclipse.core.ui.IRefreshable;

/**
 * View for working with AMIs.
 */
public class AmiBrowserView extends ViewPart implements IRefreshable {

    /** AMI selection table displaying the available AMIs */
    private FilteredAmiSelectionTable filteredAmiSelectionTable;

    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        parent.setLayout(new FillLayout());
    
        filteredAmiSelectionTable = new FilteredAmiSelectionTable(parent);
        filteredAmiSelectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        contributeToActionBars();
    }

    /*
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        if (filteredAmiSelectionTable != null)
            filteredAmiSelectionTable.dispose();
        
        super.dispose();
    }

    /**
     * Adds a refresh button to this view's toolbar. 
     */
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager manager = bars.getToolBarManager();
        manager.add(filteredAmiSelectionTable.getAmiSelectionTable().getRefreshAction());
        manager.add(filteredAmiSelectionTable.getAmiSelectionTable().getAmiFilterDropDownAction());
        manager.add(filteredAmiSelectionTable.getAmiSelectionTable().getPlatformFilterDropDownAction());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        filteredAmiSelectionTable.setFocus();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        if (filteredAmiSelectionTable != null) {
            filteredAmiSelectionTable.refreshData();
        }
    }

}
