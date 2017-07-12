/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk;

import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Dialog to collect information about a deployment prior to its creation.
 */
public class DeploymentInformationDialog extends MessageDialog {

    private final boolean enableDebugging;
    private final boolean warnAboutIngress;
    private final boolean showVersionTextBox;

    private final Environment environment;
    private final String launchMode;

    private Text debugPortText;
    private String debugPort = "";
    private String debugInstanceId = "";
    private String versionLabel = "";

    String getDebugInstanceId() {
        return debugInstanceId;
    }

    String getVersionLabel() {
        return versionLabel;
    }

    String getDebugPort() {
        return debugPort;
    }

    public DeploymentInformationDialog(Shell parentShell, Environment environment, String launchMode, boolean showVersionLabelTextBox, boolean enableDebugging, boolean warnAboutIngress) {
        super(parentShell, "Publishing to AWS Elastic Beanstalk", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), "Configure your environment deployment options",
                MessageDialog.NONE, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);

        this.showVersionTextBox = showVersionLabelTextBox;
        this.enableDebugging = enableDebugging;
        this.warnAboutIngress = warnAboutIngress;
        this.environment = environment;
        this.launchMode = launchMode;

        this.versionLabel = "v" + System.currentTimeMillis();
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        Composite composite = parent;
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        composite.setLayout(layout);

        if ( showVersionTextBox )
            createVersionTextBox(composite);
        if ( enableDebugging )
            createDebugPortTextBox(parent);
        if ( launchMode.equals(ILaunchManager.DEBUG_MODE) )
            createInstanceSelectionCombo(parent);
        if ( warnAboutIngress )
            createIngressWarning(parent);

        createDurationLabel(composite);

        composite.pack(true);
        return composite;
    }

    /**
     * Creates a combo selection box to choose which EC2 instance to connect to,
     * if there's more than one.
     */
    private void createInstanceSelectionCombo(Composite parent) {
        try {
            final List<String> ec2InstanceIds = environment.getEC2InstanceIds();
            if ( ec2InstanceIds.size() < 2 ) {
                return;
            }

            debugInstanceId = ec2InstanceIds.get(0);

            Label label = new Label(parent, SWT.None);
            label.setText("Connect remote debugger to instance: ");

            final Combo combo = new Combo(parent, SWT.READ_ONLY);
            combo.setItems(ec2InstanceIds.toArray(new String[ec2InstanceIds.size()]));
            combo.select(0);
            combo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    debugInstanceId = ec2InstanceIds.get(combo.getSelectionIndex());
                }
            });

            GridDataFactory.fillDefaults().grab(true, false).applyTo(combo);
        } catch (AmazonServiceException ignored) {
            return;
        }

    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        validate();
    }

    private void validate() {
        if ( versionLabel.length() == 0 ) {
            getButton(0).setEnabled(false);
            return;
        }
        if ( enableDebugging ) {
            if ( debugPort.length() == 0 ) {
                getButton(0).setEnabled(false);
                return;
            }
            try {
                Integer.parseInt(debugPort);
            } catch ( NumberFormatException e ) {
                getButton(0).setEnabled(false);
            }
        }
        getButton(0).setEnabled(true);
    }

    private void createVersionTextBox(Composite composite) {
        Label versionLabelLabel = new Label(composite, SWT.NONE);
        versionLabelLabel.setText("Version Label:");

        final Text versionLabelText = new Text(composite, SWT.BORDER);
        versionLabelText.setText(versionLabel);
        versionLabelText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        versionLabelText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                versionLabel = versionLabelText.getText();
                validate();
            }
        });
    }

    private void createDebugPortTextBox(Composite parent) {
        Label chooseADebugPort = new Label(parent, SWT.READ_ONLY);
        chooseADebugPort.setText("Remote debugging port:");
        debugPortText = new Text(parent, SWT.BORDER);
        debugPortText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                debugPort = debugPortText.getText();
                validate();
            }
        });
    }

    private void createIngressWarning(Composite parent) {
        Label ingressWarning = new Label(parent, SWT.READ_ONLY | SWT.WRAP);
        ingressWarning.setText("Please note: to connect the remote debugger, "
                + "the debug port will be opened for TCP ingress " + "on your EC2 security group.");
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).span(2, 1).applyTo(ingressWarning);
    }

    private void createDurationLabel(Composite composite) {
        final Label info = new Label(composite, SWT.READ_ONLY | SWT.WRAP);
        info.setText("Launching a new environment may take several minutes.  "
                + "To monitor its progress, check the Progress View.");
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.horizontalSpan = 2;
        gridData.widthHint = 300;
        info.setLayoutData(gridData);
    }

}
