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

package com.amazonaws.eclipse.ec2.ui.keypair;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;

/**
 * A dialog for creating a new EC2 Key Pair.
 */
class CreateKeyPairDialog extends Dialog {

    private Text keyPairNameText;
    private Text privateKeyDirectoryText;
    
    private String keyPairName;
    private String privateKeyDirectoryName;
    
    private final String accountId;

    protected CreateKeyPairDialog(Shell parentShell, String accountId) {
        super(parentShell);
        this.accountId = accountId;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#create()
     */
    @Override
    public void create() {
        super.create();
        
        this.getShell().setText("Create New Key Pair");
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

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        ModifyListener listener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateOkButton();
            }
        };
        
        Composite composite = new Composite(parent, SWT.BORDER);
        GridLayout gridLayout = new GridLayout(3, false);
        gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gridLayout.verticalSpacing = 0;
        gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

        composite.setLayout(gridLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridData gridData;
                    
        Label label = new Label(composite, SWT.NONE);
        label.setText("Key Pair Name:");
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.CENTER;
        gridData.horizontalSpan = 3;
        label.setLayoutData(gridData);

        keyPairNameText = new Text(composite, SWT.BORDER);
        keyPairNameText.addModifyListener(listener);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        keyPairNameText.setLayoutData(gridData);
        
        label = new Label(composite, SWT.NONE);
        label.setText("Private Key Directory:");
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.CENTER;
        gridData.horizontalSpan = 3;
        label.setLayoutData(gridData);
        
        privateKeyDirectoryText = new Text(composite, SWT.BORDER);
        privateKeyDirectoryText.addModifyListener(listener);
        
        File defaultPrivateKeyDirectory = KeyPairManager.getDefaultPrivateKeyDirectory();
        if (defaultPrivateKeyDirectory != null) {
            privateKeyDirectoryText.setText(defaultPrivateKeyDirectory.getAbsolutePath());
        }
        
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 2;
        gridData.widthHint = 300;
        privateKeyDirectoryText.setLayoutData(gridData);
        
        Button button = new Button(composite, SWT.PUSH);
        button.setText("Browse...");
        button.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}

            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
                String directoryPath = dialog.open();
                
                privateKeyDirectoryText.setText(directoryPath);
            }                
        });
        
        applyDialogFont(composite);
        
        return composite;
    }

    private void updateOkButton() {
        boolean b = true;
        if (keyPairNameText == null || keyPairNameText.getText().length() == 0 || privateKeyDirectoryText == null || privateKeyDirectoryText.getText().length() == 0) {
            b = false;
        }
        
        Button okButton = getButton(OK);
        if (okButton == null) {
            return;
        }
        
        okButton.setEnabled(b);
    }
    
    public String getKeyPairName() {
        return keyPairName;
    }
    
    public String getPrivateKeyDirectory() {
        return privateKeyDirectoryName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        privateKeyDirectoryName = privateKeyDirectoryText.getText();
        keyPairName = keyPairNameText.getText();
        
        super.okPressed();
    }

}
