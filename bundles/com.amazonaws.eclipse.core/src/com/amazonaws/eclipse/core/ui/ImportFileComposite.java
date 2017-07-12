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
package com.amazonaws.eclipse.core.ui;

import static com.amazonaws.eclipse.core.model.ImportFileDataModel.P_FILE_PATH;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newPushButton;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.widget.TextComplex;

/**
 * A reusable File import widget composite.
 */
public class ImportFileComposite extends Composite {

    private TextComplex filePathComplex;
    private Button browseButton;
    private final IValidator filePathValidator;

    public ImportFileComposite(Composite parent, DataBindingContext context,
            ImportFileDataModel dataModel, IValidator validator) {
        super(parent, SWT.NONE);
        filePathValidator = validator;
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        setLayout(new GridLayout(3, false));
        createControl(context, dataModel);
    }

    @Override
    public void setEnabled(boolean enabled) {
        filePathComplex.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }

    private void createControl(DataBindingContext context, ImportFileDataModel dataModel) {
        filePathComplex = TextComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_FILE_PATH))
                .labelValue("Import:")
                .validator(filePathValidator)
                .defaultValue(dataModel.getFilePath())
                .build();

        browseButton = newPushButton(this, "Browse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
                String path = dialog.open();
                if (path != null) filePathComplex.setText(path);
            }
        });
    }

}
