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

import static com.amazonaws.eclipse.core.model.ProjectNameDataModel.P_PROJECT_NAME;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.model.ProjectNameDataModel;
import com.amazonaws.eclipse.core.validator.ProjectNameValidator;
import com.amazonaws.eclipse.core.widget.TextComplex;

/**
 * A reusable Project Name composite.
 */
public class ProjectNameComposite extends Composite {

    private TextComplex projectNameComplex;

    public ProjectNameComposite(Composite parent, DataBindingContext context, ProjectNameDataModel dataModel) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createControl(context, dataModel);
    }

    private void createControl(DataBindingContext context, ProjectNameDataModel dataModel) {
        projectNameComplex = TextComplex.builder(this, context, PojoObservables.observeValue(dataModel, P_PROJECT_NAME))
                .addValidator(new ProjectNameValidator())
                .defaultValue(dataModel.getProjectName())
                .labelValue("Project name:")
                .build();
    }
}
