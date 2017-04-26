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
package com.amazonaws.eclipse.ec2.ui.views.instances;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.model.Instance;

public class OpenShellDialogAction extends OpenShellAction {
    public OpenShellDialogAction(InstanceSelectionTable instanceSelectionTable) {
        super(instanceSelectionTable);

        this.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("console"));
        this.setText("Open Shell As...");
        this.setToolTipText("Opens a connection to this host");
    }

    @Override
    public void run() {
        OpenShellDialog openShellDialog = new OpenShellDialog();
        if (openShellDialog.open() < 0) return;

        for (final Instance instance : instanceSelectionTable.getAllSelectedInstances()) {
            openInstanceShell(instance, openShellDialog.getUserName());
        }
    }
}
