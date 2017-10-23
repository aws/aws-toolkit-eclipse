/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_SHOW_README_FILE;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.ui.MavenConfigurationComposite;
import com.amazonaws.eclipse.core.ui.ProjectNameComposite;
import com.amazonaws.eclipse.core.widget.CheckboxComplex;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.LambdaFunctionComposite;

public class NewLambdaJavaFunctionProjectWizardPageOne extends WizardPage {

    private static final String PAGE_NAME = NewLambdaJavaFunctionProjectWizardPageOne.class.getName();

    private final LambdaFunctionWizardDataModel dataModel;
    private final DataBindingContext dataBindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    // Composite modules in this page.
    private ProjectNameComposite projectNameComposite;
    private MavenConfigurationComposite mavenConfigurationComposite;
    private LambdaFunctionComposite lambdaFunctionComposite;
    private CheckboxComplex showReadmeFileComplex;

    private ModifyListener mavenModifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent arg0) {
            onMavenConfigurationChange();
        }
    };

    public NewLambdaJavaFunctionProjectWizardPageOne(LambdaFunctionWizardDataModel dataModel) {
        super(PAGE_NAME);
        setTitle("Create a new AWS Lambda Java project");
        setDescription("Create a new AWS Lambda Java project in the workspace");

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
    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createProjectNameComposite(composite);
        createMavenConfigurationComposite(composite);
        createLambdaFunctionComposite(composite);
        createShowReadmeFileCheckBox(composite);

        setControl(composite);
    }

    @Override
    public void dispose() {
        lambdaFunctionComposite.dispose();
        super.dispose();
    }

    protected void createProjectNameComposite(Composite composite) {
        projectNameComposite = new ProjectNameComposite(
                composite, dataBindingContext, dataModel.getProjectNameDataModel());
    }

    protected void createMavenConfigurationComposite(Composite composite) {
        Group group = newGroup(composite, "Maven configuration");
        mavenConfigurationComposite = new MavenConfigurationComposite(
                group, dataBindingContext, dataModel.getMavenConfigurationDataModel());
    }

    protected void createLambdaFunctionComposite(Composite composite) {
        lambdaFunctionComposite = new LambdaFunctionComposite(
                composite, dataModel.getLambdaFunctionDataModel(), dataBindingContext);
        lambdaFunctionComposite.createClassNameControl();
        lambdaFunctionComposite.createInputTypeControl();
        lambdaFunctionComposite.createSeparator();
        lambdaFunctionComposite.createHandlerSourcePreview();
        lambdaFunctionComposite.initialize();
    }

    public void createShowReadmeFileCheckBox(Composite composite) {
        showReadmeFileComplex = CheckboxComplex.builder()
                .composite(composite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_SHOW_README_FILE))
                .labelValue("Show README guide after creating the project")
                .defaultValue(dataModel.isShowReadmeFile())
                .build();
    }

    private void populateValidationStatus() {

        IStatus status = getValidationStatus();
        if (status == null) return;

        if (status.getSeverity() == IStatus.OK) {
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

    private void onMavenConfigurationChange() {
        if (lambdaFunctionComposite != null) {
            String groupId = dataModel.getMavenConfigurationDataModel().getGroupId();
            String artifactId = dataModel.getMavenConfigurationDataModel().getArtifactId();
            lambdaFunctionComposite.getPackageNameComplex().setText(MavenFactory.assumePackageName(groupId, artifactId));
        }
    }
}