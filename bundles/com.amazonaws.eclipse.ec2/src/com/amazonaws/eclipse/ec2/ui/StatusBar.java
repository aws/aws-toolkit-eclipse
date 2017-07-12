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

package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.PreferenceLinkListener;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.SelectionTable.SelectionTableListener;

/**
 * Common status bar for AWS views. It allows error messages to be displayed as
 * well as provides feedback on when data is loading and what region is
 * currently selected.
 */
public class StatusBar extends SwappableComposite implements SelectionTableListener, IPropertyChangeListener {
    
    /** Label displaying data loading image indicator */
    private Label progressImageLabel;
    
    /** Label displaying data loading progress text */
    private Label loadingLabel;

    /** The image to display when data is being loaded */
    private final Image progressImage;
    
    /** Link displaying the currently selected EC2 region */
    private Link regionLink;
    
    /** Link displaying this status bar's error message */
    private Link errorLink;
    
    /** The error message this status bar is to display */
    private String errorMessage;

    /** Indicates that this */ 
    private boolean isLoading;
    
    /** The users AWS account info, so that we can update the error message based on its validity */
    private final AccountInfo accountInfo;
    
    /** Label for number of records that gets displayed */ 
    private Label recordLabel;
    
    /** Default value of record count */
    private int recordCount = -1;
    
    /** String for displaying Number of Records */
    private String recordLabelText;
    
    /** GridLayout for the Status Bar contents */
    private GridLayout statusBarGridLayout;
    
    /**
     * Constructs a new StatusBar with the specified parent.
     */
    public StatusBar(Composite parent) {
        super(parent, SWT.NONE);

        statusBarGridLayout = new GridLayout(2, false);   
        this.setLayout(statusBarGridLayout);
        
        progressImage = Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh").createImage();

        accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
        
        AwsToolkitCore.getDefault().getPreferenceStore().addPropertyChangeListener(this);
        
        updateStatusBar();
        validateAccountInfo();
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (progressImage != null) {
            progressImage.dispose();
        }

        AwsToolkitCore.getDefault().getPreferenceStore().removePropertyChangeListener(this);
        
        super.dispose();
    }
    
    /*
     * AmiSelectionTableListener Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.amis.AmiSelectionTable.AmiSelectionTableListener#finishedLoadingAmis()
     */
    @Override
    public void finishedLoadingData(int recordCount) {
        this.recordCount = recordCount;
        isLoading = false;

        updateStatusBar();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.amis.AmiSelectionTable.AmiSelectionTableListener#loadingAmis()
     */
    @Override
    public void loadingData() {
        isLoading = true;
        updateStatusBar();
    }

    
    /*
     * IPropertyChangeListener Interface
     */
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        validateAccountInfo();
    }

    
    /*
     * Private Interface
     */

    /**
     * Updates the StatusBar based on the current status to decide what
     * information to display. Error messages always preempt anything else so
     * are shown no matter what other status is set. If no error messages are
     * set and no data is loading, then the current region will be display as a
     * clickable Link.
     */
    private void updateStatusBar() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                if ( !isDisposed() ) {
                    if ( errorMessage != null ) {
                        clearRegionLink();
                        clearLoadingMessageLabel();
                        displayErrorLink();

                    } else if ( isLoading ) {
                        clearErrorLink();
                        clearRegionLink();
                        displayLoadingMessageLabel();

                    } else {
                        clearErrorLink();
                        clearLoadingMessageLabel();
                        displayRegionLink();
                    }

                    layout();
                    getParent().layout();
                }
            }
        });
    }

    /**
     * Validates the current AWS account settings and appropriately updates the
     * status bar.
     */
    private void validateAccountInfo() {
        if (accountInfo.isValid()) {
            this.errorMessage = null;
        } else {
            errorMessage = "<a href=\"" + AwsToolkitCore.ACCOUNT_PREFERENCE_PAGE_ID + "\">" +
                    "AWS account not configured correctly</a>";
        }

        updateStatusBar();
    }
    
    /**
     * Removes the error message link from this status bar.
     */
    private void clearErrorLink() {
        // Bail out if it's not there
        if (errorLink == null) return;
        
        errorLink.dispose();
        errorLink = null;
    }
    
    /**
     * Displays the error message link in this status bar.
     */
    private void displayErrorLink() {
        if (errorLink == null) {
            errorLink = new Link(this, SWT.NONE);
            errorLink.setText(errorMessage);
            errorLink.addListener(SWT.Selection, new PreferenceLinkListener());
            errorLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));                
        }
    }
        
    /**
     * Displays the loading message label in this status bar.
     */
    private void displayLoadingMessageLabel() {
        if (loadingLabel == null) {
            statusBarGridLayout.horizontalSpacing = 5;
            GridData gridData = new GridData(); 
            progressImageLabel = new Label(this, SWT.NONE);
            progressImageLabel.setImage(progressImage);
            progressImageLabel.setLayoutData(gridData);

            loadingLabel = new Label(this, SWT.NONE);
            loadingLabel.setText("Loading...");
        }
    }
    
    /**
     * Removes the loading message label from this status bar.
     */
    private void clearLoadingMessageLabel() {
        if (loadingLabel != null) {
            loadingLabel.dispose();
            loadingLabel = null;
        }
        
        if (progressImageLabel != null) {
            progressImageLabel.dispose();
            progressImageLabel = null;
        }        
    }
    
    /**
     * Displays the current region link in this status bar.
     */
    private void displayRegionLink() {
        // Bail out if the region link is already created
        if (regionLink != null) {
            //Update the AMI label with the current record count
            //recordCount is in reset state implies either it was not computed or some error took place
            if(recordCount > -1)    
                recordLabel.setText(recordLabelText + recordCount);
            return;
        }
        
        Region defaultRegion = RegionUtils.getCurrentRegion();
        String regionName = defaultRegion.getName();
        
        statusBarGridLayout.horizontalSpacing = 30;

        regionLink = new Link(this, SWT.NONE);
        regionLink.setText("Region: <a href=\"" + Ec2Plugin.REGION_PREFERENCE_PAGE_ID + "\">" + regionName + "</a>");
        regionLink.addListener(SWT.Selection, new PreferenceLinkListener());

        recordLabel = new Label(this, SWT.NONE);
        // recordCount is in reset state implies either it was not computed or some error took place
        if (recordCount > -1)    
            recordLabel.setText(recordLabelText + recordCount);
        recordLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    /**
     * Removes the current region link from this status bar.
     */
    private void clearRegionLink() {
        // Bail out if it's not there
        if (regionLink == null) return;
        
        regionLink.dispose();
        regionLink = null;
        recordLabel.dispose();
        recordLabel = null;
    }
    
    /**
     * Sets the text that gets displayed for number of records. This should be potentially be 
     * set in the corresponding view.
     * 
     * @param recordLabelText The string that needs to be displayed (Eg. Number of AMIs, Number of Instances, etc) 
     */
    public void setRecordLabel(String recordLabelText) {
        this.recordLabelText = recordLabelText;
    }

}
