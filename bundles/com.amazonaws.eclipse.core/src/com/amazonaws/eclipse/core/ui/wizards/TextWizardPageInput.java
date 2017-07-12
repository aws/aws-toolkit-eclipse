/*
 * Copyright 2008-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.ui.swt.Colors;
import com.amazonaws.eclipse.core.ui.swt.CompositeBuilder;
import com.amazonaws.eclipse.core.ui.swt.LabelBuilder;
import com.amazonaws.eclipse.core.ui.swt.PostBuildHook;
import com.amazonaws.eclipse.core.ui.swt.TextBuilder;
import com.amazonaws.eclipse.core.ui.swt.WidgetUtils;

/**
 * A text input for a CompositeWizardPage. Generates UI for taking a single
 * text input, runs pluggable validation strategies whenever the value in the
 * text box changes, and generates UI decorations indicating whether the
 * value is valid or not.
 */
public class TextWizardPageInput implements WizardPageInput {

    private final String labelText;
    private final String descriptionText;
    private final IObservableValue observableValue;
    private final TwoPhaseValidator validator;

    /**
     * Construct a new TextWizardPageInput.
     *
     * @param labelText the text for the label describing the input field
     * @param syncValidator optional synchronous validation strategy
     * @param asyncValidator optional async validation strategy
     */
    public TextWizardPageInput(final String labelText,
                               final String descriptionText,
                               final InputValidator syncValidator,
                               final InputValidator asyncValidator) {

        if (labelText == null) {
            throw new IllegalArgumentException("labelText cannot be null");
        }

        this.labelText = labelText;
        this.descriptionText = descriptionText;

        this.observableValue = new WritableValue();
        this.validator = new TwoPhaseValidator(
            observableValue,
            syncValidator,
            asyncValidator
        );
    }

    /** {@inheritDoc} */
    @Override
    public void init(final Composite parent,
                     final DataBindingContext context) {

        createLabelColumn(parent);

        PostBuildHook<Text> bindInputHook = new PostBuildHook<Text>() {
            @Override
            public void run(final Text value) {

                context.bindValue(
                    observableValue,
                    SWTObservables.observeText(value, SWT.Modify)
                );
                context.addValidationStatusProvider(validator);

                ErrorDecorator.bind(value, validator.getValidationStatus());
            }
        };
        createInputColumn(parent, bindInputHook);
    }

    /** {@inheritDoc} */
    @Override
    public Object getValue() {
        return observableValue.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        // Nothing to do here.
    }

    /**
     * Create the label column, containing a basic description of each input
     * field. Indent the label down a couple pixels from the top of the row so
     * it lines up better with the center of the text box in the input column.
     *
     * @param parent the parent composite to add to
     */
    private void createLabelColumn(final Composite parent) {
        WidgetUtils.indentDown(
            new LabelBuilder(labelText),
            5    // pixels of indent
        )
        .build(parent);
    }

    /**
     * Create the input column, containing the input text box and the long
     * description text (if applicable) stacked on top of one another. Extend
     * this column to take up any space in the wizard page not claimed by
     * the label column.
     *
     *  @param parent the parent composite to add to
     *  @return the input text box that was created
     */
    private void createInputColumn(final Composite parent,
                                   final PostBuildHook<Text> inputHook) {

        CompositeBuilder column = WidgetUtils.column(
            new TextBuilder()
                .withFullHorizontalFill()
                .withPostBuildHook(inputHook)
        );

        if (descriptionText != null) {
            column.withChild(WidgetUtils.indentRight(
                new LabelBuilder(descriptionText, Colors.GRAY),
                5   // pixels of indent
            ));
        }

        column.build(parent);
    }
}
