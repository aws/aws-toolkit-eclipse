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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.AwsAction;

final class RebootInstancesAction extends AwsAction {

    private final InstanceSelectionTable instanceSelectionTable;

    RebootInstancesAction(InstanceSelectionTable instanceSelectionTable) {
        super(AwsToolkitMetricType.EXPLORER_EC2_REBOOT_ACTION);
        this.instanceSelectionTable = instanceSelectionTable;
    }

    @Override
    public void doRun() {
        MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);

        messageBox.setText("Reboot selected instances?");
        messageBox.setMessage("If you continue, you won't be able to access these instances " +
                "until they finish rebooting.");

        // Bail out if the user cancels...
        if (messageBox.open() == SWT.CANCEL) {
            actionCanceled();
        } else {
            new RebootInstancesThread(instanceSelectionTable, instanceSelectionTable.getAllSelectedInstances()).start();
            actionSucceeded();
        }
        actionFinished();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("reboot");
    }

    @Override
    public String getText() {
        return "Reboot";
    }

    @Override
    public String getToolTipText() {
        return "Reboot instances";
    }
}
