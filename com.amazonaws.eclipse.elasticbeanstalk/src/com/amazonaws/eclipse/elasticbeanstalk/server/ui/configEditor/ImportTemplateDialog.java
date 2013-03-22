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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog to prompt the user to select a template to import into their editor.
 */
public class ImportTemplateDialog extends MessageDialog {

    private String templateName;
    private Collection<String> existingTemplateNames;

    public String getTemplateName() {
        return templateName;
    }

    public ImportTemplateDialog(Shell parentShell, Collection<String> existingTemplateNames) {
        super(parentShell, "Import configuration template", null,
                "Choose the configuration template to import into the editor.  "
                        + "This will overwrite any unsaved values in the editor.", MessageDialog.NONE, new String[] {
                        IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        this.existingTemplateNames = existingTemplateNames;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        parent.setLayout(new FillLayout());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));

        new Label(composite, SWT.None).setText("Template to import: ");
        final Combo existingTemplateNamesCombo = new Combo(composite, SWT.READ_ONLY);
        existingTemplateNamesCombo.setItems(existingTemplateNames.toArray(new String[existingTemplateNames.size()]));
        existingTemplateNamesCombo.select(0);

        existingTemplateNamesCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                templateName = existingTemplateNamesCombo.getItem(existingTemplateNamesCombo.getSelectionIndex());
            }

        });

        return composite;
    }
}
