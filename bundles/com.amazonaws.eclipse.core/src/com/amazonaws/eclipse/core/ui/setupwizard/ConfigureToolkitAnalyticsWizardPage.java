/*
 * Copyright 2015 Amazon Technologies, Inc.
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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

final class ConfigureToolkitAnalyticsWizardPage extends WizardPage {

    private Button enableAnalyticsButton;

    ConfigureToolkitAnalyticsWizardPage() {
        super("initializeToolkitAnalyticsWizardPage");

        setTitle("Collection of Analytics");
        setDescription("Help us improve AWS Toolkit by enabling analytics data collection?");
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        composite.setLayout(gridLayout);
        setControl(composite);

        Text description = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP);
        description.setText(
                "By leaving this box checked, you agree that AWS may " +
                "collect analytics about your usage of AWS Toolkit (such as " +
                "service/feature usage and view, UI instrumentation usage, AWS " +
                "Toolkit version and user platform). AWS will use this information " +
                "to improve the AWS Toolkit and other Amazon products and services " +
                "and will handle all information received in accordance with the " +
                "AWS Privacy Policy (<http://aws.amazon.com/privacy/>)\n" +
                "When available, an AWS Account ID is associated with this information.\n");
        description.setEditable(false);
        description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        enableAnalyticsButton = new Button(composite, SWT.CHECK | SWT.MULTI | SWT.WRAP);
        enableAnalyticsButton.setText(
                "I acknowledge the legal notice above and agree to let AWS collect" +
                " analytics about my AWS Toolkit usage.");
        enableAnalyticsButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        enableAnalyticsButton.setSelection(true);

    }

    public boolean performFinish() {
        AwsToolkitCore corePlugin = AwsToolkitCore.getDefault();
        if (enableAnalyticsButton.getSelection()) {
            corePlugin.getPreferenceStore()
                    .setValue(
                            PreferenceConstants.P_TOOLKIT_ANALYTICS_COLLECTION_ENABLED,
                            true);
            corePlugin.getAnalyticsManager().setEnabled(true);
        } else {
            corePlugin.getPreferenceStore()
                    .setValue(
                            PreferenceConstants.P_TOOLKIT_ANALYTICS_COLLECTION_ENABLED,
                            false);
            corePlugin.getAnalyticsManager().setEnabled(false);
        }
        return true;
    }

}
