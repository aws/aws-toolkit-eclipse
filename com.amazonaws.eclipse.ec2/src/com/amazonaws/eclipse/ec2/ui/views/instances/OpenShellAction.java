/*
 * Copyright 2009-2011 Amazon Technologies, Inc.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.PlatformUtils;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;
import com.amazonaws.eclipse.ec2.ui.SetupExternalToolsAction;
import com.amazonaws.services.ec2.model.Instance;

final class OpenShellAction extends Action {

    private final InstanceSelectionTable instanceSelectionTable;

    public OpenShellAction(InstanceSelectionTable instanceSelectionTable) {
        this.instanceSelectionTable = instanceSelectionTable;
    }

    public void run() {
        for ( final Instance instance : instanceSelectionTable.getAllSelectedInstances() ) {
            openInstanceShell(instance);
        }
    }

    private void openInstanceShell(Instance instance) {
        PlatformUtils platformUtils = new PlatformUtils();

        try {
            KeyPairManager keyPairManager = new KeyPairManager();

            String keyName = instance.getKeyName();
            String keyPairFilePath = keyPairManager.lookupKeyPairPrivateKeyFile(keyName);
            String user = Ec2Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SSH_USER);

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
        } catch ( Exception e ) {
            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to open a shell to the selected instance: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
    
    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("console");
    }

    @Override
    public String getText() {
        return "Open Shell";
    }

    @Override
    public String getToolTipText() {
        return "Opens a secure shell to this host";
    }
}
