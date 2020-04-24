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
package com.amazonaws.eclipse.ec2.ui.views.instances;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.ui.SetupAwsAccountAction;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.ec2.model.Instance;

final class CreateAmiAction extends AwsAction {

    private final InstanceSelectionTable instanceSelectionTable;

    public CreateAmiAction(InstanceSelectionTable instanceSelectionTable) {
        super(AwsToolkitMetricType.EXPLORER_EC2_CREATE_AMI_ACTION);
        this.instanceSelectionTable = instanceSelectionTable;
    }

    @Override
    public void doRun() {
        for ( Instance instance : instanceSelectionTable.getAllSelectedInstances() ) {
            createAmiFromInstance(instance);
            actionFinished();
        }
    }

    private void createAmiFromInstance(Instance instance) {
        boolean userIdIsValid = (InstanceSelectionTable.accountInfo.getUserId() != null);
        if ( !InstanceSelectionTable.accountInfo.isValid() || !InstanceSelectionTable.accountInfo.isCertificateValid()
                || !userIdIsValid ) {
            String message = "Your AWS account information doesn't appear to be fully configured yet.  "
                    + "To bundle an instance you'll need all the information configured, "
                    + "including your AWS account ID, EC2 certificate and private key file."
                    + "\n\nWould you like to configure it now?";

            if ( MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Configure AWS Account Information",
                    message) ) {
                new SetupAwsAccountAction().run();
            }

            return;
        }

        KeyPairManager keyPairManager = new KeyPairManager();
        String keyName = instance.getKeyName();
        String keyPairFilePath = keyPairManager.lookupKeyPairPrivateKeyFile(AwsToolkitCore.getDefault().getCurrentAccountId(), keyName);

        if ( keyPairFilePath == null ) {
            String message = "There is no private key registered for the key this host was launched with (" + keyName
                    + ").";
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "No Registered Private Key", message);
            return;
        }

        BundleDialog bundleDialog = new BundleDialog(Display.getCurrent().getActiveShell());
        if ( bundleDialog.open() != IDialogConstants.OK_ID )
            return;

        String bundleName = bundleDialog.getImageName();
        String s3Bucket = bundleDialog.getS3Bucket();

        BundleJob job = new BundleJob(instance, s3Bucket, bundleName);
        job.schedule();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("bundle");
    }

    @Override
    public String getText() {
        return "Bundle AMI...";
    }

    @Override
    public String getToolTipText() {
        return "Create a new Amazon Machine Image from this instance";
    }
}
