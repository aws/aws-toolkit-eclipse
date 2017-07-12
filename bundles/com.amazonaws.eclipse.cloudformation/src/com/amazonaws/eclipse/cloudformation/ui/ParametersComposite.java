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
package com.amazonaws.eclipse.cloudformation.ui;

import static com.amazonaws.eclipse.cloudformation.validators.TemplateParameterValidators.newMaxLengthValidator;
import static com.amazonaws.eclipse.cloudformation.validators.TemplateParameterValidators.newMaxValueValidator;
import static com.amazonaws.eclipse.cloudformation.validators.TemplateParameterValidators.newMinLengthValidator;
import static com.amazonaws.eclipse.cloudformation.validators.TemplateParameterValidators.newMinValueValidator;
import static com.amazonaws.eclipse.cloudformation.validators.TemplateParameterValidators.newPatternValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.model.ParametersDataModel;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.services.cloudformation.model.TemplateParameter;

/**
 * A composite dynamically built from a set of AWS CloudFormation template parameters.
 */
public class ParametersComposite extends Composite {

    private static final String ALLOWED_VALUES = "AllowedValues";
    private static final String MAX_LENGTH = "MaxLength";
    private static final String MIN_LENGTH = "MinLength";
    private static final String MAX_VALUE = "MaxValue";
    private static final String MIN_VALUE = "MinValue";
    private static final String CONSTRAINT_DESCRIPTION = "ConstraintDescription";
    private static final String ALLOWED_PATTERN = "AllowedPattern";

    private final ParametersDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final Map<?, ?> parameterMap;

    public ParametersComposite(Composite parent, ParametersDataModel dataModel, DataBindingContext bindingContext) {
        super(parent, SWT.NONE);
        this.dataModel = dataModel;
        this.parameterMap = (Map<?, ?>) dataModel.getTemplate().get("Parameters");
        this.bindingContext = bindingContext;
        setLayout(new GridLayout(2, false));
        setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createControls();
    }

    private void createControls() {
        if (dataModel.getTemplateParameters() == null || dataModel.getTemplateParameters().isEmpty()) {
            Label label = new Label(this, SWT.NONE);
            label.setText("Selected template has no parameters");
            label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
        }

        for (TemplateParameter param : dataModel.getTemplateParameters()) {
            createParameterSection(param);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createParameterSection(TemplateParameter param) {
        // Unfortunately, we have to manually adjust for field decorations
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        int fieldDecorationWidth = fieldDecoration.getImage().getBounds().width;

        Control paramControl = null;
        ISWTObservableValue observeParameter = null;
        ChainValidator<String> validationStatusProvider = null;
        if ( parameterMap.containsKey(param.getParameterKey()) ) {

            Label label = new Label(this, SWT.None);
            label.setText(param.getParameterKey());

            Map paramMap = (Map) parameterMap.get(param.getParameterKey());

            // Update the default value in the model.
            if (dataModel.getParameterValues().get(param.getParameterKey()) == null) {
                dataModel.getParameterValues().put(param.getParameterKey(), param.getDefaultValue());
            }

            // If the template enumerates allowed values, present them as a
            // combo drop down
            if ( paramMap.containsKey(ALLOWED_VALUES) ) {
                Combo combo = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
                Collection<String> allowedValues = (Collection<String>) paramMap.get(ALLOWED_VALUES);
                GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(combo);
                combo.setItems(allowedValues.toArray(new String[allowedValues.size()]));
                observeParameter = SWTObservables.observeSelection(combo);
                paramControl = combo;
            } else {
                // Otherwise, just use a text field with validation constraints
                Text text = new Text(this, SWT.BORDER);
                GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(text);
                observeParameter = SWTObservables.observeText(text, SWT.Modify);
                paramControl = text;

                // Add validators for the constraints listed in the template
                List<IValidator> validators = new ArrayList<>();

                if ( paramMap.containsKey(ALLOWED_PATTERN) ) {
                    String pattern = (String) paramMap.get(ALLOWED_PATTERN);
                    Pattern p = Pattern.compile(pattern);
                    validators.add(newPatternValidator(p, param.getParameterKey() + ": "
                            + (String) paramMap.get(CONSTRAINT_DESCRIPTION)));
                }
                if ( paramMap.containsKey(MIN_LENGTH) ) {
                    validators.add(newMinLengthValidator(parseValueToInteger(paramMap.get(MIN_LENGTH)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MAX_LENGTH) ) {
                    validators.add(newMaxLengthValidator(parseValueToInteger(paramMap.get(MAX_LENGTH)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MIN_VALUE) ) {
                    validators.add(newMinValueValidator(parseValueToInteger(paramMap.get(MIN_VALUE)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MAX_VALUE) ) {
                    validators.add(newMaxValueValidator(parseValueToInteger(paramMap.get(MAX_VALUE)), param
                            .getParameterKey()));
                }

                if ( !validators.isEmpty() ) {
                    validationStatusProvider = new ChainValidator<>(observeParameter,
                            validators.toArray(new IValidator[validators.size()]));
                }
            }
        } else {
            CloudFormationPlugin.getDefault().logError("No parameter map object found for " + param.getParameterKey(),
                    null);
            return;
        }

        if (param.getDescription() != null) {
            Label description = new Label(this, SWT.WRAP);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).indent(0, -8).applyTo(description);
            description.setText(param.getDescription());
            description.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        }

        bindingContext.bindValue(observeParameter, Observables.observeMapEntry(
                dataModel.getParameterValues(), param.getParameterKey(), String.class));

        if ( validationStatusProvider != null ) {
            bindingContext.addValidationStatusProvider(validationStatusProvider);
            ControlDecoration decoration = new ControlDecoration(paramControl, SWT.TOP | SWT.LEFT);
            decoration.setDescriptionText("Invalid value");
            decoration.setImage(fieldDecoration.getImage());
            new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());
        }
    }

    private int parseValueToInteger(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        } else {
            return Integer.parseInt((String) value);
        }
    }
}
