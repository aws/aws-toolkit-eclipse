/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.ui.launchwizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.InstanceType;
import com.amazonaws.eclipse.ec2.InstanceTypes;
import com.amazonaws.eclipse.ec2.ui.ChargeWarningComposite;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairSelectionTable;
import com.amazonaws.eclipse.ec2.ui.securitygroups.SecurityGroupSelectionComposite;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;

/**
 * Wizard Page for launching EC2 instances.
 */
public class LaunchWizardPage extends WizardPage {

    private static final String NO_INSTANCE_PROFILE = "None";
    private Combo availabilityZoneCombo;
    private Combo instanceTypeCombo;
    private Label instanceTypeArchitectureLabel;
    private Label instanceTypeVirtualCoresLabel;
    private Label instanceTypeDiskCapacityLabel;
    private Label instanceTypeMemoryLabel;
    private Combo instanceProfileCombo;

    private Text userDataText;
    private KeyPairComposite keyPairComposite;
    private SecurityGroupSelectionComposite securityGroupSelectionComposite;
    private Spinner numberOfHostsSpinner;

    /** The Image being launched */
    private Image image;

    /** Label displaying the name of the AMI being launched */
    private Label amiLabel;

    /**
     * Creates a new LaunchWizardPage, configured to launch the specified Image.
     *
     * @param image
     *            The Image being launched by this LaunchWizardPage.
     */
    protected LaunchWizardPage(Image image) {
        super("Launch", "Launch Amazon EC2 Instances", null);
        this.image = image;

        this.setDescription("Configure the options for launching your Amazon EC2 instances");
    }

    /**
     * Loads the controls (key pair selection table, availability zones,
     * image details) once this page is displayed.
     *
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            LaunchWizard launchWizard = (LaunchWizard)this.getWizard();
            image = launchWizard.getImageToLaunch();

            if (image != null) {
                amiLabel.setText(image.getImageId() + " (" + image.getArchitecture() + ")");
                populateValidInstanceTypes();
            }

            securityGroupSelectionComposite.getRefreshSecurityGroupsAction().run();
            keyPairComposite.getKeyPairSelectionTable().refreshKeyPairs();
            loadAvailabilityZones();

            try {
                loadInstanceProfiles();
            } catch ( Exception e ) {
                instanceProfileCombo.select(0);
                AwsToolkitCore.getDefault().logError("Couldn't load IAM Instance Profiles", e);
            }
        }

        super.setVisible(visible);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite originalParent) {
        Composite parent = new Composite(originalParent, SWT.NONE);

        GridLayout parentLayout = new GridLayout(2, false);
        parent.setLayout(parentLayout);

        newLabel(parent, "AMI:");
        amiLabel = newLabel(parent, "N/A");
        amiLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        newLabel(parent, "Number of Hosts:");

        numberOfHostsSpinner = new Spinner(parent, SWT.BORDER);
        numberOfHostsSpinner.setSelection(1);
        numberOfHostsSpinner.setMaximum(20);
        numberOfHostsSpinner.setMinimum(1);
        numberOfHostsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        newLabel(parent, "Instance Type:");

        instanceTypeCombo = new Combo(parent, SWT.READ_ONLY);
        instanceTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        instanceTypeCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateInstanceTypeInformation();
            }
        });

        populateValidInstanceTypes();

        new Label(parent, SWT.NONE);

        createInstanceTypeDetailsComposite(parent);


        // Create the availability zone combo; zones are loaded when the page
        // becomes visible
        newLabel(parent, "Availability Zone:");
        availabilityZoneCombo = new Combo(parent, SWT.READ_ONLY);
        availabilityZoneCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        SelectionListener selectionListener = new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateControls();
            }
        };

        newLabel(parent, "Key Pair:");
        keyPairComposite = new KeyPairComposite(parent);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 100;
        keyPairComposite.setLayoutData(gridData);
        keyPairComposite.getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateControls();
            }
        });

        newLabel(parent, "Security Group:");
        securityGroupSelectionComposite = new SecurityGroupSelectionComposite(parent);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 100;
        securityGroupSelectionComposite.setLayoutData(gridData);
        securityGroupSelectionComposite.addSelectionListener(selectionListener);

        newLabel(parent, "Instance Profile:");
        instanceProfileCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        instanceProfileCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        instanceProfileCombo.setItems(new String[] { NO_INSTANCE_PROFILE });
        instanceProfileCombo.select(0);

        newLabel(parent, "User Data:");
        userDataText = new Text(parent, SWT.MULTI | SWT.BORDER);
        userDataText.setLayoutData(new GridData(GridData.FILL_BOTH));
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 85;
        userDataText.setLayoutData(gridData);
        userDataText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateControls();
            }
        });

        ChargeWarningComposite chargeWarningComposite = new ChargeWarningComposite(parent, SWT.NONE);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        data.widthHint = 150;
        chargeWarningComposite.setLayoutData(data);

        updateControls();
        this.setControl(parent);
    }

    /**
     * Loads the EC2 availability zones for the current region and displays them
     * in the combo box.
     */
    private void loadAvailabilityZones() {
        try {
            availabilityZoneCombo.removeAll();
            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            DescribeAvailabilityZonesResult response = ec2.describeAvailabilityZones();
            for ( AvailabilityZone zone : response.getAvailabilityZones() ) {
                availabilityZoneCombo.add(zone.getZoneName());
                availabilityZoneCombo.select(0);
            }
        } catch ( Exception e ) {
            Status status = new Status(Status.WARNING, Ec2Plugin.PLUGIN_ID, "Unable to query EC2 availability zones: "
                    + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    private void loadInstanceProfiles() {
        ListInstanceProfilesResult listInstanceProfiles = AwsToolkitCore.getClientFactory()
                .getIAMClient().listInstanceProfiles();
        List<String> profileNames = new ArrayList<>();
        profileNames.add(NO_INSTANCE_PROFILE);
        for ( InstanceProfile profile : listInstanceProfiles.getInstanceProfiles() ) {
            profileNames.add(profile.getInstanceProfileName());
            instanceProfileCombo.setData(profile.getInstanceProfileName(), profile);
        }
        instanceProfileCombo.setItems(profileNames.toArray(new String[profileNames.size()]));
        instanceProfileCombo.select(0);
    }

    /**
     * Updates the EC2 instance type combo box so that only the instance types
     * that are appropriate for the selected Amazon Machine Image being launched
     * (i.e. the instnace type architecture matches the AMI architecture - 32bit
     * vs. 64bit).
     */
    private void populateValidInstanceTypes() {
        instanceTypeCombo.removeAll();

        for (InstanceType instanceType : InstanceTypes.getInstanceTypes()) {
            // Only display instance types that will work with the selected AMI
            if ( !instanceType.canLaunch(image) )
                continue;

            instanceTypeCombo.add(instanceType.name);
            instanceTypeCombo.setData(instanceType.name, instanceType);
            instanceTypeCombo.select(0);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.DialogPage#performHelp()
     */
    @Override
    public void performHelp() {
        PlatformUI.getWorkbench().getHelpSystem().displayHelp(
                "com.amazonaws.eclipse.ec2.launchWizardHelp");
    }

    private void updateControls() {
        setErrorMessage(null);

        if (keyPairComposite.getKeyPairSelectionTable().isValidKeyPairSelected() == false) {
            // If an invalid key is selected (as opposed to no key selected)
            // we want to display an error message.
            if (keyPairComposite.getKeyPairSelectionTable().getSelectedKeyPair() != null) {
                setErrorMessage(KeyPairSelectionTable.INVALID_KEYPAIR_MESSAGE);
            }

            setPageComplete(false);
            return;
        }

        if (securityGroupSelectionComposite.getSelectedSecurityGroup() == null) {
            setPageComplete(false);
            return;
        }

        setErrorMessage(null);
        setPageComplete(true);
    }

    private Label newLabel(Composite parent, String text) {
        Label label;
        label = new Label(parent, SWT.NONE);
        label.setText(text);
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gridData.verticalIndent = 4;
        label.setLayoutData(gridData);

        return label;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.DialogPage#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();

        keyPairComposite.dispose();
    }

    private void createInstanceTypeDetailsComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(4, true));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label label = new Label(composite, SWT.NONE);
        label.setText("Memory:");
        instanceTypeMemoryLabel = new Label(composite, SWT.NONE);
        instanceTypeMemoryLabel.setLayoutData(new GridData(GridData.FILL_BOTH));

        label = new Label(composite, SWT.NONE);
        label.setText("Virtual Cores:");
        instanceTypeVirtualCoresLabel = new Label(composite, SWT.NONE);
        instanceTypeVirtualCoresLabel.setLayoutData(new GridData(GridData.FILL_BOTH));

        label = new Label(composite, SWT.NONE);
        label.setText("Disk Capacity:");
        instanceTypeDiskCapacityLabel = new Label(composite, SWT.NONE);
        instanceTypeDiskCapacityLabel.setLayoutData(new GridData(GridData.FILL_BOTH));

        label = new Label(composite, SWT.NONE);
        label.setText("Architecture:");
        instanceTypeArchitectureLabel = new Label(composite, SWT.NONE);
        instanceTypeArchitectureLabel.setLayoutData(new GridData(GridData.FILL_BOTH));

        updateInstanceTypeInformation();
    }

    private void updateInstanceTypeInformation() {
        // Bail out early if this method has been called before
        // all the widgets have been initialized
        if ( instanceTypeCombo == null
                || instanceTypeArchitectureLabel == null
                || instanceTypeDiskCapacityLabel == null
                || instanceTypeMemoryLabel == null
                || instanceTypeVirtualCoresLabel == null) return;

        String s = instanceTypeCombo.getText();
        InstanceType instanceType = (InstanceType)instanceTypeCombo.getData(s);

        if (instanceType == null) {
            instanceTypeArchitectureLabel.setText("N/A");
            instanceTypeDiskCapacityLabel.setText("N/A");
            instanceTypeMemoryLabel.setText("N/A");
            instanceTypeVirtualCoresLabel.setText("N/A");
        } else {
            instanceTypeArchitectureLabel.setText(instanceType.architectureBits + " bits");
            instanceTypeDiskCapacityLabel.setText(instanceType.diskSpaceWithUnits);
            instanceTypeMemoryLabel.setText(instanceType.memoryWithUnits);
            instanceTypeVirtualCoresLabel.setText(Integer.toString(instanceType.numberOfVirtualCores));
        }
    }

    /*
     * Accessors for user entered data
     */

    /**
     * Returns the selected availability zone.
     *
     * @return The selected availability zone.
     */
    public String getAvailabilityZone() {
        return availabilityZoneCombo.getText();
    }

    /**
     * Returns the selected security group.
     *
     * @return The selected security group.
     */
    public String getSecurityGroup() {
        return securityGroupSelectionComposite.getSelectedSecurityGroup().getGroupName();
    }

    /**
     * Returns the String ID of the selected instance type.
     *
     * @return The String ID of the selected instance type.
     */
    public String getInstanceTypeId() {
        String s = instanceTypeCombo.getText();
        InstanceType instanceType = (InstanceType)instanceTypeCombo.getData(s);

        return instanceType.id;
    }

    /**
     * Returns the selected key pair name.
     *
     * @return The selected key pair name.
     */
    public String getKeyPairName() {
        return keyPairComposite.getKeyPairSelectionTable().getSelectedKeyPair().getKeyName();
    }

    /**
     * Returns the arn of the selected instance profile, or null if none is
     * selected.
     */
    public String getInstanceProfileArn() {
        if ( instanceProfileCombo.getSelectionIndex() == 0 ) {
            return null;
        } else {
            return ((InstanceProfile) instanceProfileCombo.getData(instanceProfileCombo.getText())).getArn();
        }
    }

    /**
     * Returns the requested user data to pass to instances.
     *
     * @return The requested user data to pass to instances.
     */
    public String getUserData() {
        return userDataText.getText();
    }

    /**
     * Returns the number of instances requested to be launched.
     *
     * @return The number of instances requested to be launched.
     */
    public int getNumberOfInstances() {
        return numberOfHostsSpinner.getSelection();
    }

}
