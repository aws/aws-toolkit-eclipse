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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.eclipse.lambda.blueprint.BlueprintsProvider;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprint;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprintsConfig;
import com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData;

/**
 * Data model for Lambda function composite.
 */
public class LambdaFunctionDataModel {

    public static final String P_PACKAGE_NAME = "packageName";
    public static final String P_CLASS_NAME = "className";
    public static final String P_INPUT_TYPE = "inputType";

    private static final LambdaBlueprintsConfig BLUEPRINTS_CONFIG = BlueprintsProvider.provideLambdaBlueprints();

    private String packageName = "com.amazonaws.lambda.demo";
    private String className = "LambdaFunctionHandler";

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

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        String oldValue = this.getPackageName();
        this.packageName = packageName;
        this.pcs.firePropertyChange(P_PACKAGE_NAME, oldValue, packageName);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        String oldValue = this.getClassName();
        this.className = className;
        this.pcs.firePropertyChange(P_CLASS_NAME, oldValue, className);
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        String oldValue = this.getInputType();
        this.inputType = inputType;
        this.selectedBlueprint = getLambdaBlueprint(inputType);
        this.pcs.firePropertyChange(P_INPUT_TYPE, oldValue, inputType);
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
