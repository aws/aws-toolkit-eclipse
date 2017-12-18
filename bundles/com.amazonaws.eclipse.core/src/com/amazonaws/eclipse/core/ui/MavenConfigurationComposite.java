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

import static com.amazonaws.eclipse.core.model.MavenConfigurationDataModel.P_ARTIFACT_ID;
import static com.amazonaws.eclipse.core.model.MavenConfigurationDataModel.P_GROUP_ID;
import static com.amazonaws.eclipse.core.model.MavenConfigurationDataModel.P_PACKAGE_NAME;
import static com.amazonaws.eclipse.core.model.MavenConfigurationDataModel.P_VERSION;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.validator.PackageNameValidator;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;

/**
 * A reusable Maven configuration composite including configurations such as group id, artifact id etc.
 */
public class MavenConfigurationComposite extends Composite {
    private TextComplex groupIdComplex;
    private TextComplex artifactIdComplex;
    private TextComplex versionComplex;
    private TextComplex packageComplex;

    public MavenConfigurationComposite(Composite parent, DataBindingContext context, MavenConfigurationDataModel dataModel) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createControl(context, dataModel);
    }

    private void createControl(DataBindingContext context, MavenConfigurationDataModel dataModel) {
        groupIdComplex = TextComplex.builder(this, context, PojoObservables.observeValue(dataModel, P_GROUP_ID))
                .addValidator(new NotEmptyValidator("Group ID must be provided!"))
                .modifyListener(e -> {
                    onMavenConfigurationChange();
                })
                .labelValue("Group ID:")
                .defaultValue(dataModel.getGroupId())
                .build();

        artifactIdComplex = TextComplex.builder(this, context, PojoObservables.observeValue(dataModel,  P_ARTIFACT_ID))
                .addValidator(new NotEmptyValidator("Artifact ID must be provided!"))
                .modifyListener(e -> {
                    onMavenConfigurationChange();
                })
                .labelValue("Artifact ID:")
                .defaultValue(dataModel.getArtifactId())
                .build();

        versionComplex = TextComplex.builder(this, context, PojoObservables.observeValue(dataModel, P_VERSION))
                .addValidator(new NotEmptyValidator("Version must be provided!"))
                .labelValue("Version:")
                .defaultValue(dataModel.getVersion())
                .build();

        packageComplex = TextComplex.builder(this, context, PojoObservables.observeValue(dataModel, P_PACKAGE_NAME))
                .addValidator(new PackageNameValidator("Package name must be provided!"))
                .labelValue("Package name:")
                .defaultValue(dataModel.getPackageName())
                .build();
    }

    private void onMavenConfigurationChange() {
        if (packageComplex != null && groupIdComplex != null && artifactIdComplex != null) {
            String groupId = groupIdComplex.getText().getText();
            String artifactId = artifactIdComplex.getText().getText();
            packageComplex.setText(MavenFactory.assumePackageName(groupId, artifactId));
        }
    }
}