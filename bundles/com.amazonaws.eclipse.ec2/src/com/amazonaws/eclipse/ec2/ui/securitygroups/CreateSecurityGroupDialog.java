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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to collect information on creating a new security group.
 */
public class CreateSecurityGroupDialog extends Dialog {

    /** The Text control for the security group name */
    private Text groupNameText;
    
    /** String to save the security group name after the controls are disposed */ 
    private String groupName;

    /** The Text control for the security group description */
    private Text descriptionText;
    
    /** String to save the security group description after the controls are disposed */
    private String groupDescription;

    
    /**
     * Creates a new security group creation dialog ready to be displayed.
     * 
     * @param parentShell The parent shell for this dialog.
     */
    public CreateSecurityGroupDialog(Shell parentShell) {
        super(parentShell);
    }

    /*
     * Dialog Interface
     */
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = createComposite(parent);

        createLabel("Security Group Name:", composite, 2);
        groupNameText = createTextBox(composite, 2);
        
        createLabel("Description:", composite, 2);
        descriptionText = createTextBox(composite, 2);
        
        return composite;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        groupName = groupNameText.getText();
        groupDescription = descriptionText.getText();
        
        super.okPressed();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {
        Control control = super.createButtonBar(parent);
        
        updateOkButton();
        
        return control;
    }

    
    /*
     * Public Interface
     */
    
    /**
     * Returns the security group name that the user entered.
     * 
     * @return The security group name that the user entered.
     */
    public String getSecurityGroupName() {
        return groupName;
    }

    /**
     * Returns the security group description that the user entered.
     * 
     * @return The security group description that the user entered.
     */
    public String getSecurityGroupDescription() {
        return groupDescription;
    }
    
    /*
     * Private Interface
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
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = columnSpan;
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
        text.setLayoutData(gridData);

        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateOkButton();
            }
        });

        return text;
    }

    private Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER);        
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gridLayout.verticalSpacing = convertVerticalDLUsToPixels(1);
        gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

        composite.setLayout(gridLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        return composite;
    }
    
    /**
     * Validates the user input and enables or disables the Ok button as necessary.
     */
    private void updateOkButton() {
        boolean b = true;
        if (descriptionText == null || descriptionText.getText().length() == 0) {
            b = false;
        }
        
        if (groupNameText == null || groupNameText.getText().length() == 0) {
            b = false;
        }
        
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(b);
        }
    }
    
}
