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

package com.amazonaws.eclipse.ec2.ui.securitygroups;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.ec2.ui.SwappableComposite;

/**
 * Dialog to collect user input on a security group permission entry.
 */
public class EditSecurityGroupPermissionEntryDialog extends Dialog {

    private final String securityGroupName;

    private SwappableComposite swappableComposite;
    private UserGroupPermissionComposite userGroupComposite;
    private PortRangePermissionComposite portRangeComposite;
    private Button userGroupPermissionButton;
    private Button portRangePermissionButton;
    
    private boolean isUserGroupPermission;
    
    public EditSecurityGroupPermissionEntryDialog(Shell parentShell, String securityGroupName) {
        super(parentShell);
                
        this.securityGroupName = securityGroupName;
    }

    public UserGroupPermissionComposite getUserGroupPermissionComposite() {
        return userGroupComposite;
    }
    
    public PortRangePermissionComposite getPortRangePermissionComposite() {
        return portRangeComposite;
    }

    public boolean isUserGroupPermission() {
        return isUserGroupPermission;
    }
    
    
    /*
     * Dialog Interface
     */
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        
        updateOkButton();
        
        return control;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = createComposite(parent);

        createLabel("Assign permissions by:", composite, 1);
        portRangePermissionButton = createRadioButton("Protocol, port and network", composite, 2);
        userGroupPermissionButton = createRadioButton("AWS user and group", composite, 2);    

        swappableComposite = new SwappableComposite(composite, SWT.NONE);
        swappableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        userGroupComposite = new UserGroupPermissionComposite(swappableComposite); 
        portRangeComposite = new PortRangePermissionComposite(swappableComposite);    
        

        SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.getSource().equals(userGroupPermissionButton)) {
                    swappableComposite.setActiveComposite(userGroupComposite);
                } else {
                    swappableComposite.setActiveComposite(portRangeComposite);
                }
            }            
        };
        
        userGroupPermissionButton.addSelectionListener(listener);
        portRangePermissionButton.addSelectionListener(listener);

        return composite;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#create()
     */
    @Override
    public void create() {
        super.create();
        
        Point p = portRangeComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Rectangle r = userGroupComposite.getParent().getClientArea();

        if (r.width > p.x) p.x = r.width;
        
        swappableComposite.setSize(p);
        
        // Start off with the user / group permission style selected
        portRangePermissionButton.setSelection(true);
        swappableComposite.setActiveComposite(portRangeComposite);
        
        getShell().layout();
        getShell().setSize(getShell().computeSize(SWT.DEFAULT,SWT.DEFAULT));
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        userGroupComposite.saveFieldValues();
        portRangeComposite.saveFieldValues();
        
        this.isUserGroupPermission = userGroupPermissionButton.getSelection();
        
        super.okPressed();
    }

    
    /*
     * Private Classes
     */
    
    /**
     * Composite with options for defining security group permissions
     * by AWS user IDs and groups.
     */
    class UserGroupPermissionComposite extends Composite {
        private Text securityGroupText;
        private Text userIdText;

        private String securityGroup;
        private String userId;
        
        /**
         * Creates a new composite with the specified parent.
         * 
         * @param parent The parent for this composite.
         */
        public UserGroupPermissionComposite(Composite parent) {
            super(parent, SWT.NONE);
            
            create();
        }
        
        public void saveFieldValues() {
            securityGroup = securityGroupText.getText();
            userId = userIdText.getText();
        }
        
        public String getSecurityGroup() {
            return securityGroup;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public boolean isComplete() {
            if (securityGroupText == null || securityGroupText.getText().length() == 0) return false;
        
            if (userIdText == null || userIdText.getText().length() == 0) return false;
            
            return true;
        }

        private void create() {
            GridLayout gridLayout = new GridLayout(1, true);
            gridLayout.marginWidth = 0;
            setLayout(gridLayout);
            
            createLabel("AWS User ID:", this, 1);
            userIdText = createTextBox(this, 1);
            
            createLabel("AWS Security Group:", this, 1);
            securityGroupText = createTextBox(this, 1);
            
            pack();
        }
        
    }
    
    /**
     * Composite with options for defining security group permissions
     * by protocol, port and network mask.
     */
    class PortRangePermissionComposite extends Composite {
        private Combo protocolCombo;
        private Text portText;
        private Text networkText;

        private String protocol;
        private String network;

        private int fromPort;
        private int toPort;
        private final Pattern networkMaskPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+");
        
        /**
         * Creates new composite with the specified parent.
         * 
         * @param parent The parent of this composite.
         */
        public PortRangePermissionComposite(Composite parent) {
            super (parent, SWT.NONE);
            
            create();
        }

        public String getNetwork() {
            return network;
        }
        
        public int getFromPort() {
            return fromPort;
        }
        
        public int getToPort() {
            return toPort;
        }
        
        public String getProtocol() {
            return protocol.toLowerCase();
        }
        
        public void saveFieldValues() {
            protocol = protocolCombo.getText();
            network = networkText.getText();

            Integer[] ports = extractPortRange();
            if (ports != null) {
                fromPort = ports[0];
                toPort = ports[1];
            }
        }

        /**
         * Parses the user entered port value and extracts a port range returned
         * as a two element list of integers. If invalid input is detected null
         * is returned.
         * 
         * @return An array of two elements where the first element is the
         *         starting port in the port range and the second is the ending
         *         port in the port range.  If invalid input was entered null
         *         will be returned.
         */
        private Integer[] extractPortRange() {
            if (portText == null) return null;
            
            String portInfo = portText.getText().trim();
            
            int port1, port2;    
            if (portInfo.contains("-") && !portInfo.startsWith("-")) {
                String[] strings = portInfo.split("-");

                if (strings.length != 2) return null;
                
                try {
                    port1 = Integer.parseInt(strings[0].trim());
                    port2 = Integer.parseInt(strings[1].trim());
                } catch (NumberFormatException nfe) {
                    return null;
                }
            } else {
                try {
                    port1 = Integer.parseInt(portInfo.trim());
                    port2 = port1;
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
            
            if (port2 < port1) return null;
            
            return new Integer[] {port1, port2};
        }
                    
        public boolean isComplete() {
            if (extractPortRange() == null) return false;
        
            if (networkText == null) return false;
            Matcher matcher = networkMaskPattern.matcher(networkText.getText());
            if (!matcher.matches()) return false;
            
            return true;
        }

        private void create() {
            GridLayout gridLayout = new GridLayout(1, true);
            gridLayout.verticalSpacing = 2;
            gridLayout.marginHeight = 2;
            gridLayout.marginTop = 2;
            gridLayout.marginWidth = 0;
            setLayout(gridLayout);
            
            createLabel("Protocol:", this, 1);
            protocolCombo = new Combo(this, SWT.NONE | SWT.READ_ONLY);
            protocolCombo.add("TCP");
            protocolCombo.add("UDP");
            protocolCombo.add("ICMP");
            protocolCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            protocolCombo.select(0);
            
            createLabel("Port or Port Range:", this, 1);
            portText = createTextBox(this, 1);
            
            createLabel("Network Mask:", this, 1);
            networkText = createTextBox(this, 1);
            networkText.setText("0.0.0.0/0");
            
            pack();            
        }
    }
    
    private Button createRadioButton(String text, Composite parent, int columnSpan) {
        Button b = new Button(parent, SWT.RADIO);
        b.setText(text);
        
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = columnSpan;
        b.setLayoutData(gridData);
        
        b.addSelectionListener(selectionListener);
        
        return b;        
    }

    /*
     * Private Methods
     */
    
    private Label createLabel(String text, Composite parent, int columnSpan) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_END;
        gridData.horizontalSpan = columnSpan;
        gridData.verticalIndent = 2;
        
        label.setLayoutData(gridData);

        return label;
    }

    private Text createTextBox(Composite parent, int columnSpan) {
        Text text = new Text(parent, SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = columnSpan;
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
        text.setLayoutData(gridData);

        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateOkButton();
            }
        });
        
        text.addSelectionListener(selectionListener);

        return text;
    }

    private SelectionListener selectionListener = new SelectionListener() {
        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
        @Override
        public void widgetSelected(SelectionEvent e) {
            updateOkButton();
        }
    };
    
    private Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);        
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gridLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        gridLayout.marginHeight = 2;
        
        composite.setLayout(gridLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        return composite;
    }
    
    private void updateOkButton() {
        if (userGroupComposite == null || portRangeComposite == null) return;
        
        boolean b = false;
        if (userGroupPermissionButton.getSelection()) {
            b = userGroupComposite.isComplete();
        } else {
            b = portRangeComposite.isComplete();
        }
        
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(b);
        }
    }
}
