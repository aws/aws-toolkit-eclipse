/*
 * Copyright 2008-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Action to open the views provided by the AWS Toolkit EC2 plugin.
 */
public class OpenAwsViewsAction extends Action {

    /**
     * The list of view IDs that this action should open.
     */
    private static final String[] VIEW_IDS_TO_OPEN = new String[] {
        "com.amazonaws.eclipse.ec2.ui.views.AmiBrowserView",
        "com.amazonaws.eclipse.ec2.ui.views.InstanceView",
        "com.amazonaws.eclipse.ec2.ui.views.ElasticBlockStorageView", 
        "com.amazonaws.eclipse.ec2.views.SecurityGroupView",
    };
    
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        try {
            minimizeWelcomeView();
            openAllAwsToolkitViews();
        } catch (CoreException e) {
            Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to open AWS views: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }

    /**
     * Minimizes the welcome/intro view if it's open and maximized.
     */
    private void minimizeWelcomeView() {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart activePart = workbenchPage.getActivePart();
        
        IIntroPart introPart = (IIntroPart)activePart.getAdapter(IIntroPart.class);
        if (introPart != null) {
            IWorkbenchPartReference reference = workbenchPage.getActivePartReference();
            if (workbenchPage.getPartState(reference) == IWorkbenchPage.STATE_MAXIMIZED) {
                workbenchPage.toggleZoom(reference);
            }
        }
    }
    
    /**
     * Opens all of AWS Toolkit views.
     * 
     * @throws PartInitException
     */
    private void openAllAwsToolkitViews() throws PartInitException {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        for (String viewId : VIEW_IDS_TO_OPEN) {
            workbenchPage.showView(viewId);
        }
    }

}
