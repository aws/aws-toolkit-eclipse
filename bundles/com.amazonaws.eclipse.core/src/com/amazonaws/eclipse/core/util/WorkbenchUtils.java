/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.util;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * The IWorkbench.getActiveWorkbenchWindow must be invoked in the UI thread. Otherwise, null will be returned.
 */
public class WorkbenchUtils {

    public static void selectAndReveal(final IResource resource, final IWorkbench workbench) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                if (resource == null || workbench == null) return;
                BasicNewResourceWizard.selectAndReveal(resource, workbench.getActiveWorkbenchWindow());
            }
        });
    }

    public static void openFileInEditor(final IFile file, final IWorkbench workbench) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                if (file == null || workbench == null) return;
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                if (window != null) {
                    IWorkbenchPage page = window.getActivePage();
                    try {
                        IDE.openEditor(page, file, true);
                    } catch (PartInitException e) {
                        AwsToolkitCore.getDefault().logWarning(String.format(
                                "Failed to open file %s in the editor!", file.toString()), e);
                    }
                }
            }
        });
    }

    public static void openInternalBrowserAsEditor(final URL url, final IWorkbench workbench) {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                try {
                    if (url == null || workbench == null) return;
                    IWorkbenchBrowserSupport browserSupport = workbench.getBrowserSupport();
                    browserSupport.createBrowser(IWorkbenchBrowserSupport.AS_EDITOR, null, null, null)
                        .openURL(url);
                } catch (PartInitException e) {
                    AwsToolkitCore.getDefault().logWarning(String.format("Failed to open the url %s in the editor!", url.toString()), e);
                }
            }
        });
    }

}
