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

package com.amazonaws.eclipse.ec2.ui.ebs;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Snapshot;

/**
 * A dialog that allows a user to enter information for creating a new EBS
 * volume.
 */
public class CreateNewVolumeDialog extends Dialog {

    /** Shared client factory */
    private AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /** Spinner control for specifying how large of a volume to create */
    private Spinner sizeSpinner;

    /** Combo box to select what availability zone to create a volume in */
    private Combo availabilityZoneCombo;

    /** The volume size */
    private int size;

    /** The availability zone to create a volume in */
    private String availabilityZone;

    /** The radio button for creating a volume by size */
    private Button emptyVolumeRadioButton;

    /** The radio button for creating a volume from a snapshot */
    private Button useSnapshotRadioButton;

    /** The snapshot selection table */
    private SnapshotSelectionTable snapshotSelectionComposite;

    /** The id of the selected snapshot */
    private String snapshotId;

    /**
     * The device the new volume should be attached to.  This is only
     * set if the the new volume being created is explicitly being created
     * to be attached to a specific instance.
     */
    private String device;

    /**
     * The instance to which this new volume is being attached. This is only
     * present if the new volume being created is explicitly being created to be
     * attached to an instance, and isn't set during the normal
     * "Create New Volume" workflow.
     */
    private Instance instance = null;

    private Combo deviceCombo;

    /**
     * Creates a new dialog ready to be opened to collect information from the
     * user on how to create a new EBS volume.
     *
     * @param parentShell
     *            The parent shell for this new dialog window.
     */
    public CreateNewVolumeDialog(Shell parentShell) {
        super(parentShell);
    }

    /**
     * Creates a new dialog ready to be opened to collect information from the
     * user on how to create a new EBS volume which will be attached to the
     * specified instance.  When creating a new volume explicitly for attaching
     * it to a specific instance, certain parts of the dialog are different,
     * such as the availability zone being locked down to the zone the specified
     * instance is in, and a new Combo box is present that allows the user to
     * select what device the new EBS volume should be attached as.
     *
     * @param parentShell
     *            The parent shell for this new dialog window.
     * @param instance
     *            The instance that the new EBS volume will be attached to.
     */
    public CreateNewVolumeDialog(Shell parentShell, Instance instance) {
        this(parentShell);
        this.instance = instance;
    }

    /**
     * Returns the selected volume size. This method can only be called after
     * the dialog's Ok button has been pressed.
     *
     * @return The selected volume size.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the selected availability zone. This method can only be called
     * after the dialog's Ok button has been pressed.
     *
     * @return The selected availability zone.
     */
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    /**
     * Returns the selected device that this new volume should be attached to.
     *
     * @return The selected device that this new volume should be attached to.
     */
    public String getDevice() {
        return device;
    }

    /**
     * Returns the selected snapshot id.  This method can only be called after
     * the dialog's Ok button has been pressed.
     *
     * @return The selected snapshot id.
     */
    public String getSnapshotId() {
        return snapshotId;
    }

    /*
     * Dialog Interface
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        GridData gridData;

        SelectionListener selectionListener = new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateControls();
            }
        };

        /*
         * If we aren't creating this new volume explicitly to attach it to
         * a specific instance, then we can let the user select an availability
         * zone, otherwise we need to lock it down to the availability zone of the
         * instance that will be attaching this new volume.
         */
        newLabel("Availability Zone:", composite);
        if (instance == null) {
            availabilityZoneCombo = new Combo(composite, SWT.READ_ONLY);
            availabilityZoneCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            populateAvailabilityZoneCombo();
        } else {
            newLabel(instance.getPlacement().getAvailabilityZone(), composite);
        }

        /*
         * If we are creating this new volume explicitly for attaching it to a
         * specified instance, then we also need to let the user select the
         * device they want the volume attached as.
         */
        if (instance != null) {
            newLabel("Device:", composite);
            deviceCombo = new Combo(composite, SWT.READ_ONLY);
            deviceCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            String devicePrefix = "/dev/sd";
            for (char c = 'b'; c <= 'z'; c++) {
                deviceCombo.add(devicePrefix + c);
            }
            deviceCombo.setText("/dev/sdh");
        }

        emptyVolumeRadioButton = new Button(composite, SWT.RADIO);
        emptyVolumeRadioButton.setText("Create empty volume by size (GB):");
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 1;
        emptyVolumeRadioButton.setLayoutData(gridData);
        emptyVolumeRadioButton.addSelectionListener(selectionListener);

        sizeSpinner = new Spinner(composite, SWT.NONE);
        sizeSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sizeSpinner.setMinimum(1);
        sizeSpinner.setMaximum(1024);
        sizeSpinner.setEnabled(false);

        useSnapshotRadioButton = new Button(composite, SWT.RADIO);
        useSnapshotRadioButton.setText("Or create volume from existing snapshot:");
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        useSnapshotRadioButton.setLayoutData(gridData);
        useSnapshotRadioButton.addSelectionListener(selectionListener);

        snapshotSelectionComposite = new SnapshotSelectionTable(composite);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        gridData.horizontalIndent = 15;
        gridData.heightHint = 150;
        snapshotSelectionComposite.setLayoutData(gridData);

        snapshotSelectionComposite.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateControls();
            }
        });

        return composite;
    }

    /**
     * Populates the availability zone combo box with the available zones.
     */
    private void populateAvailabilityZoneCombo() {
        if (availabilityZoneCombo == null) return;

        try {
            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            for (AvailabilityZone zone : ec2.describeAvailabilityZones().getAvailabilityZones()) {
                availabilityZoneCombo.add(zone.getZoneName());
            }
            availabilityZoneCombo.select(0);
        } catch (Exception e) {
            Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID, "Unable to list availability zones");
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#create()
     */
    @Override
    public void create() {
        super.create();

        String title = null;
        if (instance == null) {
            title = "Create New EBS Volume";
        } else {
            title = "Attach New EBS Volume";
        }

        getShell().setText(title);
        emptyVolumeRadioButton.setSelection(true);
        updateControls();
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        size = sizeSpinner.getSelection();

        if (availabilityZoneCombo != null) {
            availabilityZone = availabilityZoneCombo.getText();
        }

        if (deviceCombo != null) {
            device = deviceCombo.getText();
        }

        Snapshot selectedSnapshot = snapshotSelectionComposite.getSelectedSnapshot();
        if (selectedSnapshot != null) {
            snapshotId = selectedSnapshot.getSnapshotId();
        }

        super.okPressed();
    }

    /*
     * Private Interface
     */

    private void updateControls() {
        boolean creatingFromSnapshot = useSnapshotRadioButton.getSelection();
        Button okButton = getButton(IDialogConstants.OK_ID);

        snapshotSelectionComposite.setEnabled(creatingFromSnapshot);
        sizeSpinner.setEnabled(!creatingFromSnapshot);

        if (creatingFromSnapshot) {
            if (okButton != null) {
                Snapshot snapshot = snapshotSelectionComposite.getSelectedSnapshot();
                okButton.setEnabled(snapshot != null);
            }
        } else {
            snapshotSelectionComposite.clearSelection();
            if (okButton != null) okButton.setEnabled(true);
        }
    }

    private Label newLabel(String text, Composite parent) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(text);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
        l.setLayoutData(gridData);

        return l;
    }

}
