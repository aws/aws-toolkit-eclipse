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

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;

public class InitialSetupWizard extends Wizard {

    private static final String DEFAULT_ACCOUNT_NAME = "default";

    private InitialSetupWizardDataModel dataModel = new InitialSetupWizardDataModel();

    private WizardPage configureAccountWizardPage;


    public InitialSetupWizard() {
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
        IPreferenceStore preferenceStore = AwsToolkitCore.getDefault().getPreferenceStore();
        preferenceStore.setValue(internalAccountId + ":accountName", encodeString(DEFAULT_ACCOUNT_NAME));
        preferenceStore.setValue(internalAccountId + ":accessKey",   encodeString(dataModel.getAccessKeyId()));
        preferenceStore.setValue(internalAccountId + ":secretKey",   encodeString(dataModel.getSecretAccessKey()));
        preferenceStore.setValue("currentAccount", internalAccountId);
        preferenceStore.setValue("accountIds", internalAccountId);

        if (preferenceStore instanceof IPersistentPreferenceStore) {
            IPersistentPreferenceStore persistentPreferenceStore = (IPersistentPreferenceStore)preferenceStore;
            try {
                persistentPreferenceStore.save();
            } catch (IOException e) {
                String errorMessage = "Unable to open the AWS Explorer view: " + e.getMessage();
                Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }

        if (dataModel.isOpenExplorer()) openAwsExplorer();

        return true;
    }

    private static String encodeString(String s) {
        return new String(Base64.encodeBase64(s.getBytes()));
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