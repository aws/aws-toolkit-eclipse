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
package com.amazonaws.eclipse.lambda.launching;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;

import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.lambda.LambdaPlugin;

/**
 * Recognize the console URL and convert it as a link to be clickable.
 */
public class SamLocalConsoleLineTracker implements IConsoleLineTracker {
    // The pattern accept a four-digit IP address or localhost as a special IP address, with a port number at the end.
    private static final String URL_LINK_PATTERN_STRING  = "((http|https)://)?"
            + "(((\\d|\\d{2}|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(\\d|\\d{2}|1\\d{2}|2[0-4]\\d|25[0-5])|localhost)"
            + ":(6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5]|[0-5]?\\d{1,5})";

    public static final Pattern URL_PATTERN = Pattern.compile(URL_LINK_PATTERN_STRING);

    private IConsole console;

    @Override
    public void init(IConsole console) {
        this.console = console;
    }

    @Override
    public void lineAppended(IRegion line) {
        try {
            int offset = line.getOffset();
            int length = line.getLength();
            String text = console.getDocument().get(offset, length);
            Matcher matcher = URL_PATTERN.matcher(text);

            String url = null;
            if (matcher.find()) {
                url = matcher.group();
                offset += matcher.start();
            }

            if (url != null) {
                final String thisUrl = url;
                console.addLink(new IHyperlink() {

                    @Override
                    public void linkExited() {}

                    @Override
                    public void linkEntered() {}

                    @Override
                    public void linkActivated() {
                        try {
                            WorkbenchUtils.openInternalBrowserAsEditor(new URL(thisUrl), PlatformUI.getWorkbench());
                        } catch (MalformedURLException e) {
                            LambdaPlugin.getDefault().logError("Cannot open the URL: " + thisUrl, e);
                        }
                    }
                }, offset, url.length());
            }
        } catch (BadLocationException e) {
            LambdaPlugin.getDefault().logError("Failed to process the console document.", e);
        }
    }

    @Override
    public void dispose() {
    }
}
