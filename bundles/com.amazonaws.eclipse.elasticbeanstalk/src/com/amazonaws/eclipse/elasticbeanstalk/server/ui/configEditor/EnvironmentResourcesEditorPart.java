/*
 * Copyright 2011-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;
import org.eclipse.wst.server.ui.internal.ImageResource;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.ui.views.instances.InstanceSelectionTable;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.sqs.AddMessageAction;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentResourcesRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentResourceDescription;
import com.amazonaws.services.elasticbeanstalk.model.Instance;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;

public class EnvironmentResourcesEditorPart extends ServerEditorPart {

    protected ManagedForm managedForm;
    private EnvironmentInstancesEditorSection instancesEditorSection;
    private EnvironmentAutoScalingEditorSection autoScalingEditorSection;
    private EnvironmentElasticLoadBalancingEditorSection elasticLoadBalancingEditorSection;
    private EnvironmentQueueEditorSection queueEditorSection;

    @Override
    public void createPartControl(Composite parent) {
        managedForm = new ManagedForm(parent);
        setManagedForm(managedForm);
        ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        toolkit.decorateFormHeading(form.getForm());
        form.setText("Environment Resources");
        form.setImage(ImageResource.getImage(ImageResource.IMG_SERVER));
        FillLayout fillLayout = new FillLayout();
        form.getBody().setLayout(fillLayout);
        fillLayout.marginHeight = 10;
        fillLayout.marginWidth = 10;


        Composite composite = toolkit.createComposite(form.getBody());
        composite.setLayout(new GridLayout(2, true));

        if (!getEnvironment().getEnvironmentType().equals(ConfigurationOptionConstants.SINGLE_INSTANCE_ENV)) {

            autoScalingEditorSection = new EnvironmentAutoScalingEditorSection();
            autoScalingEditorSection.setServerEditorPart(this);
            autoScalingEditorSection.init(this.getEditorSite(), this.getEditorInput());
            autoScalingEditorSection.createSection(composite);

            if (ConfigurationOptionConstants.WEB_SERVER.equals(getEnvironment().getEnvironmentTier())) {
                elasticLoadBalancingEditorSection = new EnvironmentElasticLoadBalancingEditorSection();
                elasticLoadBalancingEditorSection.setServerEditorPart(this);
                elasticLoadBalancingEditorSection.init(this.getEditorSite(), this.getEditorInput());
                elasticLoadBalancingEditorSection.createSection(composite);
            } else {
                queueEditorSection = new EnvironmentQueueEditorSection();
                queueEditorSection.setServerEditorPart(this);
                queueEditorSection.init(this.getEditorSite(), this.getEditorInput());
                queueEditorSection.createSection(composite);
            }
        }

        instancesEditorSection = new EnvironmentInstancesEditorSection();
        instancesEditorSection.setServerEditorPart(this);
        instancesEditorSection.init(this.getEditorSite(), this.getEditorInput());
        instancesEditorSection.createSection(composite);

        form.getToolBarManager().add(new RefreshAction(AwsToolkitMetricType.EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR));
        form.getToolBarManager().update(true);

        new RefreshThread().start();
    }

    private class RefreshAction extends AwsAction {
        public RefreshAction(AwsToolkitMetricType metricType) {
            super(metricType);
            setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
            setText("Refresh");
            setToolTipText("Refresh");
        }

        @Override
        protected void doRun() {
            new RefreshThread().start();
            actionFinished();
        }
    }


    private class RefreshThread extends Thread {
        @Override
        public void run() {
            if (getEnvironment().doesEnvironmentExistInBeanstalk() == false) {
                return;
            }

            DescribeEnvironmentResourcesRequest request = new DescribeEnvironmentResourcesRequest()
                .withEnvironmentName(getEnvironment().getEnvironmentName());
            EnvironmentResourceDescription resources = getClient().describeEnvironmentResources(request).getEnvironmentResources();

            instancesEditorSection.update(resources);

            if (autoScalingEditorSection != null) {
                autoScalingEditorSection.update(resources);
            }

            if (elasticLoadBalancingEditorSection != null) {
                elasticLoadBalancingEditorSection.update(resources);
            }

            if (queueEditorSection != null) {
                queueEditorSection.update(resources);
            }
        }
    }

    private static abstract class AbstractEnvironmentResourcesEditorSection extends ServerEditorSection {
        protected Section section;
        protected FormToolkit toolkit;

        protected Composite createSection(Composite parent, String label, String description) {
            toolkit = getFormToolkit(Display.getDefault());

            section = toolkit.createSection(parent,
                Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR |
                Section.DESCRIPTION | Section.FOCUS_TITLE);
            section.setText(label);
            section.setDescription(description);

            Composite composite = toolkit.createComposite(section);
            FillLayout layout = new FillLayout();
            layout.marginHeight = 10;
            layout.marginWidth = 10;
            layout.type = SWT.VERTICAL;
            composite.setLayout(layout);
            toolkit.paintBordersFor(composite);
            section.setClient(composite);
            section.setLayout(layout);

            return composite;
        }

        public abstract void update(EnvironmentResourceDescription resources);
    }

    private class EnvironmentQueueEditorSection extends AbstractEnvironmentResourcesEditorSection {

        private Label nameLabel;
        private Link urlLink;
        private Label createdLabel;
        private Label numberOfMessagesLabel;
        private Label numberOfMessagesInFlightLabel;

        @Override
        public void createSection(Composite parent) {
            super.createSection(parent);

            Composite composite = createSection(
                parent,
                "Amazon SQS Queue",
                "Your Amazon SQS Queue queues work items for delivery to your "
                + "worker tier environment."
            );
            section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            composite.setLayout(new GridLayout(2, false));

            GridDataFactory gdf = GridDataFactory
                .swtDefaults()
                .align(SWT.FILL, SWT.TOP)
                .grab(true, false);

            new Label(composite, SWT.NONE).setText("Name:");
            nameLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(nameLabel);

            new Label(composite, SWT.NONE).setText("URL:");
            urlLink = new Link(composite, SWT.NONE);
            urlLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    String queueUrl = event.text;

                    AmazonSQS sqs = AwsToolkitCore
                        .getClientFactory(getEnvironment().getAccountId())
                        .getSQSClientByEndpoint(getEndpointFromUrl(queueUrl));

                    new AddMessageAction(sqs, event.text, null).run();
                }
            });
            gdf.applyTo(urlLink);

            new Label(composite, SWT.NONE).setText("Created:");
            createdLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(createdLabel);

            new Label(composite, SWT.NONE).setText("Messages Available:");
            numberOfMessagesLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(numberOfMessagesLabel);

            new Label(composite, SWT.NONE).setText("Messages in Flight:");
            numberOfMessagesInFlightLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(numberOfMessagesInFlightLabel);
        }

        @Override
        public void update(final EnvironmentResourceDescription resources) {
            if (resources.getQueues().isEmpty()) {
                return;
            }

            final String queueName = resources.getQueues().get(0).getName();
            final String queueUrl = resources.getQueues().get(0).getURL();

            AmazonSQS sqs = AwsToolkitCore
                .getClientFactory(getEnvironment().getAccountId())
                .getSQSClientByEndpoint(getEndpointFromUrl(queueUrl));

            final Map<String, String> attributes =
                sqs.getQueueAttributes(new GetQueueAttributesRequest()
                    .withQueueUrl(queueUrl)
                    .withAttributeNames(
                        QueueAttributeName.ApproximateNumberOfMessages,
                        QueueAttributeName.ApproximateNumberOfMessagesNotVisible,
                        QueueAttributeName.CreatedTimestamp,
                        QueueAttributeName.DelaySeconds))
                .getAttributes();

            if (attributes.isEmpty()) {
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    nameLabel.setText(queueName);
                    urlLink.setText("<a>" + queueUrl + "</a>");

                    String createdTimestamp = attributes.get("CreatedTimestamp");
                    if (createdTimestamp != null) {
                        String text;
                        try {
                            long timestamp = Long.parseLong(createdTimestamp);
                            text = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")
                                .format(new Date(timestamp * 1000L));
                        } catch (NumberFormatException exception) {
                            text = "";
                        }
                        createdLabel.setText(text);
                    }

                    String numberOfMessages = attributes.get("ApproximateNumberOfMessages");
                    numberOfMessagesLabel.setText(numberOfMessages == null ? "" : numberOfMessages);

                    String numberOfMessagesInFlight = attributes.get("ApproximateNumberOfMessagesNotVisible");
                    numberOfMessagesInFlightLabel.setText(numberOfMessagesInFlight == null ? "" : numberOfMessagesInFlight);
                }
            });
        }

        private String getEndpointFromUrl(final String url) {
            try {

                URI parsed = new URI(url);
                return parsed.getScheme() + "://" + parsed.getAuthority();

            } catch (URISyntaxException exception) {
                throw new RuntimeException("Could not parse URL: " + url,
                                           exception);
            }
        }
    }

    private class EnvironmentAutoScalingEditorSection extends AbstractEnvironmentResourcesEditorSection {

        private Label maxSizeLabel;
        private Label minSizeLabel;
        private Label launchConfigurationLabel;
        private Label desiredCapacityLabel;
        private Label healthCheckTypeLabel;
        private Label createdLabel;
        private Label availabilityZonesLabel;
        private Label nameLabel;

        @Override
        public void createSection(Composite parent) {
            super.createSection(parent);

            Composite composite = createSection(parent, "Amazon Auto Scaling Group",
                    "Your Amazon Auto Scaling group controls how your fleet dynamically resizes.");
            section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

            composite.setLayout(new GridLayout(2, false));
            GridDataFactory gdf = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false);

            new Label(composite, SWT.NONE).setText("Name:");
            nameLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(nameLabel);

            new Label(composite, SWT.NONE).setText("Availability Zones:");
            availabilityZonesLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(availabilityZonesLabel);

            new Label(composite, SWT.NONE).setText("Created:");
            createdLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(createdLabel);

            new Label(composite, SWT.NONE).setText("Health Check Type:");
            healthCheckTypeLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(healthCheckTypeLabel);

            new Label(composite, SWT.NONE).setText("Launch Configuration:");
            launchConfigurationLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(launchConfigurationLabel);

            new Label(composite, SWT.NONE).setText("Desired Capacity:");
            desiredCapacityLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(desiredCapacityLabel);

            new Label(composite, SWT.NONE).setText("Min Size:");
            minSizeLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(minSizeLabel);

            new Label(composite, SWT.NONE).setText("Max Size:");
            maxSizeLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(maxSizeLabel);
        }

        @Override
        public void update(EnvironmentResourceDescription resources) {
            Region region = RegionUtils.getRegionByEndpoint(getEnvironment().getRegionEndpoint());
            String endpoint = region.getServiceEndpoints().get(ServiceAbbreviations.AUTOSCALING);
            AmazonAutoScaling as = AwsToolkitCore.getClientFactory(getEnvironment().getAccountId()).getAutoScalingClientByEndpoint(endpoint);

            if ( !resources.getAutoScalingGroups().isEmpty() ) {
                DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(resources.getAutoScalingGroups().get(0).getName());
                List<AutoScalingGroup> autoScalingGroups = as.describeAutoScalingGroups(request).getAutoScalingGroups();

                if ( autoScalingGroups.size() > 0 ) {
                    final AutoScalingGroup group = autoScalingGroups.get(0);

                    Display.getDefault().asyncExec(new Runnable() {

                        @Override
                        public void run() {
                            nameLabel.setText(group.getAutoScalingGroupName());
                            availabilityZonesLabel.setText(group.getAvailabilityZones().toString());
                            createdLabel.setText("" + group.getCreatedTime());
                            healthCheckTypeLabel.setText(group.getHealthCheckType());
                            desiredCapacityLabel.setText("" + group.getDesiredCapacity());
                            launchConfigurationLabel.setText(group.getLaunchConfigurationName());
                            minSizeLabel.setText("" + group.getMinSize());
                            maxSizeLabel.setText("" + group.getMaxSize());
                        }
                    });
                }
            }
        }
    }

    private class EnvironmentElasticLoadBalancingEditorSection extends AbstractEnvironmentResourcesEditorSection {
        private Label nameLabel;
        private Label dnsLabel;
        private Label createdLabel;
        private Label availabilityZonesLabel;

        @Override
        public void createSection(Composite parent) {
            super.createSection(parent);

            Composite composite = createSection(parent, "Amazon Elastic Load Balancer",
                    "Your Elastic Load Balancer provides a single access point for the fleet of EC2 instances running your application.");
            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
            gridData.widthHint = 300;
            section.setLayoutData(gridData);

            composite.setLayout(new GridLayout(2, false));
            GridDataFactory gdf = GridDataFactory.swtDefaults().align(SWT.FILL, SWT.TOP).grab(true, false);

            new Label(composite, SWT.NONE).setText("Name:");
            nameLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(nameLabel);

            new Label(composite, SWT.NONE).setText("DNS:");
            dnsLabel = new Label(composite, SWT.WRAP);
            gdf.applyTo(dnsLabel);

            new Label(composite, SWT.NONE).setText("Created:");
            createdLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(createdLabel);

            new Label(composite, SWT.NONE).setText("Availability Zones:");
            availabilityZonesLabel = new Label(composite, SWT.NONE);
            gdf.applyTo(availabilityZonesLabel);
        }

        @Override
        public void update(EnvironmentResourceDescription resources) {
            Region region = RegionUtils.getRegionByEndpoint(getEnvironment().getRegionEndpoint());
            String endpoint = region.getServiceEndpoints().get(ServiceAbbreviations.ELB);
            AmazonElasticLoadBalancing elb = AwsToolkitCore.getClientFactory(getEnvironment().getAccountId()).getElasticLoadBalancingClientByEndpoint(endpoint);

            if (resources.getLoadBalancers() == null || resources.getLoadBalancers().size() == 0) {
                return;
            }

            String loadBalancerName = resources.getLoadBalancers().get(0).getName();
            DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest().withLoadBalancerNames(loadBalancerName);
            List<LoadBalancerDescription> loadBalancers = elb.describeLoadBalancers(request).getLoadBalancerDescriptions();

            if (loadBalancers.size() == 0) {
                return;
            }

            final LoadBalancerDescription lb = loadBalancers.get(0);

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    nameLabel.setText(lb.getLoadBalancerName());
                    dnsLabel.setText(lb.getDNSName());
                    createdLabel.setText(lb.getCreatedTime().toString());
                    availabilityZonesLabel.setText(lb.getAvailabilityZones().toString());

                    Composite parent = nameLabel.getParent();
                    parent.setSize(parent.computeSize(parent.getSize().x, SWT.DEFAULT, true));
                    parent.getParent().layout(true, true);
                }
            });
        }
    }

    private class EnvironmentInstancesEditorSection extends AbstractEnvironmentResourcesEditorSection {

        private InstanceSelectionTable instanceSelectionTable;

        @Override
        public void createSection(Composite parent) {
            super.createSection(parent);

            Composite composite = createSection(parent, "Amazon EC2 Instances",
                    "These instances make up the fleet in your environment");

            GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
            section.setLayoutData(gridData);

            instanceSelectionTable = new InstanceSelectionTable(composite);
            instanceSelectionTable.setAccountIdOverride(getEnvironment().getAccountId());

            Region region = RegionUtils.getRegionByEndpoint(getEnvironment().getRegionEndpoint());
            String ec2Endpoint = region.getServiceEndpoints().get(ServiceAbbreviations.EC2);
            instanceSelectionTable.setEc2RegionOverride(region);
        }


        @Override
        public void update(EnvironmentResourceDescription resources) {
            List<String> instanceIds = new ArrayList<>();
            for (Instance instance : resources.getInstances()) {
                instanceIds.add(instance.getId());
            }

            instanceSelectionTable.setInstancesToList(instanceIds);
        }
    }

    @Override
    public void setFocus() {}

    private AWSElasticBeanstalk getClient() {
        Environment environment = getEnvironment();

        return AwsToolkitCore.getClientFactory(environment.getAccountId())
            .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
    }

    private Environment getEnvironment() {
        return (Environment)server.loadAdapter(Environment.class, null);
    }

}
