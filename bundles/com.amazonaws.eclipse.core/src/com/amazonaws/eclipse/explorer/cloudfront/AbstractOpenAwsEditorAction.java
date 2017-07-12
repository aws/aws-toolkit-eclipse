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
package com.amazonaws.eclipse.explorer.cloudfront;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;

public abstract class AbstractOpenAwsEditorAction extends Action {
    private final String editorName;
    private final String editorId;
    private final String serviceAbbreviation;

    public abstract IEditorInput createEditorInput(String endpoint, String accountId);

    public AbstractOpenAwsEditorAction(String editorName, String editorId, String serviceAbbreviation) {
        this.editorName = editorName;
        this.editorId = editorId;
        this.serviceAbbreviation = serviceAbbreviation;

        this.setText("Open in " + editorName);
    }

    @Override
    public void run() {
        String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoints().get(serviceAbbreviation);
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = createEditorInput(endpoint, accountId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, editorId);
                } catch (PartInitException e) {
                    AwsToolkitCore.getDefault().logError("Unable to open the " + editorName, e);
                }
            }
        });
    }

}
