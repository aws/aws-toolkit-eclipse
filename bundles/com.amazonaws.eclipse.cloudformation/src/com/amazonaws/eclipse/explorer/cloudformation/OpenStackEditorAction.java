/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;

public class OpenStackEditorAction extends Action {
    private final String stackId;
    private Region region;
    private final boolean autoRefresh;

    public OpenStackEditorAction(String stackId) {
        this(stackId, null, false);
    }

    public OpenStackEditorAction(String stackId, Region region, boolean autoRefresh) {
        this.setText("Open in Stack Editor");
        this.stackId = stackId;
        this.region = region;
        this.autoRefresh = autoRefresh;
    }

    @Override
    public void run() {
        Region selectedRegion = region == null ? RegionUtils.getCurrentRegion() : region;
        String endpoint = selectedRegion.getServiceEndpoints().get(ServiceAbbreviations.CLOUD_FORMATION);
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = new StackEditorInput(stackId, endpoint, accountId, autoRefresh);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, "com.amazonaws.eclipse.explorer.cloudformation.stackEditor");
                } catch (PartInitException e) {
                    String errorMessage = "Unable to open the Amazon CloudFormation stack editor: " + e.getMessage();
                    Status status = new Status(Status.ERROR, CloudFormationPlugin.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
    }
}
