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
package com.amazonaws.eclipse.core.ui.setupwizard;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.internal.Profile;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.accounts.profiles.SdkProfilesCredentialsConfiguration;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

public class InitialSetupWizard extends Wizard {

    private final InitialSetupWizardDataModel dataModel = new InitialSetupWizardDataModel();
    private final IPreferenceStore preferenceStore;
    private WizardPage configureAccountWizardPage;


    public InitialSetupWizard(IPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
        setNeedsProgressMonitor(false);
        setDefaultPageImageDescriptor(
            AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
    }

    @Override
    public void addPages() {
        if (configureAccountWizardPage == null) {
            configureAccountWizardPage = new ConfigureAccountWizardPage(dataModel);
            addPage(configureAccountWizardPage);
        }
    }

    @Override
    public boolean performFinish() {
        String internalAccountId = UUID.randomUUID().toString();
        saveToCredentialsFile(internalAccountId);
        preferenceStore.setValue(PreferenceConstants.P_CURRENT_ACCOUNT, internalAccountId);
        preferenceStore.setValue(PreferenceConstants.P_GLOBAL_CURRENT_DEFAULT_ACCOUNT, internalAccountId);
        if (preferenceStore instanceof IPersistentPreferenceStore) {
            IPersistentPreferenceStore persistentPreferenceStore = (IPersistentPreferenceStore)preferenceStore;
            try {
                persistentPreferenceStore.save();
            } catch (IOException e) {
                String errorMessage = "Unable to write the account information to disk: " + e.getMessage();
                Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
        AwsToolkitCore.getDefault().getAccountManager().reloadAccountInfo();

        if (dataModel.isOpenExplorer()) {
            openAwsExplorer();
        }

        return true;
    }

    /**
     * Persist the credentials entered in the wizard to the AWS credentials file
     * @param internalAccountId - Newly generated UUID to identify the account in eclipse
     */
    private void saveToCredentialsFile(String internalAccountId) {
        Profile emptyProfile = new Profile( PreferenceConstants.DEFAULT_ACCOUNT_NAME, new BasicAWSCredentials("", ""));
        SdkProfilesCredentialsConfiguration credentialsConfig = new SdkProfilesCredentialsConfiguration(
                preferenceStore, internalAccountId, emptyProfile);
        credentialsConfig.setAccessKey(dataModel.getAccessKeyId());
        credentialsConfig.setSecretKey(dataModel.getSecretAccessKey());

        try {
            credentialsConfig.save();
        } catch (AmazonClientException e) {
            StatusManager.getManager()
                    .handle(new Status(
                            IStatus.ERROR, AwsToolkitCore.PLUGIN_ID,
                            "Could not write profile information to the credentials file ", e), StatusManager.SHOW);
        }
    }

    private void openAwsExplorer() {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(AwsToolkitCore.EXPLORER_VIEW_ID);
                } catch (PartInitException e) {
                    String errorMessage = "Unable to open the AWS Explorer view: " + e.getMessage();
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
    }
}