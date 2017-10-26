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
package com.amazonaws.eclipse.lambda.model;

import com.amazonaws.eclipse.core.model.AbstractAwsToolkitDataModel;
import com.amazonaws.eclipse.lambda.blueprint.BlueprintsProvider;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprint;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprintsConfig;
import com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData;

/**
 * Data model for Lambda function composite.
 */
public class LambdaFunctionDataModel extends AbstractAwsToolkitDataModel {

    private static final String P_PACKAGE_NAME = "packageName";
    public static final String P_CLASS_NAME = "className";
    public static final String P_INPUT_TYPE = "inputType";

    private static final LambdaBlueprintsConfig BLUEPRINTS_CONFIG = BlueprintsProvider.provideLambdaBlueprints();

    private String className = "LambdaFunctionHandler";
    private String packageName = "com.amazonaws.lambda.demo";

    private LambdaBlueprint selectedBlueprint = BLUEPRINTS_CONFIG.getBlueprints()
            .get(BLUEPRINTS_CONFIG.getDefaultBlueprint());
    private String inputType = selectedBlueprint.getDisplayName();

    public LambdaBlueprintTemplateData collectLambdaBlueprintTemplateData() {

        if (getSelectedBlueprint() == null) {
            throw new RuntimeException("The specified blueprint " + getInputType() + " doesn't exist!");
        }

        LambdaBlueprintTemplateData data = new LambdaBlueprintTemplateData();

        data.setPackageName(getPackageName());
        data.setHandlerClassName(getClassName());
        data.setHandlerTestClassName(getClassName() + "Test");
        data.setInputJsonFileName(getSelectedBlueprint().getTestJsonFile());
        return data;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.setProperty(P_PACKAGE_NAME, packageName, this::getPackageName, (newValue) -> this.packageName = newValue);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.setProperty(P_CLASS_NAME, className, this::getClassName, (newValue) -> this.className = newValue);
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.selectedBlueprint = getLambdaBlueprint(inputType);
        this.setProperty(P_INPUT_TYPE, inputType, this::getInputType, (newValue) -> this.inputType = newValue);
    }

    public LambdaBlueprint getSelectedBlueprint() {
        return selectedBlueprint;
    }

    /**
     * Return the LambdaBlueprint by display name.
     */
    private static LambdaBlueprint getLambdaBlueprint(String displayName) {
        for (LambdaBlueprint lambdaBlueprint : BLUEPRINTS_CONFIG.getBlueprints().values()) {
            if (lambdaBlueprint.getDisplayName().equals(displayName)) {
                return lambdaBlueprint;
            }
        }
        return null;
    }
}
