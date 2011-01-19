/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core;

import java.net.URL;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.statushandlers.StatusManager;

public class BrowserUtils {
    
    private static final int BROWSER_STYLE = IWorkbenchBrowserSupport.LOCATION_BAR 
                                           | IWorkbenchBrowserSupport.AS_EXTERNAL
                                           | IWorkbenchBrowserSupport.STATUS
                                           | IWorkbenchBrowserSupport.NAVIGATION_BAR;
    /**
     * Opens the specified URL in an external browser window.
     * @param url The URL to open.
     */
    public static void openExternalBrowser(String url) {
        try {
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            browserSupport.createBrowser(BROWSER_STYLE, null, null, null)
                .openURL(new URL(url));
        } catch (Exception e) {
            Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, 
                    "Unable to open external web browser to '" + url + "': " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.SHOW);
        }
    }
}
