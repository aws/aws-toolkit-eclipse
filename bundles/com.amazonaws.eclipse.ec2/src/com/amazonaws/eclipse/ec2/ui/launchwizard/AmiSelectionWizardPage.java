/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.ec2.ui.launchwizard;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.ec2.ui.amis.FilteredAmiSelectionTable;
import com.amazonaws.services.ec2.model.Image;

/**
 * Wizard page for the launch wizard allowing users to select an Amazon Machine
 * Image to launch.
 */
class AmiSelectionWizardPage extends WizardPage {

    /**
     * The composite listing the available AMIs for users to select.
     */
    private FilteredAmiSelectionTable amiSelectionComposite;


    /**
     * Creates a new AMI selection wizard page for the launch wizard.
     */
    public AmiSelectionWizardPage() {
        super("AMI Selection Page", "Select an AMI to launch", null);
        this.setDescription("Select an Amazon Machine Image to launch");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    @Override
    public void dispose() {
        if (amiSelectionComposite != null) amiSelectionComposite.dispose();

        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
     */
    @Override
    public boolean canFlipToNextPage() {
        // Bail out early if the table doesn't exist yet
        if (amiSelectionComposite == null) return false;

        // Make sure the user has selected an AMI
        return amiSelectionComposite.getSelectedImage() != null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NONE);
        control.setLayout(new GridLayout(1, false));
        control.setLayoutData(new GridData(GridData.FILL_BOTH));

        ToolBarManager manager = new ToolBarManager(SWT.None);
        amiSelectionComposite = new FilteredAmiSelectionTable(control, manager, 3);
        amiSelectionComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        amiSelectionComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateErrorMessages();
                getContainer().updateButtons();
            }
        });

        manager.add(amiSelectionComposite.getAmiSelectionTable().getRefreshAction());
        manager.add(amiSelectionComposite.getAmiSelectionTable().getAmiFilterDropDownAction());
        manager.add(amiSelectionComposite.getAmiSelectionTable().getPlatformFilterDropDownAction());
        manager.update(true);
                
        updateErrorMessages();
        this.setControl(control);
    }

    /**
     * Returns the AMI the user selected to launch.
     *
     * @return The AMI the user selected to launch.
     */
    Image getSelectedAmi() {
        if (amiSelectionComposite == null) return null;

        return amiSelectionComposite.getSelectedImage();
    }

    /**
     * Updates the wizard error message with any issues that the user needs to
     * take care of before they can progress to the next page in the wizard.
     */
    private void updateErrorMessages() {
        if (amiSelectionComposite == null) return;

        if (amiSelectionComposite.getSelectedImage() == null) {
            this.setErrorMessage("No AMI selected");
        } else {
            this.setErrorMessage(null);
        }
    }

}
