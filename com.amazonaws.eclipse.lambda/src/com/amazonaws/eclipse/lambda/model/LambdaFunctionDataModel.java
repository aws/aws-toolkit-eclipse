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

import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType.STREAM_REQUEST_HANDLER;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.eclipse.lambda.project.template.data.HandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType;
import com.amazonaws.eclipse.lambda.project.wizard.model.PredefinedHandlerInputType;

/**
 * Data model for Lambda function composite.
 */
public class LambdaFunctionDataModel {

    public static final String P_PACKAGE_NAME = "packageName";
    public static final String P_CLASS_NAME = "className";
    public static final String P_TYPE = "type";
    public static final String P_INPUT_TYPE = "inputType";
    public static final String P_INPUT_NAME = "inputName";
    public static final String P_OUTPUT_NAME = "outputName";

    private String packageName = "com.amazonaws.lambda.demo";
    private String className = "LambdaFunctionHandler";
    private String type = LambdaHandlerType.REQUEST_HANDLER.getName();
    private String inputType = PredefinedHandlerInputType.S3_EVENT.getName();
    private String inputName = "Object";
    private String outputName = "Object";

    public HandlerClassTemplateData collectHandlerTemplateData() {

        HandlerClassTemplateData data = new HandlerClassTemplateData();

        data.setPackageName(getPackageName());
        data.setHandlerClassName(getClassName());
        data.setOutputType(getOutputName());
        PredefinedHandlerInputType type = getInputTypeEnum();

        if (type == null || type == PredefinedHandlerInputType.CUSTOM) {
            data.setInputType(getInputName());
        } else {
            data.addAdditionalImport(type.getFqcn());
            data.setInputType(type.getClassName());
        }

        return data;
    }

    public HandlerTestClassTemplateData collectHandlerTestTemplateData() {

        HandlerTestClassTemplateData data = new HandlerTestClassTemplateData();

        data.setPackageName(getPackageName());
        data.setHandlerClassName(getClassName());
        data.setHandlerTestClassName(getClassName() + "Test");
        data.setOutputType(getOutputName());
        PredefinedHandlerInputType type = getInputTypeEnum();

        if (type == null || type == PredefinedHandlerInputType.CUSTOM) {
            data.setInputType(getInputName());
        } else {
            data.addAdditionalImport(type.getFqcn());
            data.setInputType(type.getClassName());
            data.setInputJsonFileName(type.getSampleInputJsonFile());
        }

        return data;
    }

    public StreamHandlerClassTemplateData collectStreamHandlerTemplateData() {

        StreamHandlerClassTemplateData data = new StreamHandlerClassTemplateData();

        data.setPackageName(getPackageName());
        data.setHandlerClassName(getClassName());

        return data;
    }

    public StreamHandlerTestClassTemplateData collectStreamHandlerTestTemplateData() {

        StreamHandlerTestClassTemplateData data = new StreamHandlerTestClassTemplateData();

        data.setPackageName(getPackageName());
        data.setHandlerClassName(getClassName());
        data.setHandlerTestClassName(getClassName() + "Test");

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        String oldValue = this.type;
        this.type = type;
        this.pcs.firePropertyChange(P_TYPE, oldValue, type);
    }

    public String getInputType() {
        return inputType;
    }

    // Get the underlying enum type for the input type.
    public PredefinedHandlerInputType getInputTypeEnum() {
        for (PredefinedHandlerInputType type : PredefinedHandlerInputType.list()) {
            if (type.getName().equals(inputType)) return type;
        }
        return null;
    }

    public void setInputType(String inputType) {
        String oldValue = this.getInputType();
        this.inputType = inputType;
        this.pcs.firePropertyChange(P_INPUT_TYPE, oldValue, inputType);
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        String oldValue = this.getInputName();
        this.inputName = inputName;
        this.pcs.firePropertyChange(P_INPUT_NAME, oldValue, inputName);
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        String oldValue = this.getOutputName();
        this.outputName = outputName;
        this.pcs.firePropertyChange(P_OUTPUT_NAME, oldValue, outputName);
    }

    public boolean isUseStreamHandler() {
        return STREAM_REQUEST_HANDLER.getName().equals(getType());
    }
}
