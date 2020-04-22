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
package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.ui.overview.OverviewSection;
import com.amazonaws.eclipse.ec2.ui.launchwizard.LaunchWizard;

/**
 * Amazon EC2 OverviewSection implementation that provides overview content for
 * the EC2 Management functionality in the AWS Toolkit for Eclipse (links to
 * docs, shortcuts for creating new EC2 Tomcat clusters, etc.)
 */
public class Ec2OverviewSection extends OverviewSection implements OverviewSection.V2 {

    private static final String EC2_ECLIPSE_SCREENCAST_URL =
        "http://d1un85p0f2qstc.cloudfront.net/eclipse/ec2/index.html" + "?" + AwsUrls.TRACKING_PARAMS;

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.core.ui.overview.OverviewSection#createControls(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControls(Composite parent) {
        Composite tasksSection = toolkit.newSubSection(parent, "Tasks");
        toolkit.newListItem(tasksSection, "Launch Amazon EC2 instances",
                null, openEc2LaunchWizard);

        Composite resourcesSection = toolkit.newSubSection(parent, "Additional Resources");
        this.toolkit.newListItem(resourcesSection,
                "Video: Overview of Amazon EC2 Management in Eclipse",
                EC2_ECLIPSE_SCREENCAST_URL, null);
    }

    /** Action to open the AWS EC2 launch wizard */
    private IAction openEc2LaunchWizard = new Action() {
          @Override
          public void run() {
              WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new LaunchWizard("Overview"));
              dialog.open();
          }
    };
}
