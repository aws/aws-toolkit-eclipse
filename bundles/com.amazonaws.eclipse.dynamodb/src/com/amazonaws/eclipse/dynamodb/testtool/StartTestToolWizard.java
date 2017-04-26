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
package com.amazonaws.eclipse.dynamodb.testtool;

import org.eclipse.jface.wizard.Wizard;

/**
 * A wizard that starts an instance of the DynamoDBLocal Test Tool.
 */
public class StartTestToolWizard extends Wizard {

    private StartTestToolPickVersionWizardPage versionPage;
    private StartTestToolConfigurationWizardPage portPage;

    /**
     * Create the wizard.
     */
    public StartTestToolWizard() {
        super.setNeedsProgressMonitor(false);
        super.setWindowTitle("Start the DynamoDB Local Test Tool");
    }

    @Override
    public void addPages() {
        // TODO: Could we use an initial page describing what the local test
        // tool is?

        versionPage = new StartTestToolPickVersionWizardPage();
        addPage(versionPage);

        portPage = new StartTestToolConfigurationWizardPage();
        addPage(portPage);
    }

    @Override
    public boolean performFinish() {
        TestToolManager.INSTANCE.startVersion(
            versionPage.getSelectedVersion(),
            portPage.getPort()
        );
        return true;
    }

}
