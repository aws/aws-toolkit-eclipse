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
package com.amazonaws.eclipse.identitymanagement.role;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.identitymanagement.databinding.DataBindingUtils;


public class CreateRoleFirstPage extends WizardPage {

    private Text roleNameText;
    private IObservableValue roleName;
    private final static String OK_MESSAGE = "Specifiy a role name.";
    private final DataBindingContext bindingContext = new DataBindingContext();

    protected CreateRoleFirstPage(CreateRoleWizard wizard) {
        super(OK_MESSAGE);
        setMessage(OK_MESSAGE);
        roleName = PojoObservables.observeValue(wizard.getDataModel(), "roleName");
    }

    @Override
    public void createControl(Composite parent) {
        final Composite comp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        comp.setLayout(new GridLayout(1, false));
        new Label(comp, SWT.NONE).setText("Role Name:");;

        roleNameText = new Text(comp, SWT.BORDER);

        bindingContext.bindValue(SWTObservables.observeText(roleNameText, SWT.Modify), roleName);

        ChainValidator<String> roleNameValidationStatusProvider = new ChainValidator<>(roleName,
                 new NotEmptyValidator("Please provide a valid role name"));

         bindingContext.addValidationStatusProvider(roleNameValidationStatusProvider);
         DataBindingUtils.addStatusDecorator(roleNameText, roleNameValidationStatusProvider);



        GridDataFactory.fillDefaults().grab(true, false).applyTo(roleNameText);
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

        setPageComplete(false);
        setControl(comp);
    }
}
