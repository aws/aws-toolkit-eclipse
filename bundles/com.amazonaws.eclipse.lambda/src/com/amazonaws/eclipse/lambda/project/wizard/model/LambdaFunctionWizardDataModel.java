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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.model.ProjectNameDataModel;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData;

/**
 * This data model is shared by both New AWS Lambda Java Project wizard and New AWS Lambda Function wizard.
 */
public class LambdaFunctionWizardDataModel {

    public static final String P_SHOW_README_FILE = "showReadmeFile";

    private final ProjectNameDataModel projectNameDataModel = new ProjectNameDataModel();
    private final MavenConfigurationDataModel mavenConfigurationDataModel = new MavenConfigurationDataModel();
    private final LambdaFunctionDataModel lambdaFunctionDataModel = new LambdaFunctionDataModel();

    /* Show README checkbox */
    private boolean showReadmeFile = true;

    public ProjectNameDataModel getProjectNameDataModel() {
        return projectNameDataModel;
    }

    public MavenConfigurationDataModel getMavenConfigurationDataModel() {
        return mavenConfigurationDataModel;
    }

    public LambdaFunctionDataModel getLambdaFunctionDataModel() {
        return lambdaFunctionDataModel;
    }

    public boolean isShowReadmeFile() {
        return showReadmeFile;
    }

    public void setShowReadmeFile(boolean showReadmeFile) {
        this.showReadmeFile = showReadmeFile;
    }

    public PomFileTemplateData collectPomTemplateData() {
        PomFileTemplateData pomData = new PomFileTemplateData();
        pomData.setGroupId(mavenConfigurationDataModel.getGroupId());
        pomData.setArtifactId(mavenConfigurationDataModel.getArtifactId());
        pomData.setVersion(mavenConfigurationDataModel.getVersion());
        pomData.setAwsJavaSdkVersion(MavenFactory.getLatestJavaSdkVersion());
        return pomData;
    }
}
