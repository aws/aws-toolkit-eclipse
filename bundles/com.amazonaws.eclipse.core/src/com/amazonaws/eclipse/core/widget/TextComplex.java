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
package com.amazonaws.eclipse.core.widget;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import static com.amazonaws.util.ValidationUtils.assertNotNull;
import static com.amazonaws.util.ValidationUtils.assertStringNotEmpty;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;

/**
 * A complex Text widget including a Label, DataBinding, Validator and Decoration.
 */
public class TextComplex {

    private final Text text;
    private ControlDecoration controlDecoration;
    private ISWTObservableValue swtObservableValue;
    private final IObservableValue enabler = new WritableValue();

    private TextComplex(
            Composite composite,
            DataBindingContext dataBindingContext,
            IObservableValue pojoObservableValue,
            List<IValidator> validators,
            ModifyListener modifyListener,
            boolean createLabel,
            String labelValue,
            String defaultValue,
            int textColSpan,
            int labelColSpan) {

        if (createLabel) newLabel(composite, labelValue, labelColSpan);
        text = newText(composite, "", textColSpan);
        controlDecoration = newControlDecoration(text, "");

        swtObservableValue = SWTObservables.observeText(text, SWT.Modify);
        dataBindingContext.bindValue(swtObservableValue, pojoObservableValue);

        enabler.setValue(true);
        ChainValidator<String> handlerPackageValidator = new ChainValidator<String>(
                swtObservableValue, enabler, validators);
        dataBindingContext.addValidationStatusProvider(handlerPackageValidator);
        new DecorationChangeListener(controlDecoration,
                handlerPackageValidator.getValidationStatus());
        if (modifyListener != null) text.addModifyListener(modifyListener);
        swtObservableValue.setValue(defaultValue);
    }

    // Whether enable widget or not.
    public void setEnabled(boolean enabled) {
        text.setEnabled(enabled);
        enabler.setValue(enabled);
    }

    public void setText(String textValue) {
        text.setText(textValue);
    }

    public Text getText() {
        return text;
    }

    public static TextComplexBuilder builder() {
        return new TextComplexBuilder();
    }

    public static class TextComplexBuilder {

        private Composite composite;
        private DataBindingContext dataBindingContext;
        private IObservableValue pojoObservableValue;
        private List<IValidator> validators = new ArrayList<IValidator>();
        private String labelValue;

        private ModifyListener modifyListener;
        private boolean createLabel = true;
        private String defaultValue = "";
        private int textColSpan = 1;
        private int labelColSpan = 1;

        public TextComplex build() {
            validateParameters();

            return new TextComplex(
                    composite, dataBindingContext, pojoObservableValue, validators, modifyListener,
                    createLabel, labelValue, defaultValue, textColSpan, labelColSpan);
        }

        public TextComplexBuilder composite(Composite composite) {
            this.composite = composite;
            return this;
        }

        public TextComplexBuilder dataBindingContext(DataBindingContext dataBindingContext) {
            this.dataBindingContext = dataBindingContext;
            return this;
        }

        public TextComplexBuilder pojoObservableValue(IObservableValue pojoObservableValue) {
            this.pojoObservableValue = pojoObservableValue;
            return this;
        }

        @Deprecated
        public TextComplexBuilder validator(IValidator validator) {
            this.validators.add(validator);
            return this;
        }

        public TextComplexBuilder addValidator(IValidator validator) {
            this.validators.add(validator);
            return this;
        }

        public TextComplexBuilder addValidators(List<IValidator> validators) {
            this.validators.addAll(validators);
            return this;
        }

        public TextComplexBuilder modifyListener(ModifyListener modifyListener) {
            this.modifyListener = modifyListener;
            return this;
        }

        public TextComplexBuilder createLabel(boolean createLabel) {
            this.createLabel = createLabel;
            return this;
        }

        public TextComplexBuilder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        public TextComplexBuilder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public TextComplexBuilder textColSpan(int textColSpan) {
            this.textColSpan = textColSpan;
            return this;
        }

        public TextComplexBuilder labelColSpan(int labelColSpan) {
            this.labelColSpan = labelColSpan;
            return this;
        }

        private void validateParameters() {
            assertNotNull(composite, "Composite");
            assertNotNull(dataBindingContext, "DataBindingContext");
            assertNotNull(pojoObservableValue, "PojoObservableValue");
            if (createLabel) assertStringNotEmpty(labelValue, "LabelValue");
        }
    }

}
