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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog allowing the user to select which device an EBS volume should be
 * attached to.
 */
class DeviceDialog extends MessageDialog {

    /** The default device to attach a volume to */
    private static final String DEFAULT_DEVICE = "/dev/sdh";

    /** Stores the selected device after this dialog is disposed */
    private String device;
    
    /** The combo box where the user selects the device */
    private Combo deviceCombo;

    /**
     * Creates a new DeviceDialog ready to be opened.
     */
    public DeviceDialog() {
        super(new Shell(),
                "Select Device",
                null,
                "Select the device to attach this volume to.",
                MessageDialog.QUESTION,
                new String[] {"OK", "Cancel"},
                0);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(2, false));
        
        Label label = new Label(composite, SWT.NONE);
        label.setText("Device:");
        
        deviceCombo = new Combo(composite, SWT.READ_ONLY);
        deviceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        String devicePrefix = "/dev/sd";
        for (char c = 'b'; c <= 'z'; c++) {
            deviceCombo.add(devicePrefix + c);            
        }
        deviceCombo.setText(DEFAULT_DEVICE);
        
        return composite;            
    }
    
    /**
     * Returns the device the user selected (ex: '/dev/sdh').
     * 
     * @return The device the user selected (ex: '/dev/sdh').
     */
    public String getDevice() {
        return device;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.MessageDialog#buttonPressed(int)
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == DeviceDialog.OK) {
            device = deviceCombo.getText();
        }

        super.buttonPressed(buttonId);
    }
    
}
