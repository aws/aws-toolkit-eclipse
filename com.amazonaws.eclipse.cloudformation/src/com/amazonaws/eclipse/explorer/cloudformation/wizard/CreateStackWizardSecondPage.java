/*
 * Copyright 2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.services.cloudformation.model.TemplateParameter;

/**
 * The second page in the create wizard second page
 */
public class CreateStackWizardSecondPage extends WizardPage {

    private static final String ALLOWED_VALUES = "AllowedValues";
    private static final String MAX_LENGTH = "MaxLength";
    private static final String MIN_LENGTH = "MinLength";
    private static final String MAX_VALUE = "MaxValue";
    private static final String MIN_VALUE = "MinValue";
    private static final String CONSTRAINT_DESCRIPTION = "ConstraintDescription";
    private static final String ALLOWED_PATTERN = "AllowedPattern";

    CreateStackWizard wizard;
    private DataBindingContext bindingContext;

    private boolean complete = false;

    private static final String OK_MESSAGE = "Provide values for template parameters.";
    private Composite comp;
    private ScrolledComposite scrolledComp;

    protected CreateStackWizardSecondPage(CreateStackWizard wizard) {
        super("Fill in stack template parameters");
        setMessage(OK_MESSAGE);
        this.wizard = wizard;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
     * .Composite)
     */
    public void createControl(final Composite parent) {
        scrolledComp = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComp);

        comp = new Composite(scrolledComp, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(comp);
        scrolledComp.setContent(comp);

        scrolledComp.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                Rectangle r = scrolledComp.getClientArea();
                scrolledComp.setMinSize(comp.computeSize(r.width, SWT.DEFAULT));
            }
        });

        setControl(scrolledComp);
    }

    private void createContents() {
        for ( Control c : comp.getChildren() ) {
            c.dispose();
        }

        bindingContext = new DataBindingContext();

        if (wizard.getDataModel().getTemplateParameters().isEmpty()) {
            setComplete(true);
            Label label = new Label(comp, SWT.NONE);
            label.setText("Selected template has no parameters");
            label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 2, 1));
        }

        for (TemplateParameter param : wizard.getDataModel().getTemplateParameters()) {
            createParameterSection(comp, param);
        }

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
                AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                if ( status.isOK() ) {
                    setErrorMessage(null);
                    setMessage(OK_MESSAGE, Status.OK);
                } else if ( status.getSeverity() == Status.WARNING ) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if ( status.getSeverity() == Status.ERROR ) {
                    setErrorMessage(status.getMessage());
                }

                setComplete(status.isOK());
            }
        });

        comp.layout();
        Rectangle r = scrolledComp.getClientArea();
        scrolledComp.setMinSize(comp.computeSize(r.width, SWT.DEFAULT));
    }

    @Override
    public void setVisible(boolean visible) {
        if ( visible ) {
            createContents();
        }
        super.setVisible(true);
    }

    @Override
    public boolean isPageComplete() {
        return complete;
    }

    private void setComplete(boolean complete) {
        this.complete = complete;
        if ( getWizard().getContainer() != null )
            getWizard().getContainer().updateButtons();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void createParameterSection(Composite comp, TemplateParameter param) {
        Map parameterMap = (Map) wizard.getDataModel().getTemplate().get("Parameters");

        // Unfortunately, we have to manually adjust for field decorations
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        int fieldDecorationWidth = fieldDecoration.getImage().getBounds().width;

        Control paramControl = null;
        ISWTObservableValue observeParameter = null;
        ChainValidator<String> validationStatusProvider = null;
        if ( parameterMap.containsKey(param.getParameterKey()) ) {

            Label label = new Label(comp, SWT.None);
            label.setText(param.getParameterKey());

            Map paramMap = (Map) parameterMap.get(param.getParameterKey());
            // If the template enumerates allowed values, present them as a
            // combo drop down
            if ( paramMap.containsKey(ALLOWED_VALUES) ) {
                Combo combo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
                Collection<String> allowedValues = (Collection<String>) paramMap.get(ALLOWED_VALUES);
                GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(combo);
                combo.setItems(allowedValues.toArray(new String[allowedValues.size()]));
                observeParameter = SWTObservables.observeSelection(combo);
                int i = 0;
                for ( String value : allowedValues ) {
                    if ( value.equals(param.getDefaultValue()) ) {
                        break;
                    }
                    i++;
                }
                combo.select(i);
                paramControl = combo;
            } else {
                // Otherwise, just use a text field with validation constraints
                Text text = new Text(comp, SWT.BORDER);
                text.setText(param.getDefaultValue() == null ? "" : param.getDefaultValue());
                GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(text);
                observeParameter = SWTObservables.observeText(text, SWT.Modify);
                paramControl = text;

                // Add validators for the constraints listed in the template
                List<IValidator> validators = new ArrayList<IValidator>();
                validators.add(new NotEmptyValidator("Please enter a value for " + param.getParameterKey()));

                if ( paramMap.containsKey(ALLOWED_PATTERN) ) {
                    String pattern = (String) paramMap.get(ALLOWED_PATTERN);
                    Pattern p = Pattern.compile(pattern);
                    validators.add(new PatternValidator(p, param.getParameterKey() + ": "
                            + (String) paramMap.get(CONSTRAINT_DESCRIPTION)));
                }
                if ( paramMap.containsKey(MIN_LENGTH) ) {
                    validators.add(new MinLengthValidator(Integer.parseInt((String) paramMap.get(MIN_LENGTH)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MAX_LENGTH) ) {
                    validators.add(new MaxLengthValidator(Integer.parseInt((String) paramMap.get(MAX_LENGTH)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MIN_VALUE) ) {
                    validators.add(new MinValueValidator(Integer.parseInt((String) paramMap.get(MIN_VALUE)), param
                            .getParameterKey()));
                }
                if ( paramMap.containsKey(MAX_VALUE) ) {
                    validators.add(new MaxValueValidator(Integer.parseInt((String) paramMap.get(MAX_VALUE)), param
                            .getParameterKey()));
                }

                if ( !validators.isEmpty() ) {
                    validationStatusProvider = new ChainValidator<String>(observeParameter,
                            validators.toArray(new IValidator[validators.size()]));
                }
            }
        } else {
            AwsToolkitCore.getDefault().logException("No parameter map object found for " + param.getParameterKey(),
                    null);
            return;
        }

        if (param.getDescription() != null) {
            Label description = new Label(comp, SWT.WRAP);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).indent(0, -8).applyTo(description);
            description.setText(param.getDescription());
            description.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        }

        bindingContext.bindValue(observeParameter, Observables.observeMapEntry(wizard.getDataModel()
                .getParameterValues(), param.getParameterKey(), String.class));

        if ( validationStatusProvider != null ) {
            bindingContext.addValidationStatusProvider(validationStatusProvider);
            ControlDecoration decoration = new ControlDecoration(paramControl, SWT.TOP | SWT.LEFT);
            decoration.setDescriptionText("Invalid value");
            decoration.setImage(fieldDecoration.getImage());
            new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());
        }
    }

    private final class PatternValidator implements IValidator {

        private Pattern pattern;
        private String errorMessage;

        private PatternValidator(Pattern pattern, String errorMessage) {
            this.pattern = pattern;
            this.errorMessage = errorMessage;
        }

        public IStatus validate(Object value) {
            Matcher matcher = pattern.matcher((CharSequence) value);
            if ( matcher.matches() ) {
                return ValidationStatus.ok();
            } else {
                return ValidationStatus.error(errorMessage);
            }
        }
    }

    private final class MaxLengthValidator implements IValidator {

        private int maxLength;
        private String fieldName;

        private MaxLengthValidator(int maxLength, String fieldName) {
            super();
            this.maxLength = maxLength;
            this.fieldName = fieldName;
        }

        public IStatus validate(Object value) {
            if ( ((String) value).length() > maxLength ) {
                return ValidationStatus.error(fieldName + " must be at most " + maxLength + " characters long");
            } else {
                return ValidationStatus.ok();
            }
        }
    }

    private final class MinLengthValidator implements IValidator {

        private int minLength;
        private String fieldName;

        private MinLengthValidator(int minLength, String fieldName) {
            super();
            this.minLength = minLength;
            this.fieldName = fieldName;
        }

        public IStatus validate(Object value) {
            if ( ((String) value).length() < minLength ) {
                return ValidationStatus.error(fieldName + " must be at least " + minLength + " characters long");
            } else {
                return ValidationStatus.ok();
            }
        }
    }

    private final class MinValueValidator implements IValidator {

        private int minValue;
        private String fieldName;

        private MinValueValidator(int minValue, String fieldName) {
            super();
            this.minValue = minValue;
            this.fieldName = fieldName;
        }

        public IStatus validate(Object value) {
            String string = (String) value;
            try {
                if ( Integer.parseInt(string) < minValue ) {
                    return ValidationStatus.error(fieldName + " must be at least " + minValue);
                } else {
                    return ValidationStatus.ok();
                }
            } catch ( Exception e ) {
                return ValidationStatus.error(fieldName + " must be at least " + minValue);
            }
        }
    }

    private final class MaxValueValidator implements IValidator {

        private int maxValue;
        private String fieldName;

        private MaxValueValidator(int maxValue, String fieldName) {
            super();
            this.maxValue = maxValue;
            this.fieldName = fieldName;
        }

        public IStatus validate(Object value) {
            String string = (String) value;
            try {
                if ( Integer.parseInt(string) > maxValue ) {
                    return ValidationStatus.error(fieldName + " must be at most " + maxValue);
                } else {
                    return ValidationStatus.ok();
                }
            } catch ( Exception e ) {
                return ValidationStatus.error(fieldName + " must be at most " + maxValue);
            }
        }
    }

}
