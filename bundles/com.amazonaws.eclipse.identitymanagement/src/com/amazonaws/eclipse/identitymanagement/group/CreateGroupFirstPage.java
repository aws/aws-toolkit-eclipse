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
package com.amazonaws.eclipse.identitymanagement.group;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.identitymanagement.databinding.DataBindingUtils;

public class CreateGroupFirstPage extends WizardPage {

    private Text  groupNameText;
    private IObservableValue groupName;
    private final DataBindingContext bindingContext = new DataBindingContext();

    private final static String OK_MESSAGE = "Specify a group name";

    protected CreateGroupFirstPage(CreateGroupWizard wizard) {
        super(OK_MESSAGE);
        setMessage(OK_MESSAGE);
        groupName = PojoObservables.observeValue(wizard.getDataModel(), "groupName");
    }

    @Override
    public void createControl(Composite parent) {
        final Composite comp = new Composite(parent, SWT.NONE);
        comp.setLayoutData(new GridData(GridData.FILL_BOTH));
        comp.setLayout(new GridLayout(1, false));

        new Label(comp, SWT.NONE).setText("Group Name:");

        groupNameText = new Text(comp, SWT.BORDER);
        groupNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        bindingContext.bindValue(SWTObservables.observeText(groupNameText, SWT.Modify), groupName);

        ChainValidator<String> groupNameValidationStatusProvider = new ChainValidator<>(groupName,
                 new NotEmptyValidator("Please provide a group name"));

         bindingContext.addValidationStatusProvider(groupNameValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(groupNameText, groupNameValidationStatusProvider);

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false)
                    return;

                IStatus status = (IStatus) value;
                if (status.isOK()) {
                    setErrorMessage(null);
                    setMessage(OK_MESSAGE, Status.OK);
                } else if (status.getSeverity() == Status.WARNING) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                }

                setPageComplete(status.isOK());
            }
        });

        setControl(comp);
        setPageComplete(false);
    }
}
