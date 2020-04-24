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
package com.amazonaws.eclipse.explorer.ec2;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.AwsAction;

import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.AMIS_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.INSTANCES_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.EBS_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.SECURITY_GROUPS_VIEW_ID;

public class OpenViewAction extends AwsAction {
    private final String viewId;

    public OpenViewAction(String viewId) {
        super(mapToMetricType(viewId));
        this.viewId = viewId;
    }

    @Override
    public void doRun() {
        try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewId);
            actionSucceeded();
        } catch (PartInitException e) {
            actionFailed();
            IStatus status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                "Unable to open view " + viewId, e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        } finally {
            actionFinished();
        }
    }

    private static AwsToolkitMetricType mapToMetricType(String viewId) {
        switch(viewId) {
        case AMIS_VIEW_ID:
            return AwsToolkitMetricType.EXPLORER_EC2_OPEN_AMIS_VIEW;
        case INSTANCES_VIEW_ID:
            return AwsToolkitMetricType.EXPLORER_EC2_OPEN_INSTANCES_VIEW;
        case EBS_VIEW_ID:
            return AwsToolkitMetricType.EXPLORER_EC2_OPEN_EBS_VIEW;
        case SECURITY_GROUPS_VIEW_ID:
            return AwsToolkitMetricType.EXPLORER_EC2_OPEN_SECURITY_GROUPS_VIEW;
        }
        return AwsToolkitMetricType.EXPLORER_EC2_OPEN_VIEW;
    }
}
