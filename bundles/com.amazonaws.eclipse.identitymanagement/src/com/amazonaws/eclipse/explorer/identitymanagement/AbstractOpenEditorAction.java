/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.identitymanagement;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;

public class AbstractOpenEditorAction extends Action {

    protected String titleName;
    protected String editorId;

    public  AbstractOpenEditorAction(String titleName, String editorId) {
        this.setText("Open " + titleName + " Editor");
        this.titleName = titleName;
        this.editorId = editorId;
    }

    @Override
    public void run() {
        String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoints().get(ServiceAbbreviations.IAM);
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = new EditorInput(titleName, endpoint, accountId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, editorId);
                } catch (PartInitException e) {
                    String errorMessage = "Unable to open the Amazon Identity Management " + titleName.toLowerCase() + "editor: " + e.getMessage();
                    Status status = new Status(Status.ERROR, IdentityManagementPlugin.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
    }

}
