/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codecommit.pages;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.codecommit.widgets.GitCredentialsComposite;
import com.amazonaws.eclipse.core.model.GitCredentialsDataModel;

/**
 */
public class GitCredentialsConfigurationPage extends WizardPage {

    private final GitCredentialsDataModel dataModel;
    private GitCredentialsComposite gitCredentialsComposite;
    private final DataBindingContext dataBindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    public GitCredentialsConfigurationPage(GitCredentialsDataModel dataModel) {
        super(GitCredentialsConfigurationPage.class.getName());
        this.setTitle("Git Credential Configuration");
        this.dataModel = dataModel;
        this.dataBindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                populateValidationStatus();
            }
        });
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        createGitCredentialsComposite(composite);

        setControl(composite);
    }

    private void createGitCredentialsComposite(Composite composite) {
        Group gitCredentialsGroup = newGroup(composite, "Configure Git Credentials:");
        gitCredentialsGroup.setLayout(new GridLayout(1, false));
        this.gitCredentialsComposite = new GitCredentialsComposite(
                gitCredentialsGroup, dataBindingContext, dataModel);
    }

    private void populateValidationStatus() {
        IStatus status = getValidationStatus();

        if (status == null || status.getSeverity() == IStatus.OK) {
            this.setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            setErrorMessage(status.getMessage());
            super.setPageComplete(false);
        }
    }

    private IStatus getValidationStatus() {
        if (aggregateValidationStatus == null) return null;
        Object value = aggregateValidationStatus.getValue();
        if (!(value instanceof IStatus)) return null;
        return (IStatus)value;
    }
}
