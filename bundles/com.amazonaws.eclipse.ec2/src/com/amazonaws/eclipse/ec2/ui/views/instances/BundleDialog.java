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
 * Dialog to allow users to specify options for bundling AMIs from instances.
 */
class BundleDialog extends Dialog {

    private Text bucketNameText;
    private Text imageNameText;
    
    private String bucketName;
    private String imageName;

    /**
     * Creates a new Bundle Dialog parented by the specified shell.
     * 
     * @param parentShell
     *            The parent shell for this new Dialog.
     */
    protected BundleDialog(Shell parentShell) {
        super(parentShell);
    }

    public String getS3Bucket() {
        return bucketName;
    }
    
    public String getImageName() {
        return imageName;
    }

    /*
     * Dialog Interface
     */
    
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
        
        this.getShell().setText("Bundle Image");
        
        Composite composite = new Composite(parent, SWT.BORDER);        
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        gridLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        gridLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);

        composite.setLayout(gridLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridData gridData;
                    
        Label label = new Label(composite, SWT.NONE);
        label.setText("Amazon S3 Bucket:");
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.CENTER;
        label.setLayoutData(gridData);

        bucketNameText = new Text(composite, SWT.BORDER);
        bucketNameText.addModifyListener(listener);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 300;
        bucketNameText.setLayoutData(gridData);
        
        
        
        label = new Label(composite, SWT.NONE);
        label.setText("Image Name:");
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.CENTER;
        label.setLayoutData(gridData);
        
        imageNameText = new Text(composite, SWT.BORDER);
        imageNameText.addModifyListener(listener);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 300;
        imageNameText.setLayoutData(gridData);
        

        applyDialogFont(composite);
        
        return composite;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        imageName = imageNameText.getText();
        bucketName = bucketNameText.getText();
        
        super.okPressed();
    }

    /*
     * Private Interface
     */
    
    private void updateOkButton() {
        boolean b = true;
        if (bucketNameText == null || bucketNameText.getText().length() == 0 || imageNameText == null || imageNameText.getText().length() == 0) {
            b = false;
        }
        
        Button okButton = getButton(OK);
        if (okButton == null) {
            return;
        }
        
        okButton.setEnabled(b);
    }

}
