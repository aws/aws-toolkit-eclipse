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
package com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;


/**
 * Opens up the custom query editor on a given domain
 */
public class OpenQueryEditorAction extends Action {

    private final String domainName;

    public OpenQueryEditorAction(final String domainName) {
        this.domainName = domainName;
        setText("Open Query Editor");
        setToolTipText("Opens the query editor to run queries against this domain");
    }


    @Override
    public void run() {
        try {
            IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            workbenchPage.openEditor(new DomainEditorInput(this.domainName, AwsToolkitCore.getDefault()
                    .getCurrentAccountId()), QueryEditor.ID);
        } catch ( PartInitException e ) {
            AwsToolkitCore.getDefault().logError(e.getMessage(), e);
        }
    }
}
