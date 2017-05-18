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
package com.amazonaws.eclipse.cloudformation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.observable.map.WritableMap;

import com.amazonaws.eclipse.cloudformation.ui.ParametersComposite;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.TemplateParameter;

/**
 * Data model for {@link ParametersComposite}.
 */
public class ParametersDataModel {
    // This is from {@link ValidateTemplateResult}, which are parameters in the local template file.
    private List<TemplateParameter> templateParameters;
    // This is from {@link Stack}, which are parameters from the existing Stack.
    private final List<Parameter> parameters = new ArrayList<>();
    // This is a list of parameters collected from the Wizard UI.
    private final WritableMap parameterValues = new WritableMap(String.class, String.class);
    private Map template;

    public List<TemplateParameter> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(List<TemplateParameter> templateParameters) {
        this.templateParameters = templateParameters;
    }

    public WritableMap getParameterValues() {
        return parameterValues;
    }

    public Map getTemplate() {
        return template;
    }

    public void setTemplate(Map template) {
        this.template = template;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }
}
