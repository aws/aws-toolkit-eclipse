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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.amazonaws.eclipse.core.preferences.AccountPreferenceChangeRefreshListener;
import com.amazonaws.eclipse.core.preferences.PreferenceChangeRefreshListener;
import com.amazonaws.eclipse.core.regions.DefaultRegionChangeRefreshListener;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.ui.StatusBar;
import com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener;

/**
 * View for working with EC2 instances.
 */
public class InstanceView extends ViewPart implements IRefreshable, SelectionTableListener {
	
	/** Shared factory for creating EC2 clients */
	private StatusBar statusBar;
	
	/** Table of running instances */
	private InstanceSelectionTable selectionTable;

	/**
	 * Listener for AWS account preference changes (such as access key or secret
	 * access key) that require this view to be refreshed.
	 */
	private final PreferenceChangeRefreshListener accountPreferenceChangeRefreshListener
			= new AccountPreferenceChangeRefreshListener(this);

	/**
	 * Listener for EC2 preference changes (such as current region) that require
	 * this view to be refreshed.
	 */
	private final PreferenceChangeRefreshListener ec2PreferenceChangeRefreshListener 
			= new DefaultRegionChangeRefreshListener(this);


	/* (non-Javadoc)
	 * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
	 */
	public void refreshData() {
	    if (selectionTable != null) {
	        selectionTable.refreshInstances();
	    }
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 2;
		parent.setLayout(layout);

		statusBar = new StatusBar(parent);
		statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		statusBar.setRecordLabel("Displayed Instances: ");
		
		selectionTable = new InstanceSelectionTable(parent);
		selectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		selectionTable.setListener(this);

		refreshData();
		
		contributeToActionBars();
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		if (accountPreferenceChangeRefreshListener != null)
			accountPreferenceChangeRefreshListener.stopListening();
		
		if (ec2PreferenceChangeRefreshListener != null)
			ec2PreferenceChangeRefreshListener.stopListening();
		
		if (statusBar != null) statusBar.dispose();
		
		if (selectionTable != null) selectionTable.dispose();
		
		super.dispose();
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(selectionTable.getRefreshAction());
		
		/** Adds Dropdown filter action for Instance States */
		bars.getToolBarManager().add(selectionTable.getInstanceStateFilterDropDownAction());
		
		/** Adds Dropdown filter action for Security Filter Groups */
		bars.getToolBarManager().add(selectionTable.getSecurityGroupFilterAction());
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		selectionTable.setFocus();
	}

	
	/*
	 * SelectionTableListener Interface
	 */
	
	/* (non-Javadoc)
	 * @see com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener#finishedLoadingData()
	 */
	public void finishedLoadingData(int noOfInstances) {
		statusBar.finishedLoadingData(noOfInstances);
	}

	/* (non-Javadoc)
	 * @see com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener#loadingData()
	 */
	public void loadingData() {
		statusBar.loadingData();
	}
	
}
