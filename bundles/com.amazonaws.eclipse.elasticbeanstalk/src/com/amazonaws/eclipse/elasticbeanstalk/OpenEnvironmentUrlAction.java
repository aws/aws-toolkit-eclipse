/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.wst.server.core.IServer;

import com.amazonaws.eclipse.core.BrowserUtils;

public class OpenEnvironmentUrlAction implements IObjectActionDelegate {
    private String environmentUrl;

    @Override
    public void run(IAction action) {
        if (environmentUrl != null) BrowserUtils.openExternalBrowser(environmentUrl);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        StructuredSelection structuredSelection = (StructuredSelection)selection;
        IServer selectedServer = (IServer)structuredSelection.getFirstElement();

        if (selectedServer != null) {
            Environment environment = (Environment)selectedServer.loadAdapter(Environment.class, null);
            environmentUrl = environment.getEnvironmentUrl();
        }

        action.setEnabled(selectedServer != null &&
                          selectedServer.getServerState() == IServer.STATE_STARTED);
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {}
}
