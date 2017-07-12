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

package com.amazonaws.eclipse.core.ui;

import java.util.logging.Logger;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.ui.preferences.AwsAccountPreferencePage;

/**
 * Implementation of Listener that opens an internal browser pointing to the
 * text in the event object and optionally closes a shell after opening the
 * link if the caller requested that one be closed.
 */
public class WebLinkListener implements Listener {

    /** Optional Shell object to close after opening the link */
    private Shell shellToClose;

    /**
     * Sets the Shell object that should be closed after a link is opened.
     * This is useful for preference pages so that they can ensure the
     * preference dialog is closed so that users can see the link that was
     * just opened.
     *
     * @param shell
     *            The Shell to close after opening the selected link.
     */
    public void setShellToClose(Shell shell) {
        shellToClose = shell;
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
        try {
            BrowserUtils.openExternalBrowser(event.text);

            if (shellToClose != null) {
                shellToClose.close();
            }
        } catch (Exception e) {
            Logger logger = Logger.getLogger(AwsAccountPreferencePage.class.getName());

            logger.warning("Unable to open link to '" + event.text + "': " + e.getMessage());
        }
    }
}
