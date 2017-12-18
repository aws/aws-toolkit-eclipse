/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newSashForm;
import static com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel.P_USE_BLUEPRINT;
import static com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel.P_USE_SERVERLESS_TEMPLATE_FILE;

import java.util.Iterator;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.core.ui.ImportFileComposite;
import com.amazonaws.eclipse.core.ui.MavenConfigurationComposite;
import com.amazonaws.eclipse.core.ui.ProjectNameComposite;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.widget.RadioButtonComplex;
import com.amazonaws.eclipse.lambda.blueprint.BlueprintsProvider;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.serverless.ui.FormBrowser;
import com.amazonaws.eclipse.lambda.serverless.validator.ServerlessTemplateFilePathValidator;

public class NewServerlessProjectWizardPageOne extends WizardPage {

    private static final String PAGE_NAME = NewServerlessProjectWizardPageOne.class.getName();

    private final NewServerlessProjectDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    //Composite modules in this page.
    private ProjectNameComposite projectNameComposite;
    private MavenConfigurationComposite mavenConfigurationComposite;
    private ImportFileComposite importFileComposite;

    private TableViewer blueprintSelectionViewer;
    private FormBrowser descriptionBrowser;

    private RadioButtonComplex useBlueprintButtonComplex;
    private RadioButtonComplex useServerlessTemplateButtonComplex;

    public NewServerlessProjectWizardPageOne(NewServerlessProjectDataModel dataModel) {
        super(PAGE_NAME);
        setTitle("Create a new Serverless Java project");
        setDescription("You can create a new Serverless Java project either from a Blueprint "
                + "or an existing Serverless template file.");

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
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
        createUseBlueprintButtonSection(composite);
        createBlueprintsSelectionSection(composite);
        createUseServerlessTemplateButtonSection(composite);
        createServerlessTemplateImportSection(composite);

        initialize();
        setControl(composite);
    }

    protected void createProjectNameComposite(Composite composite) {
        projectNameComposite = new ProjectNameComposite(
                composite, bindingContext, dataModel.getProjectNameDataModel());
    }

    protected void createMavenConfigurationComposite(Composite composite) {
        Group group = WizardWidgetFactory.newGroup(composite, "Maven Configuration");
        mavenConfigurationComposite = new MavenConfigurationComposite(
                group, bindingContext, dataModel.getMavenConfigurationDataModel());
    }

    private void initialize() {
        onBlueprintSelectionViewerSelectionChange();
        onSelectBlueprintButtonSelect();
    }

    private void createUseBlueprintButtonSection(Composite parent) {
        useBlueprintButtonComplex = RadioButtonComplex.builder()
                .composite(parent)
                .dataBindingContext(bindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_USE_BLUEPRINT))
                .labelValue("Select a Blueprint:")
                .defaultValue(dataModel.isUseBlueprint())
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onSelectBlueprintButtonSelect();
                    }
                })
                .build();
    }

    private void createUseServerlessTemplateButtonSection(Composite parent) {
        useServerlessTemplateButtonComplex = RadioButtonComplex.builder()
                .composite(parent)
                .dataBindingContext(bindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_USE_SERVERLESS_TEMPLATE_FILE))
                .labelValue("Select a Serverless template file:")
                .defaultValue(dataModel.isUseServerlessTemplateFile())
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onSelectServerlessTemplateButtonSelect();
                    }
                })
                .build();
    }

    private void createBlueprintsSelectionSection(Composite parent) {
        SashForm sashForm = newSashForm(parent, 1, 2);
        blueprintSelectionViewer = new TableViewer(sashForm, SWT.BORDER);
        blueprintSelectionViewer.setContentProvider(new ArrayContentProvider());
        blueprintSelectionViewer.setInput(BlueprintsProvider.getServerlessBlueprintDisplayNames());
        ISelection selection = new StructuredSelection(dataModel.getBlueprintName());
        blueprintSelectionViewer.setSelection(selection, true);
        blueprintSelectionViewer.addSelectionChangedListener(new ISelectionChangedListener(){
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                onBlueprintSelectionViewerSelectionChange();
            }
        });

        descriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
        descriptionBrowser.setText("");
        descriptionBrowser.createControl(sashForm);
        Control c = descriptionBrowser.getControl();
        GridData gd = new GridData(GridData.FILL_BOTH);
        c.setLayoutData(gd);
    }

    private void setBlueprintSelectionSectionEnabled(boolean enabled) {
        blueprintSelectionViewer.getTable().setEnabled(enabled);
        descriptionBrowser.getControl().setEnabled(enabled);
    }

    private void onSelectBlueprintButtonSelect() {
        setBlueprintSelectionSectionEnabled(true);
        setServerlessTemplateImportSectionEnabled(false);
        runValidators();
    }

    private void onSelectServerlessTemplateButtonSelect() {
        setBlueprintSelectionSectionEnabled(false);
        setServerlessTemplateImportSectionEnabled(true);
        runValidators();
    }

    private void onBlueprintSelectionViewerSelectionChange() {
        IStructuredSelection selection = (IStructuredSelection) blueprintSelectionViewer.getSelection();
        String blueprint = (String)selection.getFirstElement();
        dataModel.setBlueprintName(blueprint);
        descriptionBrowser.setText(dataModel.getSelectedBlueprint().getDescription());
    }

    private void createServerlessTemplateImportSection(Composite parent) {
        importFileComposite = ImportFileComposite.builder(parent, bindingContext, dataModel.getImportFileDataModel())
                .filePathValidator(new ServerlessTemplateFilePathValidator())
                .build();
    }

    private void setServerlessTemplateImportSectionEnabled(boolean enabled) {
        importFileComposite.setEnabled(enabled);
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

    private void runValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }
}
