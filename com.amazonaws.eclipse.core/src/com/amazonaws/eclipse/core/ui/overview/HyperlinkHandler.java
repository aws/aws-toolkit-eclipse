/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.overview;

import java.net.URL;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Generic hyperlink handler that knows how to handle basic link href actions.
 * <p>
 * <b>HTTP/HTTPS web links</b> - Hyperlink hrefs starting with "http" will be
 * treated as a web URL and opened in an external web browser.
 * <p>
 * <b>Eclipse preference page links</b> - Hyperlink hrefs starting with
 * "preference:" will open Eclipse's preference dialog to the specified
 * preference page (ex: 'preference:com.foo.bar.preference-id').
 * <p>
 * <b>Eclipse help links</b> - Hyperlink hrefs starting with "help:" will open
 * Eclipse's help system to the specified help content (ex:
 * 'help:com.foo.bar.preference-id').
 * <p>
 * <b>Email links</b> - Hyperlink hrefs starting with "mailto:" will open
 * up a a new email message in the system's default email client.
 */
public final class HyperlinkHandler extends HyperlinkAdapter {

    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.events.HyperlinkAdapter#linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent)
     */
    @Override
    public void linkActivated(HyperlinkEvent event) {
        String href = (String)event.getHref();
        if (href == null) return;
        String resource = href.substring(href.indexOf(":") + 1);

        if (href.startsWith("http")) {
            int browserStyle = IWorkbenchBrowserSupport.LOCATION_BAR
                | IWorkbenchBrowserSupport.AS_EXTERNAL
                | IWorkbenchBrowserSupport.STATUS
                | IWorkbenchBrowserSupport.NAVIGATION_BAR;
            IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
            try {
                browserSupport.createBrowser(browserStyle, null, null, null)
                    .openURL(new URL(href));
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to open external web browser", e);
            }
        } else if (href.startsWith("preference:")) {
            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                    null, resource, new String[] {resource}, null);
            dialog.open();
        } else if (href.startsWith("help:")) {
            PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(resource);
        } else if (href.startsWith("mailto:")) {
            Program.launch(href);
        }
    }
}
