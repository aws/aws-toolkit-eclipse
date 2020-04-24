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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.PlatformUtils;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;
import com.amazonaws.eclipse.ec2.ui.SetupExternalToolsAction;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.ec2.model.Instance;

class OpenShellAction extends AwsAction {

    protected final InstanceSelectionTable instanceSelectionTable;

    public OpenShellAction(InstanceSelectionTable instanceSelectionTable) {
        this(AwsToolkitMetricType.EXPLORER_EC2_OPEN_SHELL_ACTION, instanceSelectionTable);
    }

    protected OpenShellAction(AwsToolkitMetricType metricType, InstanceSelectionTable instanceSelectionTable) {
        super(metricType);
        this.instanceSelectionTable = instanceSelectionTable;

        this.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("console"));
        this.setText("Open Shell");
        this.setToolTipText("Opens a connection to this host");
    }

    @Override
    public void doRun() {
        for ( final Instance instance : instanceSelectionTable.getAllSelectedInstances() ) {
            try {
                openInstanceShell(instance);
                actionSucceeded();
            } catch (Exception e) {
                actionFailed();
                Ec2Plugin.getDefault().reportException("Unable to open a shell to the selected instance: " + e.getMessage(), e);
            } finally {
                actionFinished();
            }
        }
    }

    protected void openInstanceShell(Instance instance) throws Exception {
        openInstanceShell(instance, null);
    }

    protected void openInstanceShell(Instance instance, String user) throws Exception {
        PlatformUtils platformUtils = new PlatformUtils();

        KeyPairManager keyPairManager = new KeyPairManager();

        String keyName = instance.getKeyName();
        String keyPairFilePath = keyPairManager.lookupKeyPairPrivateKeyFile(AwsToolkitCore.getDefault().getCurrentAccountId(), keyName);

        if (user == null) {
            user = Ec2Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SSH_USER);
        }

        if ( keyPairFilePath == null ) {
            throw new Exception("Unable to locate the private key file for the selected key '" + keyName + "'");
        }

        if ( !platformUtils.isSshClientConfigured() ) {
            String message = "Your SSH client doesn't appear to be configured correctly yet.  Would you like to configure it now?";

            if ( MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "SSH Client Configuration",
                    message) ) {
                new SetupExternalToolsAction().run();
            }

            return;
        }

        platformUtils.openShellToRemoteHost(user, instance.getPublicDnsName(), keyPairFilePath);
    }

}
