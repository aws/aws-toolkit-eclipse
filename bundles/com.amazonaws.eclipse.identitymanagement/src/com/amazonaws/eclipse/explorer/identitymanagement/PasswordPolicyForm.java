/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.identitymanagement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetAccountPasswordPolicyResult;
import com.amazonaws.services.identitymanagement.model.PasswordPolicy;
import com.amazonaws.services.identitymanagement.model.UpdateAccountPasswordPolicyRequest;

class PasswordPolicyForm extends Composite {

    private Label headerLabel;
    private Text minCharacterLable;
    private Button requireUpppercaseButton;
    private Button requireLowercaseButtion;
    private Button requireNumbersButton;
    private Button requireSymbolsButton;
    private Button allowChangePasswordButton;
    private Button applyPolicyButton;
    private Button deletePolicyButton;
    private AmazonIdentityManagement iam;

    public PasswordPolicyForm(AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);

        this.iam = iam;

        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults()
                .align(SWT.BEGINNING, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);

        this.setLayout(new GridLayout(2, false));
        this.setBackground(toolkit.getColors().getBackground());

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalSpan = 2;

        Composite comp = toolkit.createComposite(this);
        comp.setLayoutData(gridData);

        comp.setLayout(new GridLayout(2, false));

        toolkit.createLabel(comp, "").setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_INFORMATION));
        headerLabel = toolkit.createLabel(comp, "");
        gridDataFactory.copy().minSize(SWT.MAX, SWT.MAX).applyTo(headerLabel);

        toolkit.createLabel(this, "Minimum Password Length:");
        minCharacterLable = new Text(this, SWT.BORDER | SWT.WRAP  | SWT.NONE);
        gridDataFactory.applyTo(minCharacterLable);

        requireUpppercaseButton = toolkit.createButton(this, "", SWT.CHECK);
        toolkit.createLabel(this, "Require at least one uppercase letter");
        requireLowercaseButtion = toolkit.createButton(this, "", SWT.CHECK);
        toolkit.createLabel(this, "Require at least one lowercase letter");

        requireNumbersButton = toolkit.createButton(this, "", SWT.CHECK);
        toolkit.createLabel(this, "Require at least one number");

        requireSymbolsButton = toolkit.createButton(this, "", SWT.CHECK);
        toolkit.createLabel(this, "Require at least one non-alphanumeric character");

        allowChangePasswordButton = toolkit.createButton(this, "", SWT.CHECK);
        toolkit.createLabel(this, "Allow users to change their own password");

        applyPolicyButton =  toolkit.createButton(this, "Apply Password Policy", SWT.PUSH);
        applyPolicyButton.addSelectionListener(new AddPasswordPolicySelectionListener());

        deletePolicyButton = toolkit.createButton(this, "Delete Password Policy", SWT.PUSH);
        deletePolicyButton.addSelectionListener(new DeletePasswordPolicySelectionLister());

        refresh();
    }

    private class AddPasswordPolicySelectionListener implements SelectionListener {
        @Override
        public void widgetSelected(SelectionEvent e) {

            final UpdateAccountPasswordPolicyRequest updateAccountPasswordPolicyRequest = new UpdateAccountPasswordPolicyRequest();
            updateAccountPasswordPolicyRequest.setAllowUsersToChangePassword(allowChangePasswordButton.getSelection());
            updateAccountPasswordPolicyRequest.setMinimumPasswordLength(Integer.parseInt(minCharacterLable.getText()));
            updateAccountPasswordPolicyRequest.setRequireLowercaseCharacters(requireLowercaseButtion.getSelection());
            updateAccountPasswordPolicyRequest.setRequireUppercaseCharacters(requireUpppercaseButton.getSelection());
            updateAccountPasswordPolicyRequest.setRequireSymbols(requireSymbolsButton.getSelection());
            updateAccountPasswordPolicyRequest.setRequireNumbers(requireNumbersButton.getSelection());

            new Job("Adding password policy") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        iam.updateAccountPasswordPolicy(updateAccountPasswordPolicyRequest);
                        refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to add the password policy: " + e.getMessage(), e);
                    }

                }
            }.schedule();

        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            return;
        }

    }

      private class DeletePasswordPolicySelectionLister implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {
            new Job("Deleting password policy") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        iam.deleteAccountPasswordPolicy();
                        refresh();
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return new Status(Status.ERROR, IdentityManagementPlugin.getDefault().getPluginId(), "Unable to delete the password policy: " + e.getMessage(), e);
                    }

                }
            }.schedule();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            return;
        }

      }

    public void refresh() {
        new LoadPasswordPolicyThread().start();

    }

    private class LoadPasswordPolicyThread extends Thread {

        private PasswordPolicy getPasswordPolicy() {
             GetAccountPasswordPolicyResult getAccountPasswordPolicyResult = null;
             try {
                 getAccountPasswordPolicyResult = iam.getAccountPasswordPolicy();
             } catch (Exception e) {
                 return null;
             }

             return getAccountPasswordPolicyResult.getPasswordPolicy();
        }

         @Override
         public void run() {

             final PasswordPolicy passwordPolicy = getPasswordPolicy();
             try {

                 Display.getDefault().asyncExec(new Runnable() {
                     @Override
                    public void run() {
                         if (passwordPolicy != null) {
                         headerLabel.setText("Modify your existing password policy below.");
                         minCharacterLable.setText(passwordPolicy.getMinimumPasswordLength().toString());
                         requireLowercaseButtion.setSelection(passwordPolicy.getRequireLowercaseCharacters());
                         requireUpppercaseButton.setSelection(passwordPolicy.getRequireUppercaseCharacters());
                         requireNumbersButton.setSelection(passwordPolicy.getRequireNumbers());
                         requireSymbolsButton.setSelection(passwordPolicy.getRequireSymbols());
                         allowChangePasswordButton.setSelection(passwordPolicy.getAllowUsersToChangePassword());
                         deletePolicyButton.setEnabled(true);

                         } else {
                             headerLabel.setText("Currently, this AWS account does not have a password policy. Specify a password policy below.");
                             minCharacterLable.setText("");
                             requireLowercaseButtion.setSelection(false);
                             requireUpppercaseButton.setSelection(false);
                             requireNumbersButton.setSelection(false);
                             requireSymbolsButton.setSelection(false);
                             allowChangePasswordButton.setSelection(false);
                             deletePolicyButton.setEnabled(false);

                         }
                     }
                 });
             } catch (Exception e) {
                 Status status = new Status(IStatus.WARNING, IdentityManagementPlugin.PLUGIN_ID, "Unable to describe password policy", e);
                 StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
             }
         }
    }
}



