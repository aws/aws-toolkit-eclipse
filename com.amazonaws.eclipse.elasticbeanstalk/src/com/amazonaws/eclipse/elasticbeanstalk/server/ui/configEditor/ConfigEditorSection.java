/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.overview.HyperlinkHandler;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.Region;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

/**
 * Generic environment config editor section that can create controls for a set
 * of configuration options.
 */
public class ConfigEditorSection extends ServerEditorSection {

    /** The section widget we're managing */
    private Section section;

    private Environment environment;

    private FormToolkit toolkit;

    private Label environmentNameLabel;
    private Label environmentDescriptionLabel;
    private Label regionNameLabel;
    private Label applicationNameLabel;
    private Label applicationVersionLabel;
    private Label statusLabel;
    private Label healthLabel;
    private Label solutionStackLabel;
    private Label createdOnLabel;
    private Label dateUpdatedLabel;
    private Hyperlink environmentUrlHyperlink;
    private Hyperlink owningAccountHyperlink;

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.ui.editor.ServerEditorSection#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        environment = (Environment)server.loadAdapter(Environment.class, null);
    }

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        getManagedForm().getForm().getToolBarManager().add(new Action("Refresh", SWT.None) {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh");
            }

            @Override
            public void run() {
                refreshEnvironmentDetails();
            }
        });
        getManagedForm().getForm().getToolBarManager().update(true);

        toolkit = getFormToolkit(parent.getDisplay());

        section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
            | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("AWS Elastic Beanstalk Environment");
        section.setDescription("Basic information about your environment.");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        GridData horizontalAndVerticalFillGridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);

        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 15;
        composite.setLayout(layout);
        composite.setLayoutData(horizontalAndVerticalFillGridData);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        section.setLayout(layout);
        section.setLayoutData(horizontalAndVerticalFillGridData);

        environmentNameLabel = createLabels(composite, "Environment name: ", environment.getEnvironmentName());
        environmentDescriptionLabel = createLabels(composite, "Environment description: ", environment.getEnvironmentDescription());

        createLabel(toolkit, composite, "Environment URL:");
        environmentUrlHyperlink = toolkit.createHyperlink(composite, "", SWT.NONE);
        environmentUrlHyperlink.addHyperlinkListener(new HyperlinkHandler());

        regionNameLabel = createLabels(composite, "AWS region: ", "");

        applicationNameLabel = createLabels(composite, "Application name: ", environment.getApplicationName());
        applicationVersionLabel = createLabels(composite, "Application version: ", environment.getEnvironmentDescription());

        statusLabel = createLabels(composite, "Status:", "");
        healthLabel = createLabels(composite, "Health: ", "");
        solutionStackLabel = createLabels(composite, "Solution stack: ", "");
        createdOnLabel = createLabels(composite, "Created on: ", "");
        dateUpdatedLabel = createLabels(composite, "Last updated: ", "");

        String accountId = environment.getAccountId();
        String accountName = AwsToolkitCore.getDefault().getAccounts().get(accountId);
        if ( accountName != null ) {
            createLabel(toolkit, composite, "AWS account: ");
            String href = "preference:" + AwsAccountPreferencePage.ID;
            String text = accountName;
            owningAccountHyperlink = toolkit.createHyperlink(composite, text, SWT.None);
            owningAccountHyperlink.setHref(href);
            owningAccountHyperlink.addHyperlinkListener(new HyperlinkHandler());
        }        

        refreshEnvironmentDetails();
    }

    private void refreshEnvironmentDetails() {
        String regionName = environment.getRegionEndpoint();
        Region region = Region.findByEndpoint(regionName);
        if (region != null) regionName = region.getName();
        regionNameLabel.setText(regionName);

        EnvironmentDescription environmentDescription = environment.getCachedEnvironmentDescription();
        if (environmentDescription != null) {
            environmentNameLabel.setText(environmentDescription.getEnvironmentName());
            if ( environmentDescription.getDescription() != null )
                environmentDescriptionLabel.setText(environmentDescription.getDescription());

            String environmentUrl = "http://" + environmentDescription.getCNAME();
            environmentUrlHyperlink.setText(environmentUrl);
            environmentUrlHyperlink.setHref(environmentUrl);

            applicationNameLabel.setText(environmentDescription.getApplicationName());
            applicationVersionLabel.setText(environmentDescription.getVersionLabel());
            statusLabel.setText(environmentDescription.getStatus());

            healthLabel.setText(environmentDescription.getHealth());
            solutionStackLabel.setText(environmentDescription.getSolutionStackName());
            createdOnLabel.setText(environmentDescription.getDateCreated().toString());
            dateUpdatedLabel.setText(environmentDescription.getDateUpdated().toString());
        } else {
            environmentNameLabel.setText(environment.getEnvironmentName());
            if ( environment.getEnvironmentDescription() != null )
                environmentDescriptionLabel.setText(environment.getEnvironmentDescription());

            environmentUrlHyperlink.setText("");
            environmentUrlHyperlink.setHref("");

            applicationNameLabel.setText(environment.getApplicationName());
            applicationVersionLabel.setText("");
            statusLabel.setText("");

            healthLabel.setText("");
            solutionStackLabel.setText("");
            createdOnLabel.setText("");
            dateUpdatedLabel.setText("");
        }

        section.layout(true);
    }

    protected EnvironmentDescription describeEnvironment(String environmentName) {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId()).getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        List<EnvironmentDescription> environments = client.describeEnvironments(
            new DescribeEnvironmentsRequest()
                .withEnvironmentNames(environment.getEnvironmentName())).getEnvironments();

        if (environments.isEmpty()) return null;
        else return environments.get(0);
    }

    protected Label createLabels(Composite composite, String labelText, String value) {
        createLabel(toolkit, composite, labelText);
        Label label = createLabel(toolkit, composite, value);
        label.setLayoutData(new GridData(GridData.FILL_BOTH));
        return label;
    }

    protected Label createLabel(FormToolkit toolkit, Composite parent, String text) {
        Label label = toolkit.createLabel(parent, text);
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        return label;
    }

    protected Text createReadOnlyText(FormToolkit toolkit, Composite parent, String text) {
        Text t = toolkit.createText(parent, text, SWT.READ_ONLY | SWT.SELECTED | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 400;
        t.setLayoutData(gridData);
        return t;
    }
}
