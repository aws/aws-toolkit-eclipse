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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

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
        this(parent, context, dataModel, null, null, false);
    }

    public MavenConfigurationComposite(Composite parent, DataBindingContext context, MavenConfigurationDataModel dataModel,
            ModifyListener groupIdModifyListener, ModifyListener artifactIdModifyListener) {
        this(parent, context, dataModel, groupIdModifyListener, artifactIdModifyListener, false);
    }

    public MavenConfigurationComposite(Composite parent, DataBindingContext context, MavenConfigurationDataModel dataModel,
            ModifyListener groupIdModifyListener, ModifyListener artifactIdModifyListener, boolean creatVerionAndPackage) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createControl(context, dataModel, groupIdModifyListener, artifactIdModifyListener, creatVerionAndPackage);
    }

    private void createControl(DataBindingContext context, MavenConfigurationDataModel dataModel,
            ModifyListener groupIdModifyListener, ModifyListener artifactIdModifyListener, boolean creatVerionAndPackage) {

        groupIdComplex = TextComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, MavenConfigurationDataModel.P_GROUP_ID))
                .validator(new NotEmptyValidator("Group ID must be provided!"))
                .modifyListener(groupIdModifyListener)
                .labelValue("Group ID:")
                .defaultValue(dataModel.getGroupId())
                .build();

        artifactIdComplex = TextComplex.builder()
                .composite(this)
                .dataBindingContext(context)
                .pojoObservableValue(PojoObservables.observeValue(dataModel,  MavenConfigurationDataModel.P_ARTIFACT_ID))
                .validator(new NotEmptyValidator("Artifact ID must be provided!"))
                .modifyListener(artifactIdModifyListener)
                .labelValue("Artifact ID:")
                .defaultValue(dataModel.getArtifactId())
                .build();

        if (creatVerionAndPackage) {
            versionComplex = TextComplex.builder()
                    .composite(this)
                    .dataBindingContext(context)
                    .pojoObservableValue(PojoObservables.observeValue(dataModel, MavenConfigurationDataModel.P_VERSION))
                    .validator(new NotEmptyValidator("Version must be provided!"))
                    .labelValue("Version:")
                    .defaultValue(dataModel.getVersion())
                    .build();

            packageComplex = TextComplex.builder()
                    .composite(this)
                    .dataBindingContext(context)
                    .pojoObservableValue(PojoObservables.observeValue(dataModel, MavenConfigurationDataModel.P_PACKAGE_NAME))
                    .validator(new PackageNameValidator("Package name must be provided!"))
                    .labelValue("Package name:")
                    .defaultValue(dataModel.getPackageName())
                    .build();
        }
    }
}