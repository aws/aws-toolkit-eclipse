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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;

/**
 * Dialog to inform the user that they can't connect to AWS Elastic Beanstalk, with a
 * customizable message.
 */
public class CantConnectDialog extends MessageDialog {

    public CantConnectDialog(String message) {
        super(Display.getDefault().getActiveShell(), "Unable to connect to AWS Elastic Beanstalk", AwsToolkitCore.getDefault().getImageRegistry()
                .get(AwsToolkitCore.IMAGE_AWS_ICON), message, 0, new String[] { "OK" }, 0);
    }

    @Override
    protected Control createCustomArea(Composite parent) {

        final Hyperlink link = new Hyperlink(parent, SWT.NONE);
        link.setText("Click here to learn more about AWS Elastic Beanstalk");
        link.setHref("https://aws.amazon.com/elasticbeanstalk/");
        link.setUnderlined(true);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                BrowserUtils.openExternalBrowser(link.getHref().toString());
            }
        });

        return parent;
    }

}
