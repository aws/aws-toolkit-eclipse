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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.ui.overview.OverviewSection;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.ImportEnvironmentsWizard;
import com.amazonaws.eclipse.elasticbeanstalk.webproject.NewAwsJavaWebProjectWizard;

/**
 * AWS Elastic Beanstalk specific section on the AWS Toolkit for Eclipse overview page.
 */
public class ElasticBeanstalkOverviewSection extends OverviewSection implements OverviewSection.V2 {

    private static final String ELASTIC_BEANSTALK_GETTING_STARTED_GUIDE_URL = "http://aws.amazon.com/articles/4412341514662386"
            + "?" + AwsUrls.TRACKING_PARAMS;
    private static final String ELASTIC_BEANSTALK_ECLIPSE_SCREENCAST_URL = "http://d1un85p0f2qstc.cloudfront.net/eclipse/elasticbeanstalk/index.html"
            + "?" + AwsUrls.TRACKING_PARAMS;

    @Override
    public void createControls(Composite parent) {
        Composite tasksSection = toolkit.newSubSection(parent, "Tasks");
        toolkit.newListItem(tasksSection, "Create a New AWS Java Web Project", null, openNewAwsJavaWebProjectAction);

        toolkit.newListItem(tasksSection, "Import Existing Environments", null, openImportEnvironmentsWizard);

        Composite resourcesSection = toolkit.newSubSection(parent, "Additional Resources");
        toolkit.newListItem(resourcesSection, "Getting Started with AWS Elastic Beanstalk Deployment in Eclipse",
                ELASTIC_BEANSTALK_GETTING_STARTED_GUIDE_URL);
        this.toolkit.newListItem(resourcesSection, "Video: Overview of AWS Elastic Beanstalk Deployment in Eclipse",
                ELASTIC_BEANSTALK_ECLIPSE_SCREENCAST_URL);
    }

    /** Action to open the New AWS Java Project wizard in a dialog */
    private static final IAction openNewAwsJavaWebProjectAction = new Action() {
        @Override
        public void run() {
            NewAwsJavaWebProjectWizard newWizard = new NewAwsJavaWebProjectWizard();
            newWizard.init(PlatformUI.getWorkbench(), null);
            WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), newWizard);
            dialog.open();
        }
    };

    private static final IAction openImportEnvironmentsWizard = new Action() {
        @Override
        public void run() {
            ImportEnvironmentsWizard newWizard = new ImportEnvironmentsWizard();
            WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), newWizard);
            dialog.open();
        }
    };

}
