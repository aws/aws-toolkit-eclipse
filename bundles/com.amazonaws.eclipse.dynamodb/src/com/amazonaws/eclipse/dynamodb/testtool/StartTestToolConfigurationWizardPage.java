/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.testtool;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.wizards.ErrorDecorator;
import com.amazonaws.eclipse.core.validator.IntegerRangeValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.preferences.TestToolPreferencePage;

/**
 * An (optional) wizard page that lets the user set up additional configuration
 * for the test tool instance they're about to launch. For now, that's just
 * the TCP port it will listen on.
 */
public class StartTestToolConfigurationWizardPage extends WizardPage {

    private final DataBindingContext context = new DataBindingContext();

    private final IObservableValue portValue = new WritableValue();
    private Text portInput;

    /**
     * Create a new instance.
     */
    public StartTestToolConfigurationWizardPage() {
        super("Configure the DynamoDB Local Test Tool");

        super.setMessage("Configure the DynamoDB Local Test Tool");
        super.setImageDescriptor(
            AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor("dynamodb-service")
        );
    }

    /**
     * @return the chosen port to listen on
     */
    public int getPort() {
        return Integer.parseInt((String) portValue.getValue());
    }

    /**
     * Create the wizard page's controls.
     *
     * @param parent    the parent composite to hang the controls on
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout(2, false));

        Label label = new Label(composite, SWT.NONE);
        label.setText("Port: ");

        portInput = new Text(composite, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(portInput);

        // TODO: Would be nice to drive this off of the manifest.xml for the
        // chosen version, in case future versions of the test tool support
        // additional options.

        bindInputs();

        IPreferenceStore preferences =
            DynamoDBPlugin.getDefault().getPreferenceStore();

        if (preferences.contains(TestToolPreferencePage
                                     .DEFAULT_PORT_PREFERENCE_NAME)) {

            portValue.setValue(Integer.toString(
                preferences.getInt(TestToolPreferencePage
                                       .DEFAULT_PORT_PREFERENCE_NAME)
            ));
            context.updateTargets();
        }

        setControl(composite);
    }

    /**
     * Bind the UI to our internal model and wire up a validator to make sure
     * the user has input valid value(s).
     */
    private void bindInputs() {
        IObservableValue observable =
            SWTObservables.observeText(portInput, SWT.Modify);
        context.bindValue(observable, portValue);
        context.addValidationStatusProvider(
            new ChainValidator<String>(
                observable,
                new PortValidator()
            )
        );

        final AggregateValidationStatus aggregator =
            new AggregateValidationStatus(context,
                AggregateValidationStatus.MAX_SEVERITY);

        aggregator.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(final ChangeEvent event) {
                Object value = aggregator.getValue();
                if (!(value instanceof IStatus)) {
                    return;
                }

                IStatus status = (IStatus) value;

                setPageComplete(status.isOK());
            }
        });

        ErrorDecorator.bind(portInput, aggregator);
    }

    /**
     * A validator that ensures the input is a valid port number.
     * @deprecated for {@link IntegerRangeValidator}
     */
    private static class PortValidator implements IValidator {
        @Override
        public IStatus validate(final Object value) {
            if (!(value instanceof String)) {
                return ValidationStatus.error("No port specified");
            }

            int port;
            try {
                port = Integer.parseInt((String) value);
            } catch (NumberFormatException exception) {
                return ValidationStatus.error(
                    "Port must be an integer between 1 and 35535");
            }

            if (port <= 0 || port > 0xFFFF) {
                return ValidationStatus.error(
                    "Port must be an integer between 1 and 35535");
            }

            return ValidationStatus.ok();
        }
    }
}
