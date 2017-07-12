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

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Simple Link listener that opens a link to a preference page.
 */
public class PreferenceLinkListener implements Listener {

    /** Optional Shell object to close after opening the link */
    private Shell shellToClose;

    /**
     * Sets the Shell object that should be closed after a link is opened.
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
        String preferencePage = event.text;

        if (shellToClose != null) {
            shellToClose.close();
        }
        
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                null, preferencePage, new String[] {preferencePage}, null);
        dialog.open();
    }

}
