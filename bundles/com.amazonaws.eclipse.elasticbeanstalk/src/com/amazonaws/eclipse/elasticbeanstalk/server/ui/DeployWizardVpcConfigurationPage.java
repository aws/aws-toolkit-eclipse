/*
 * Copyright 2010-2016 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.LoadResourcesCallback;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.LoadVpcsJob;
import com.amazonaws.eclipse.elasticbeanstalk.util.BeanstalkConstants;
import com.amazonaws.eclipse.elasticbeanstalk.util.OnUiThreadProxyFactory;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.util.StringUtils;

public class DeployWizardVpcConfigurationPage extends AbstractDeployWizardPage {

    private final VpcWidgetBuilder vpcWidgetBuilder = new VpcWidgetBuilder();

    private static final String VPC_CONFIGURATION_DOC_URL = "https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/vpc.html";

    private Composite wizardPageRoot;
    private final LoadResourcesCallback<Vpc> loadVpcCallback;

    protected DeployWizardVpcConfigurationPage(
            DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
        setComplete(false);
        this.loadVpcCallback = OnUiThreadProxyFactory.getProxy(
                LoadResourcesCallback.class, new LoadVpcsCallback());
    }

    @Override
    public String getPageTitle() {
        return "VPC Configuration";
    }

    @Override
    public String getPageDescription() {
        return "Configure VPC and subnets for your EC2 instances, and specify VPC security group.";
    }

    // When entering this page, according to the setup from the previous page,
    // such as region and whether using non-default VPC,
    // the VPC list will be refreshed, and the availability for the UI
    // components will be refreshed as well.
    @Override
    public void enter() {
        super.enter();
        if (wizardDataModel.isUseNonDefaultVpc()) {
            new LoadVpcsJob(wizardDataModel.getRegion(), loadVpcCallback).schedule();
        } else {
            wizardDataModel.setVpcId(null);
        }
        // Set complete true since all the resources are loaded and selected default values.
        setComplete(true);
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        wizardHandle = handle;

        setDefaultsInDataModel();

        handle.setImageDescriptor(AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        this.wizardPageRoot = new Composite(parent, SWT.NONE);
        wizardPageRoot.setLayout(new GridLayout(1, false));

        initializeValidators();
        vpcWidgetBuilder.buildVpcUiSection(wizardPageRoot);

        return wizardPageRoot;
    }

    private class LoadVpcsCallback implements LoadResourcesCallback<Vpc> {

        @Override
        public void onSuccess(List<Vpc> vpcs) {
            createVpcConfigurationSection(vpcs);
        }

        @Override
        public void onFailure() {
            onInsufficientPermissions();
        }

        @Override
        public void onInsufficientPermissions() {
            // currently do nothing, and let the caller handle the failure.
        }

        private void createVpcConfigurationSection(List<Vpc> vpcs) {
            vpcWidgetBuilder.refreshVpcConfigurationSection(vpcs);
            vpcWidgetBuilder.refreshVpcSectionAvailability();
        }
    }

    /**
     * Set the default values for the roles and vpc in the data model to be
     * reflected in the UI when the model is bound to a control
     */
    private void setDefaultsInDataModel() {
        wizardDataModel.setAssociatePublicIpAddress(false);
        if (StringUtils.isNullOrEmpty(wizardDataModel.getElbScheme())) {
            wizardDataModel
                    .setElbScheme(BeanstalkConstants.ELB_SCHEME_EXTERNAL);
        }
    }

    private enum CheckboxType {
        ELB,
        EC2
    }

    private class VpcWidgetBuilder {
        private final String[] SUBNET_TABLE_TITLES = { "Availability Zone",
                "Subnet ID", "Cidr Block", "ELB", "EC2" };
        private final String[] ELB_SCHEMES = {
                BeanstalkConstants.ELB_SCHEME_EXTERNAL,
                BeanstalkConstants.ELB_SCHEME_INTERNAL };
        private Combo vpcCombo;
        private Table subnetsTable;
        private Button apiaButton;// Associate Public Ip Address
        private Combo securityGroupCombo;
        private Combo elbSchemeCombo;

        private List<Button> checkboxButtons = new ArrayList<>();

        // build UI section only, not populating data
        public void buildVpcUiSection(Composite composite) {
            Composite group = newGroup(composite, "VPC Configuration:");
            group.setLayout(new GridLayout(3, false));

            createVpcSelectionSection(group);
            vpcCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onVpcSelectionChanged();
                }
            });

            apiaButton = newCheckbox(group, "Associate Public IP Address", 1);
            bindAssociatePublicIpAddressButton(apiaButton);

            createSubnetSelectionSection(group);
            createSecurityGroupSelection(group);
            securityGroupCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    onSecurityGroupSelectionChanged();
                }
            });

            createElbSchemesSelectionSection(group);
            bindCombo(elbSchemeCombo, DeployWizardDataModel.ELB_SCHEME);
        }

        private void onCheckboxSelected(String subnetId, CheckboxType type, boolean selected) {
            Set<String> subnets;
            switch(type) {
            case EC2:
                subnets = wizardDataModel.getEc2Subnets();
                break;
            case ELB:
                subnets = wizardDataModel.getElbSubnets();
                break;
            default:
                subnets = new HashSet<>();
            }

            if (selected) {
                subnets.add(subnetId);
            } else {
                subnets.remove(subnetId);
            }
        }

        public void refreshVpcSectionAvailability() {
            vpcCombo.setEnabled(wizardDataModel.isUseNonDefaultVpc());
            subnetsTable.setEnabled(wizardDataModel.isUseNonDefaultVpc());
            apiaButton.setEnabled(wizardDataModel.isUseNonDefaultVpc());
            elbSchemeCombo.setEnabled(wizardDataModel.isUseNonDefaultVpc()
                    && ConfigurationOptionConstants.LOAD_BALANCED_ENV
                            .equals(wizardDataModel.getEnvironmentType()));
        }

        private void createSubnetSelectionSection(Composite composite) {
            createSubnetsSelectionLabel(composite);
            createSubnetsTable(composite);
        }

        private void createElbSchemesSelectionSection(Composite parent) {
            newLabel(parent, "ELB visibility: ", 1)
                .setToolTipText("This combo box is only enabled when you are selecting Load Balanced Web Server Environment type.");
            elbSchemeCombo = newCombo(parent, 1);
            elbSchemeCombo.setItems(ELB_SCHEMES);
            newLabel(parent, "Select Internal when load balancing a back-end\nservice that should not be publicly available.", 1,
                    SWT.LEFT, SWT.BOTTOM);
        }

        private void createSecurityGroupSelection(Composite parent) {
            newLabel(parent, "VPC security group:");
            securityGroupCombo = newCombo(parent, 2);
        }

        public void refreshVpcConfigurationSection(List<Vpc> vpcs) {
            List<String> vpcsString = transformVpcList(vpcs);

            vpcCombo.setItems(new String[]{});
            for (int i = 0; i < vpcsString.size(); ++i) {
                vpcCombo.add(vpcsString.get(i));
                vpcCombo.setData(vpcsString.get(i), vpcs.get(i));
            }
            if (!vpcs.isEmpty()) {
                vpcCombo.select(0);
            }

            onVpcSelectionChanged();
        }

        private void createVpcSelectionSection(Composite composite) {
            createVpcSelectionLabel(composite);
            newLabel(composite, "VPC:");
            vpcCombo = newCombo(composite);
        }

        private void createVpcSelectionLabel(Composite composite) {
            adjustLinkLayout(
                    newLink(composite,
                            "Select the VPC to use when creating your environment. "
                                    + "<a href=\"" + VPC_CONFIGURATION_DOC_URL
                                    + "\">Learn more</a>."), 3);
        }

        private void bindAssociatePublicIpAddressButton(Button button) {
            IObservableValue apiaObservable = SWTObservables
                    .observeSelection(button);
            IObservableValue observable = PojoObservables.observeValue(
                    wizardDataModel,
                    DeployWizardDataModel.ASSOCIATE_PUBLIC_IP_ADDRESS);
            getBindingContext().bindValue(apiaObservable, observable);
        }

        private void createSubnetsSelectionLabel(Composite composite) {
            newLabel(
                    composite,
                    "Select different subnets for ELB and EC2 instances in your Availability Zone.",
                    3);
        }

        private void createSubnetsTable(Composite composite) {
            subnetsTable = new Table(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
            subnetsTable.setLinesVisible(true);
            subnetsTable.setHeaderVisible(true);

            TableLayout layout = new TableLayout();
            for (int i = 0; i < SUBNET_TABLE_TITLES.length; ++i) {
                TableColumn column = new TableColumn(subnetsTable, SWT.NONE);
                column.setText(SUBNET_TABLE_TITLES[i]);
                layout.addColumnData(new ColumnWeightData(100));
            }

            GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
            data.heightHint = 100;
            data.widthHint = 200;
            data.horizontalSpan = 5;
            subnetsTable.setLayoutData(data);
            subnetsTable.setLayout(layout);
        }

        private void onVpcSelectionChanged() {

            Vpc selectedVpc = (Vpc)vpcCombo.getData(vpcCombo.getItem(vpcCombo.getSelectionIndex()));
            // Reset Data Model
            wizardDataModel.setVpcId(selectedVpc.getVpcId());
            wizardDataModel.getEc2Subnets().clear();
            wizardDataModel.getElbSubnets().clear();

            // Redraw Subnet table UI
            subnetsTable.removeAll();
            for (Button button : checkboxButtons) {
                if (button != null) button.dispose();
            }
            AmazonEC2 ec2 = AwsToolkitCore.getClientFactory()
                    .getEC2ClientByEndpoint(
                            wizardDataModel.getRegion().getServiceEndpoint(
                                    ServiceAbbreviations.EC2));
            List<Subnet> subnets = ec2.describeSubnets(
                    new DescribeSubnetsRequest().withFilters(new Filter()
                            .withName("vpc-id").withValues(
                                    wizardDataModel.getVpcId()))).getSubnets();
            for (int i = 0; i < subnets.size(); ++i) {
                TableItem item = new TableItem(subnetsTable, SWT.CENTER);
                final Subnet subnet = subnets.get(i);

                item.setText(0, subnet.getAvailabilityZone());
                item.setText(1, subnet.getSubnetId());
                item.setText(2, subnet.getCidrBlock());

                checkboxButtons.add(drawCheckboxOnSubnetTable(item, subnet.getSubnetId(), 3, CheckboxType.ELB,
                        wizardDataModel.isUseNonDefaultVpc() &&
                        ConfigurationOptionConstants.LOAD_BALANCED_ENV.equals(wizardDataModel.getEnvironmentType())));
                checkboxButtons.add(drawCheckboxOnSubnetTable(item, subnet.getSubnetId(), 4, CheckboxType.EC2, true));
            }

            // Redraw security group UI
            List<SecurityGroup> securityGroups = ec2.describeSecurityGroups(new DescribeSecurityGroupsRequest()
                    .withFilters(new Filter()
                            .withName("vpc-id").withValues(wizardDataModel.getVpcId()))).getSecurityGroups();
            securityGroupCombo.removeAll();
            for (SecurityGroup securityGroup : securityGroups) {
                String securityGroupText = securityGroup.getGroupName() + " -- " + securityGroup.getGroupId();
                securityGroupCombo.add(securityGroupText);
                securityGroupCombo.setData(securityGroupText, securityGroup);
            }
            securityGroupCombo.select(0);
            onSecurityGroupSelectionChanged();
        }

        private void onSecurityGroupSelectionChanged() {
            SecurityGroup securityGroup = (SecurityGroup) securityGroupCombo.getData(
                    securityGroupCombo.getItem(securityGroupCombo.getSelectionIndex()));
            wizardDataModel.setSecurityGroup(securityGroup.getGroupId());
        }

        private Button drawCheckboxOnSubnetTable(TableItem item, final String subnetId, int columnIndex, final CheckboxType type, final boolean enabled) {
            TableEditor editor = new TableEditor(subnetsTable);
            Button checkbox = new Button(subnetsTable, SWT.CHECK);
            checkbox.setEnabled(enabled);
            checkbox.pack();
            editor.minimumWidth = checkbox.getSize().x;
            editor.horizontalAlignment = SWT.LEFT;
            editor.setEditor(checkbox, item, columnIndex);
            checkbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onCheckboxSelected(subnetId, type, ((Button)e.getSource()).getSelection());
                }
            });
            return checkbox;
        }

        private void bindCombo(Combo combo, String fieldName) {
            IObservableValue comboObservable = SWTObservables
                    .observeSelection(combo);
            IObservableValue pojoObservable = PojoObservables.observeValue(
                    wizardDataModel, fieldName);
            getBindingContext().bindValue(comboObservable, pojoObservable);
        }

        private List<String> transformVpcList(List<Vpc> vpcs) {
            List<String> stringVpcs = new ArrayList<>(vpcs.size());
            for (Vpc vpc : vpcs) {
                String vpcTextPrefix = getVpcName(vpc);
                if (!StringUtils.isNullOrEmpty(vpcTextPrefix)) {
                    vpcTextPrefix += " -- ";
                }
                stringVpcs.add(vpcTextPrefix + vpc.getVpcId());
            }
            return stringVpcs;
        }

        private String getVpcName(Vpc vpc) {
            if (vpc.getTags() != null && !vpc.getTags().isEmpty()) {
                for (Tag tag : vpc.getTags()) {
                    if ("Name".equals(tag.getKey())) {
                        return tag.getValue();
                    }
                }
            }
            return "";
        }

        /**
         * DataBindingContext is setup in {@link AbstractDeployWizardPage}
         *
         * @return The current data binding context
         */
        private DataBindingContext getBindingContext() {
            return DeployWizardVpcConfigurationPage.this.bindingContext;
        }
    }

}
