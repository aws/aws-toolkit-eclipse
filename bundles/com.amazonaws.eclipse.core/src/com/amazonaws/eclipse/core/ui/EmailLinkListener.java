/*
 * Copyright 2008-2014 Amazon Technologies, Inc.
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

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.amazonaws.eclipse.core.diagnostic.utils.EmailMessageLauncher;

/**
 * Implementation of Listener that launches an email client and opens up an
 * email message with the specified recipient, subject and content.
 */
public class EmailLinkListener implements Listener {

    private final EmailMessageLauncher launcher;

    public EmailLinkListener(final EmailMessageLauncher launcher) {
        this.launcher = launcher;
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
     */
    @Override
    public void handleEvent(Event event) {
        launcher.open();
    }

}
