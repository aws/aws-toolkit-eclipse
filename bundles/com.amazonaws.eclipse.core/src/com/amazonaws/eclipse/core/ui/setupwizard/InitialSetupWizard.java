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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;

public class InitialSetupWizard extends Wizard {

    private final InitialSetupWizardDataModel dataModel = new InitialSetupWizardDataModel();

    private final IPreferenceStore preferenceStore;
    private final boolean showAccountInitPage;
    private final boolean showAnalyticsInitPage;

    private ConfigureAccountWizardPage configureAccountWizardPage;
    private ConfigureToolkitAnalyticsWizardPage configureAnalyticsWizardPage;

    public InitialSetupWizard(boolean showAccountInitPage,
            boolean showAnalyticsInitPage, IPreferenceStore preferenceStore) {

        this.preferenceStore = preferenceStore;
        this.showAccountInitPage = showAccountInitPage;
        this.showAnalyticsInitPage = showAnalyticsInitPage;

        setNeedsProgressMonitor(false);
        setDefaultPageImageDescriptor(
            AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
    }

    @Override
    public void addPages() {
        if (showAccountInitPage && configureAccountWizardPage == null) {
            configureAccountWizardPage = new ConfigureAccountWizardPage(dataModel, preferenceStore);
            addPage(configureAccountWizardPage);
        }
        if (showAnalyticsInitPage && configureAnalyticsWizardPage == null) {
            configureAnalyticsWizardPage = new ConfigureToolkitAnalyticsWizardPage();
            addPage(configureAnalyticsWizardPage);
        }
    }

    @Override
    public boolean performFinish() {
        boolean finished = true;
        if (configureAccountWizardPage != null) {
            finished = finished && configureAccountWizardPage.performFinish();
        }
        if (configureAnalyticsWizardPage != null) {
            finished = finished && configureAnalyticsWizardPage.performFinish();
        }
        return finished;
    }
}