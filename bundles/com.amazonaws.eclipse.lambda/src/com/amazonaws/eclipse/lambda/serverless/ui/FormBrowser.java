/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.serverless.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

public class FormBrowser {
    FormToolkit toolkit;
    Composite container;
    ScrolledFormText formText;
    String text;
    int style;

    public FormBrowser(int style) {
        this.style = style;
    }

    public void createControl(Composite parent) {
        toolkit = new FormToolkit(parent.getDisplay());
        int borderStyle = toolkit.getBorderStyle() == SWT.BORDER ? SWT.NULL : SWT.BORDER;
        container = new Composite(parent, borderStyle);
        FillLayout flayout = new FillLayout();
        flayout.marginWidth = 1;
        flayout.marginHeight = 1;
        container.setLayout(flayout);
        formText = new ScrolledFormText(container, SWT.V_SCROLL | SWT.H_SCROLL, false);
        if (borderStyle == SWT.NULL) {
            formText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
            toolkit.paintBordersFor(container);
        }
        FormText ftext = toolkit.createFormText(formText, false);
        formText.setFormText(ftext);
        formText.setExpandHorizontal(true);
        formText.setExpandVertical(true);
        formText.setBackground(toolkit.getColors().getBackground());
        formText.setForeground(toolkit.getColors().getForeground());
        ftext.marginWidth = 2;
        ftext.marginHeight = 2;
        ftext.setHyperlinkSettings(toolkit.getHyperlinkGroup());
        formText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (toolkit != null) {
                    toolkit.dispose();
                    toolkit = null;
                }
            }
        });
        if (text != null)
            formText.setText(text);
    }

    public Control getControl() {
        return container;
    }

    public void setText(String text) {
        this.text = text;
        if (formText != null)
            formText.setText(text);
    }
}