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
package com.amazonaws.eclipse.codedeploy.explorer.action;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.explorer.editor.DeploymentGroupEditor;
import com.amazonaws.eclipse.codedeploy.explorer.editor.DeploymentGroupEditorInput;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;

public class OpenDeploymentGroupEditorAction extends AwsAction {

    private final String applicationName;
    private final String deploymentGroupName;
    private final Region region;

    public OpenDeploymentGroupEditorAction(String applicationName,
            String deploymentGroupName, Region region) {
        super(AwsToolkitMetricType.EXPLORER_CODEDEPLOY_OPEN_DEPLOYMENT_GROUP);
        this.applicationName = applicationName;
        this.deploymentGroupName = deploymentGroupName;
        this.region = region;

        this.setText("Open in Deployment Group Editor");
    }

    @Override
    public void doRun() {
        String endpoint = region.getServiceEndpoint(ServiceAbbreviations.CODE_DEPLOY);
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = new DeploymentGroupEditorInput(
                applicationName, deploymentGroupName, endpoint, accountId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input,
                            DeploymentGroupEditor.ID);
                    actionSucceeded();
                } catch (PartInitException e) {
                    CodeDeployPlugin.getDefault().reportException(
                            "Unable to open the Deployment Group editor", e);
                    actionFailed();
                } finally {
                    actionFinished();
                }
            }
        });
    }
}
