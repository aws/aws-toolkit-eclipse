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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.ChainValidator;

/**
 * A wizard page that allows the user to pick an installed version of the
 * DynamoDBLocal test tool to be started, or to install a version of the test
 * tool if one is not yet installed.
 */
public class StartTestToolPickVersionWizardPage extends WizardPage {

    private final DataBindingContext context = new DataBindingContext();

    private final IObservableValue versionSelection = new WritableValue();
    private TestToolVersionTable versionTable;

    /**
     * Create the page.
     */
    public StartTestToolPickVersionWizardPage() {
        super("Choose a Version");

        super.setMessage("Choose a version of the DynamoDB Local Test Tool "
                         + "to start");
        super.setImageDescriptor(
            AwsToolkitCore.getDefault()
                .getImageRegistry()
                .getDescriptor("dynamodb-service")
        );
    }

    /**
     * @return  the currently selected version
     */
    public TestToolVersion getSelectedVersion() {
        return (TestToolVersion) versionSelection.getValue();
    }

    /**
     * Create the version table control.
     *
     * @param parent    the parent composite to attach the control to
     */
    @Override
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        composite.setLayout(new GridLayout());

        versionTable = new TestToolVersionTable(composite);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 400;
        versionTable.setLayoutData(data);

        // TODO: Worth adding some text here to explain to the user that they
        // must pick an installed version of the test tool, and that they may
        // install a version if needed? I'm hoping it's self-evident?

        bindInputs();

        context.updateModels();
        setControl(composite);
    }

    /**
     * Bind the UI to the internal model and set up a validator to ensure an
     * installed version of the tool is selected before this page is complete.
     */
    private void bindInputs() {
        IObservableValue observable = versionTable.getObservableSelection();
        context.bindValue(observable, versionSelection);
        context.addValidationStatusProvider(
            new ChainValidator<TestToolVersion>(
                observable,
                new TestToolVersionValidator()
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
    }

    /**
     * A validator that ensures an installed version of the test tool is
     * chosen.
     */
    private static class TestToolVersionValidator implements IValidator {
        @Override
        public IStatus validate(final Object value) {
            if (!(value instanceof TestToolVersion)) {
                return ValidationStatus.error("No version selected");
            }

            TestToolVersion version = (TestToolVersion) value;
            if (!version.isInstalled()) {
                return ValidationStatus.error("Version is not installed");
            }

            return ValidationStatus.ok();
        }
    }
}
