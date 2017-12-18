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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;

/**
 * A complex Text widget including a Label, DataBinding, Validator and Decoration.
 */
public class TextComplex {
    private final Text text;

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
            int labelColSpan,
            String textMessage) {

        if (createLabel) {
            Label label = newLabel(composite, labelValue, labelColSpan);
            label.setToolTipText(textMessage);
        }
        text = newText(composite, defaultValue, textColSpan);
        text.setToolTipText(textMessage);
        text.setMessage(textMessage);

        ControlDecoration controlDecoration = newControlDecoration(text, "");
        ISWTObservableValue swtObservableValue = WidgetProperties.text(SWT.Modify).observe(text);
        Binding binding = dataBindingContext.bindValue(swtObservableValue, pojoObservableValue);
        enabler.setValue(true);
        ChainValidator<String> validatorChain = new ChainValidator<>(swtObservableValue, enabler, validators);
        dataBindingContext.addValidationStatusProvider(validatorChain);
        new DecorationChangeListener(controlDecoration, validatorChain.getValidationStatus());
        swtObservableValue.setValue(defaultValue);

        text.addDisposeListener(e -> {
            dataBindingContext.removeBinding(binding);
            dataBindingContext.removeValidationStatusProvider(validatorChain);
            validatorChain.dispose();
            controlDecoration.dispose();
            swtObservableValue.dispose();
        });

        if (modifyListener != null) {
            text.addModifyListener(modifyListener);
        }
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

    @NonNull
    public static TextComplexBuilder builder(
            @NonNull Composite parent,
            @NonNull DataBindingContext dataBindingContext,
            @NonNull IObservableValue pojoObservableValue) {
        return new TextComplexBuilder(parent, dataBindingContext, pojoObservableValue);
    }

    public static class TextComplexBuilder {
        private final Composite composite;
        private final DataBindingContext dataBindingContext;
        private final IObservableValue pojoObservableValue;
        private final List<IValidator> validators = new ArrayList<>();

        private String labelValue = "Label: ";
        private String textMessage = "";

        private ModifyListener modifyListener;
        private boolean createLabel = true;
        private String defaultValue = "";
        private int textColSpan = 1;
        private int labelColSpan = 1;

        private TextComplexBuilder(
                @NonNull Composite parent,
                @NonNull DataBindingContext dataBindingContext,
                @NonNull IObservableValue pojoObservableValue) {
            this.composite = parent;
            this.dataBindingContext = dataBindingContext;
            this.pojoObservableValue = pojoObservableValue;
        }

        public TextComplex build() {
            return new TextComplex(
                    composite, dataBindingContext, pojoObservableValue, validators, modifyListener,
                    createLabel, labelValue, defaultValue, textColSpan, labelColSpan, textMessage);
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
            this.defaultValue = Optional.<String>ofNullable(defaultValue).orElse("");
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

        public TextComplexBuilder textMessage(String textMessage) {
            this.textMessage = textMessage;
            return this;
        }
    }
}
