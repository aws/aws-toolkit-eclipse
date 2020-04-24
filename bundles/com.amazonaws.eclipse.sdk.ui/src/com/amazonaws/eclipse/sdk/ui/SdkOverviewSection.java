/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.sdk.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.ui.overview.OverviewSection;
import com.amazonaws.eclipse.sdk.ui.wizard.NewAwsJavaProjectWizard;

/**
 * AWS SDK for Java specific section on the AWS Toolkit for Eclipse overview page.
 */
public class SdkOverviewSection extends OverviewSection implements OverviewSection.V2 {

    private static final String SDK_FOR_JAVA_DEVELOPER_GUIDE_URL =
        "http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html?" + AwsUrls.TRACKING_PARAMS;

    /**
     * @see com.amazonaws.eclipse.core.ui.overview.OverviewSection#createControls(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControls(Composite parent) {
        Composite tasksSection = toolkit.newSubSection(parent, "Tasks");
        toolkit.newListItem(tasksSection,
                "Create a New AWS Java Project", null, openNewAwsJavaProjectAction);

        Composite resourcesSection = toolkit.newSubSection(parent, "Additional Resources");
        toolkit.newListItem(resourcesSection,
                "AWS SDK for Java Developer Guide",
                SDK_FOR_JAVA_DEVELOPER_GUIDE_URL);
    }

    /** Action to open the New AWS Java Project wizard in a dialog */
    private static final IAction openNewAwsJavaProjectAction = new Action() {
        @Override
        public void run() {
            NewAwsJavaProjectWizard newWizard = new NewAwsJavaProjectWizard("Overview");
            newWizard.init(PlatformUI.getWorkbench(), null);
            WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
            dialog.open();
        }
    };

}
