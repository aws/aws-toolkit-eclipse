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

package com.amazonaws.eclipse.ec2.ui.elasticip;


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

/**
 * An Eclipse view displaying a table of elastic IPs.
 */
public class ElasticIpView extends ViewPart {
    private ElasticIpComposite elasticIPTable;

    /**
     * This is a callback that will allow us
     * to create the viewer and initialize it.
     */
    @Override
    public void createPartControl(Composite parent) {
        elasticIPTable = new ElasticIpComposite(parent);
                
        contributeToActionBars();
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(elasticIPTable.getRefreshAddressesAction());
        manager.add(new Separator());
        manager.add(elasticIPTable.getNewAddressAction());
        manager.add(elasticIPTable.getReleaseAddressAction());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(elasticIPTable.getRefreshAddressesAction());
        manager.add(elasticIPTable.getNewAddressAction());
        manager.add(elasticIPTable.getReleaseAddressAction());
    }
    
    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        elasticIPTable.setFocus();
    }
}
