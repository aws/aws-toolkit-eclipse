/*
 * Copyright 2011-2013 Amazon Technologies, Inc.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A generic wizard page which is driven by a set of WizardPageInputs, all of
 * which must have valid inputs for the page to be complete.
 */
public class CompositeWizardPage extends WizardPage {

    private final Map<String, WizardPageInput> inputs;

    /**
     * Construct a new CompositeWizardPage.
     *
     * @param pageName the name of the page (where does this show up?)
     * @param title the title of the page
     * @param titleImage image to show in the page title bar
     */
    public CompositeWizardPage(final String pageName,
                               final String title,
                               final ImageDescriptor titleImage) {

        super(pageName, title, titleImage);
        this.inputs = new LinkedHashMap<>();
    }

    /**
     * Add a named input to the page. Inputs have a name which can be used to
     * retrieve their values using getValue() once the page is complete.
     */
    public void addInput(final String name, final WizardPageInput input) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        if (inputs.containsKey(name)) {
            throw new IllegalArgumentException(
                "this page already has an input named " + name
            );
        }

        inputs.put(name, input);
    }

    /**
     * Get the value entered for the given named input.
     *
     * @param inputName the name of the input to query
     * @return the value of the input
     */
    public Object getInputValue(final String inputName) {
        WizardPageInput input = inputs.get(inputName);
        if (input == null) {
            throw new IllegalArgumentException("No input named " + inputName);
        }
        return input.getValue();
    }

    /**
     * Create the control for the page. Creates a basic layout, then invokes
     * each previously-added input (in the order they were added) to add
     * itself to the grid.
     *
     * @param parent the parent composite to create this page within
     */
    @Override
    public void createControl(final Composite parent) {
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;

        Composite composite = new Composite(parent, SWT.None);
        composite.setLayout(layout);

        DataBindingContext context = new DataBindingContext();

        for (Map.Entry<String, WizardPageInput> entry : inputs.entrySet()) {
            entry.getValue().init(composite, context);
        }

        bindValidationStatus(context);

        super.setControl(composite);
    }

    /**
     * Bind the most severe validation error from any validation status
     * providers added to the binding context by our inputs to the UI
     * for the page, displaying an error message and marking the page as
     * incomplete if one of the input values is invalid.
     *
     * @param context the data binding context for all inputs
     */
    private void bindValidationStatus(final DataBindingContext context) {
        final AggregateValidationStatus status =
            new AggregateValidationStatus(
                context,
                AggregateValidationStatus.MAX_SEVERITY
            );

        status.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(final ChangeEvent event) {
                updateValidationStatus((IStatus) status.getValue());
            }
        });

        updateValidationStatus((IStatus) status.getValue());
    }

    /**
     * Update the message and completion flag for this page based on
     * the current validation status of the inputs.
     *
     * @param status the aggregated current validation status
     */
    private void updateValidationStatus(final IStatus status) {
        super.setMessage(status.getMessage(), status.getSeverity());
        super.setPageComplete(status.isOK());
    }
}
