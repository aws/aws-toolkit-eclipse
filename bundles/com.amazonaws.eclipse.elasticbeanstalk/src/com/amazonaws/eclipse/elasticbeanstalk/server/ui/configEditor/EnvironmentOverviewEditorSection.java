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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.ui.overview.HyperlinkHandler;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.util.ElasticBeanstalkClientExtensions;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentHealthRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentHealthResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.InvalidRequestException;
import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;

/**
 * Environment overview editor section
 */
public class EnvironmentOverviewEditorSection extends ServerEditorSection {

    /** The section widget we're managing */
    private Section section;

    private Environment environment;

    private FormToolkit toolkit;

    private StyledText environmentNameLabel;
    private StyledText environmentDescriptionLabel;
    private StyledText regionNameLabel;
    private StyledText applicationNameLabel;
    private StyledText applicationVersionLabel;
    private StyledText applicationTierLabel;
    private StyledText statusLabel;
    private StyledText healthLabel;
    private StyledText healthCausesLabel;
    private StyledText solutionStackLabel;
    private StyledText createdOnLabel;
    private StyledText dateUpdatedLabel;
    private Hyperlink environmentUrlHyperlink;
    private Hyperlink owningAccountHyperlink;

    /*
     * (non-Javadoc)
     * @see org.eclipse.wst.server.ui.editor.ServerEditorSection#init(org.eclipse.ui.IEditorSite,
     * org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        environment = (Environment) server.loadAdapter(Environment.class, null);
    }

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        getManagedForm().getForm().getToolBarManager().add(new AwsAction(
                AwsToolkitMetricType.EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR,
                "Refresh", SWT.None) {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh");
            }

            @Override
            protected void doRun() {
                refreshEnvironmentDetails();
                actionFinished();
            }
        });
        getManagedForm().getForm().getToolBarManager().update(true);

        toolkit = getFormToolkit(parent.getDisplay());

        section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("AWS Elastic Beanstalk Environment");
        section.setDescription("Basic information about your environment.");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        GridData horizontalAndVerticalFillGridData = new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_FILL);

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

        environmentNameLabel = createRow(composite, "Environment Name: ", environment.getEnvironmentName());
        environmentDescriptionLabel = createRow(composite, "Environment Description: ",
                environment.getEnvironmentDescription());

        if (ConfigurationOptionConstants.WEB_SERVER.equals(environment.getEnvironmentTier())) {
            createLabel(toolkit, composite, "Environment URL:");
            environmentUrlHyperlink = toolkit.createHyperlink(composite, "", SWT.NONE);
            environmentUrlHyperlink.addHyperlinkListener(new HyperlinkHandler());
        }

        regionNameLabel = createRow(composite, "AWS Region: ", "");

        applicationNameLabel = createRow(composite, "Application Name: ", environment.getApplicationName());
        applicationVersionLabel = createRow(composite, "Application Version: ", environment.getEnvironmentDescription());

        applicationTierLabel = createRow(composite, "Application Tier: ", environment.getEnvironmentTier());

        statusLabel = createRow(composite, "Status:", "");
        healthLabel = createRow(composite, "Health: ", "");
        healthCausesLabel = createRow(composite, "Causes:", "");
        solutionStackLabel = createRow(composite, "Solution Stack: ", "");
        createdOnLabel = createRow(composite, "Created On: ", "");
        dateUpdatedLabel = createRow(composite, "Last Updated: ", "");

        String accountId = environment.getAccountId();
        String accountName = AwsToolkitCore.getDefault().getAccountManager().getAllAccountNames().get(accountId);
        if (accountName != null) {
            createLabel(toolkit, composite, "AWS Account: ");
            String href = "preference:" + AwsAccountPreferencePage.ID;
            String text = accountName;
            owningAccountHyperlink = toolkit.createHyperlink(composite, text, SWT.None);
            owningAccountHyperlink.setHref(href);
            owningAccountHyperlink.addHyperlinkListener(new HyperlinkHandler());
        }

        refreshEnvironmentDetails();
    }

    private void refreshEnvironmentDetails() {
        new LoadHealthStatusJob().schedule();

        String regionEndpoint = environment.getRegionEndpoint();
        try {
            Region region = RegionUtils.getRegionByEndpoint(regionEndpoint);
            regionNameLabel.setText(region.getName());
        } catch (Exception e) {
            regionNameLabel.setText(regionEndpoint);
        }

        EnvironmentDescription environmentDescription = environment.getCachedEnvironmentDescription();
        if (environmentDescription != null) {
            environmentNameLabel.setText(environmentDescription.getEnvironmentName());
            if (environmentDescription.getDescription() != null) {
                environmentDescriptionLabel.setText(environmentDescription.getDescription());
            }

            if (environmentUrlHyperlink != null) {
                String environmentUrl = "http://" + environmentDescription.getCNAME();
                environmentUrlHyperlink.setText(environmentUrl);
                environmentUrlHyperlink.setHref(environmentUrl);
            }

            applicationNameLabel.setText(environmentDescription.getApplicationName());
            applicationVersionLabel.setText(environmentDescription.getVersionLabel());
            applicationTierLabel.setText(environmentDescription.getTier().getName());
            statusLabel.setText(environmentDescription.getStatus());

            solutionStackLabel.setText(environmentDescription.getSolutionStackName());
            createdOnLabel.setText(environmentDescription.getDateCreated().toString());
            dateUpdatedLabel.setText(environmentDescription.getDateUpdated().toString());
        } else {
            environmentNameLabel.setText(environment.getEnvironmentName());
            if (environment.getEnvironmentDescription() != null) {
                environmentDescriptionLabel.setText(environment.getEnvironmentDescription());
            }

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
        return new ElasticBeanstalkClientExtensions(environment).getEnvironmentDescription(environmentName);
    }

    protected StyledText createRow(Composite composite, String labelText, String value) {
        createLabel(toolkit, composite, labelText);
        StyledText text = createReadOnlyText(toolkit, composite, value);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        return text;
    }

    protected Label createLabel(FormToolkit toolkit, Composite parent, String text) {
        Label label = toolkit.createLabel(parent, text);
        label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        return label;
    }

    protected StyledText createReadOnlyText(FormToolkit toolkit, Composite parent, String text) {
        StyledText t = new StyledText(parent, SWT.READ_ONLY | SWT.NO_FOCUS | SWT.WRAP);
        t.setText(text);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.widthHint = 400;
        t.setLayoutData(gridData);
        return t;
    }

    /**
     * Italize all text in the StyledText. Should be called after text is set.
     *
     * @param styledText
     */
    private static void italizeStyledText(StyledText styledText) {
        StyleRange italicStyle = new StyleRange();
        italicStyle.fontStyle = SWT.ITALIC;
        italicStyle.length = styledText.getText().length();
        styledText.setStyleRange(italicStyle);
    }

    /**
     * Background job to load the environment health data and dump it into the UI
     */
    private final class LoadHealthStatusJob extends Job {

        private final BeanstalkHealthColorConverter colorConverter = new BeanstalkHealthColorConverter(
                new LocalResourceManager(JFaceResources.getResources(), section));
        private final AWSElasticBeanstalk beanstalk = environment.getClient();

        public LoadHealthStatusJob() {
            super("Loading Health Status");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                updateEnhancedHealthControls(getEnvironmentHealth());
            } catch (InvalidRequestException e) {
                // Enhanced health isn't supported for this environment so just show the basic data
                // from the EnvironmentDescription
                updateBasicHealthControls();
            }
            return Status.OK_STATUS;
        }

        private DescribeEnvironmentHealthResult getEnvironmentHealth() {
            return beanstalk.describeEnvironmentHealth(new DescribeEnvironmentHealthRequest().withEnvironmentName(
                    environment.getEnvironmentName()).withAttributeNames("All"));
        }

        /**
         * Update the Health using data from the {@link EnvironmentDescription} since Enhanced
         * health reporting is not enabled for this environment
         */
        private void updateBasicHealthControls() {
            final EnvironmentDescription envDesc = environment.getCachedEnvironmentDescription();
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    healthLabel.setForeground(colorConverter.toColor(envDesc.getHealth()));
                    healthLabel.setText(envDesc.getHealth());
                    healthCausesLabel
                            .setText("Causes information is only available for environments with Enhanced Health Reporting enabled");
                    italizeStyledText(healthCausesLabel);
                }
            });
        }

        /**
         * Update the Health and Causes using data from the {@link DescribeEnvironmentHealthResult}
         * for the Enhanced Health capable environment
         */
        private void updateEnhancedHealthControls(final DescribeEnvironmentHealthResult result) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    healthLabel.setText(result.getHealthStatus());
                    healthLabel.setForeground(colorConverter.toColor(result.getColor()));
                    healthCausesLabel.setText(getCausesDisplayText(result.getCauses()));
                }
            });
        }

        /**
         * Convert the list of causes to something that can be displayed in the Text control
         *
         * @param causes
         *            List of causes for non-OK health statuses
         * @return Display string
         */
        private String getCausesDisplayText(List<String> causes) {
            if (!CollectionUtils.isNullOrEmpty(causes)) {
                return StringUtils.join(",", causes.toArray(new String[0]));
            } else {
                return "";
            }
        }

    }

}
