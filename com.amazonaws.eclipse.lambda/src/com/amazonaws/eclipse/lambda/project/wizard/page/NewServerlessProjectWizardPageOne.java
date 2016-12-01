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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newPushButton;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newSashForm;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import static com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel.P_SERVERLESS_FILE_PATH;
import static com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel.P_PACKAGE_PREFIX;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.lambda.project.classpath.LambdaRuntimeClasspathContainer;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryManager;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.validator.ValidPackageNameValidator;
import com.amazonaws.eclipse.lambda.serverless.blueprint.BlueprintProvider;
import com.amazonaws.eclipse.lambda.serverless.ui.FormBrowser;
import com.amazonaws.eclipse.lambda.serverless.validator.ServerlessTemplateFilePathValidator;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;

public class NewServerlessProjectWizardPageOne extends NewJavaProjectWizardPageOne {

    private static final String DEFAULT_PACKAGE_NAME = "com.serverless.demo";

    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;
    private ChainValidator<String> serverlessTemplateValidator;
    private ControlDecoration serverlessTemplateFilePathTextDecoration;
    private final NewServerlessProjectDataModel dataModel;

    private Text serverlessTemplateFilePathText;
    private Button browseButton;
    private Text packagePrefixText;

    private TableViewer blueprintSelectionViewer;
    private FormBrowser descriptionBrowser;

    private Button selectBlueprintButton;
    private Button selectServerlessTemplateButton;
    private ISWTObservableValue selectServerlessTemplateButtonObservable;

    private boolean isProjectNameValid;

    public NewServerlessProjectWizardPageOne(NewServerlessProjectDataModel dataModel) {
        setTitle("Create a new Serverless Java project");
        setDescription("You can create a new Serverless Java project either from a Blueprint "
                + "or an existing Serverless template file.");

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    public void createControl(final Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Reuse the project name control of the system Java project wizard
        Control nameControl = createNameControl(composite);
        nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createPackagePrefixTextSection(composite);
        createSelectBlueprintButtonSection(composite);
        createBlueprintsSelectionSection(composite);
        createSelectServerlessTemplateButtonSection(composite);
        createServerlessTemplateImportSection(composite);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            public void handleChange(ChangeEvent arg0) {
                populateHandlerValidationStatus();
            }
        });

        initialize();
        setControl(composite);
    }

    private void initialize() {
        blueprintSelectionViewer.getTable().select(0);
        onBlueprintSelectionViewerSelectionChange();
        selectBlueprintButton.setSelection(true);
        onSelectBlueprintButtonSelect();
    }

    private void createSelectBlueprintButtonSection(Composite parent) {
        selectBlueprintButton = new Button(parent, SWT.RADIO);
        selectBlueprintButton.setText("Select a Blueprint:");
        selectBlueprintButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                onSelectBlueprintButtonSelect();
            }
        });
    }

    private void createSelectServerlessTemplateButtonSection(Composite parent) {
        selectServerlessTemplateButton = new Button(parent, SWT.RADIO);
        selectServerlessTemplateButton.setText("Select a Serverless template file:");
        selectServerlessTemplateButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                onSelectServerlessTemplateButtonSelect();
            }
        });
        selectServerlessTemplateButtonObservable = SWTObservables.observeSelection(selectServerlessTemplateButton);

    }

    private void createBlueprintsSelectionSection(Composite parent) {
        SashForm sashForm = newSashForm(parent, 1, 2);
        blueprintSelectionViewer = new TableViewer(sashForm, SWT.BORDER);
        blueprintSelectionViewer.setContentProvider(new ArrayContentProvider());

        blueprintSelectionViewer.setInput(BlueprintProvider.getInstance().getBlueprintNames());
        blueprintSelectionViewer.addSelectionChangedListener(new ISelectionChangedListener(){
            public void selectionChanged(SelectionChangedEvent event) {
                onBlueprintSelectionViewerSelectionChange();
            }
        });
        createDescriptionIn(sashForm);
    }

    private void setBlueprintSelectionSectionEnabled(boolean enabled) {
        blueprintSelectionViewer.getTable().setEnabled(enabled);
        descriptionBrowser.getControl().setEnabled(enabled);
    }

    public void createDescriptionIn(Composite composite) {
        descriptionBrowser = new FormBrowser(SWT.BORDER | SWT.V_SCROLL);
        descriptionBrowser.setText("");
        descriptionBrowser.createControl(composite);
        Control c = descriptionBrowser.getControl();
        GridData gd = new GridData(GridData.FILL_BOTH);
        c.setLayoutData(gd);
    }

    private void onSelectBlueprintButtonSelect() {
        setBlueprintSelectionSectionEnabled(true);
        setServerlessTemplateImportSectionEnabled(false);
        dataModel.setUseBlueprint(true);
        runValidators();
    }

    private void onSelectServerlessTemplateButtonSelect() {
        setBlueprintSelectionSectionEnabled(false);
        setServerlessTemplateImportSectionEnabled(true);
        dataModel.setUseBlueprint(false);
        runValidators();
    }

    private void onBlueprintSelectionViewerSelectionChange() {
        IStructuredSelection selection = (IStructuredSelection) blueprintSelectionViewer.getSelection();
        String blueprint = (String)selection.getFirstElement();
        Map<String, String> descriptions = BlueprintProvider.getInstance().getBlueprintDescriptions();
        descriptionBrowser.setText(descriptions.get(blueprint));
        dataModel.setBlueprintName(blueprint);
    }

    private void createServerlessTemplateImportSection(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        group.setLayoutData(gridData);
        group.setLayout(new GridLayout(3, false));
        newLabel(group, "Import:");
        serverlessTemplateFilePathText = newText(group, "");
        serverlessTemplateFilePathTextDecoration =
                WizardWidgetFactory.newControlDecoration(serverlessTemplateFilePathText, "");

        ISWTObservableValue serverlessTemplateFilePathTextObservable =
                SWTObservables.observeText(serverlessTemplateFilePathText, SWT.Modify);
        bindingContext.bindValue(serverlessTemplateFilePathTextObservable,
                PojoObservables.observeValue(dataModel, P_SERVERLESS_FILE_PATH));
        serverlessTemplateValidator = new ChainValidator<String>(
                serverlessTemplateFilePathTextObservable,
                selectServerlessTemplateButtonObservable,
                new ServerlessTemplateFilePathValidator());
        bindingContext.addValidationStatusProvider(serverlessTemplateValidator);

        new DecorationChangeListener(serverlessTemplateFilePathTextDecoration,
                serverlessTemplateValidator.getValidationStatus());

        browseButton = newPushButton(group, "Browse");
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
                String path = dialog.open();
                if (path != null) serverlessTemplateFilePathText.setText(path);
            }
        });
    }

    private void setServerlessTemplateImportSectionEnabled(boolean enabled) {
        serverlessTemplateFilePathText.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }

    private void createPackagePrefixTextSection(Composite parent) {
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        group.setLayout(new GridLayout(2, false));
        newLabel(group, "Package namespace:");
        packagePrefixText = newText(group, "");
        ISWTObservableValue handlerPackageTextObservable = SWTObservables.observeText(packagePrefixText, SWT.Modify);
        bindingContext.bindValue(handlerPackageTextObservable,
                PojoObservables.observeValue(dataModel, P_PACKAGE_PREFIX));
        handlerPackageTextObservable.setValue(DEFAULT_PACKAGE_NAME);

        ControlDecoration handlerPackageTextDecoration = newControlDecoration(packagePrefixText, "");

        // bind validation of package name
        ChainValidator<String> handlerPackageValidator = new ChainValidator<String>(
                handlerPackageTextObservable,
                new ValidPackageNameValidator("Please provide a valid package name for the handler class"));
        bindingContext.addValidationStatusProvider(handlerPackageValidator);
        new DecorationChangeListener(handlerPackageTextDecoration,
                handlerPackageValidator.getValidationStatus());
    }

    /**
     * @return returns the default class path entries, which includes all the
     *         default JRE entries plus the Lambda runtime API.
     *
     * TODO The project structure should eventually be Maven.
     */
    @Override
    public IClasspathEntry[] getDefaultClasspathEntries() {

        IClasspathEntry[] classpath = super.getDefaultClasspathEntries();

        classpath = addJunitLibrary(classpath);
        classpath = addLambdaRuntimeLibrary(classpath);
        if (dataModel.requireSdkDependency()) {
            classpath = addJavaSdkLibrary(classpath);
        }

        return classpath;
    }

    private IClasspathEntry[] addLambdaRuntimeLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new LambdaRuntimeClasspathContainer(
                        LambdaRuntimeLibraryManager.getInstance().getLatestVersion()
                        ).getPath());

        return augmentedClasspath;
    }

    private IClasspathEntry[] addJavaSdkLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new AwsClasspathContainer(
                        JavaSdkManager.getInstance().getDefaultSdkInstall()
                        ).getPath());

        return augmentedClasspath;
    }

    private IClasspathEntry[] addJunitLibrary(IClasspathEntry[] classpath) {
        IClasspathEntry[] augmentedClasspath = new IClasspathEntry[classpath.length + 1];
        System.arraycopy(classpath, 0, augmentedClasspath, 0, classpath.length);

        final String JUNIT_CONTAINER_ID= "org.eclipse.jdt.junit.JUNIT_CONTAINER";
        augmentedClasspath[classpath.length] = JavaCore.newContainerEntry(
                new Path(JUNIT_CONTAINER_ID).append("4"));

        return augmentedClasspath;
    }

    /**
     * A very hacky way of combining the project name validation with our custom
     * validation logic.
     */
    @Override
    public void setPageComplete(boolean pageComplete) {
        isProjectNameValid = pageComplete;
        if (!pageComplete) {
            super.setPageComplete(pageComplete);
        } else {
            populateHandlerValidationStatus();
        }
    }

    @Override
    public void setMessage(String newMessage, int newType) {
        super.setMessage(newMessage, newType);
        populateHandlerValidationStatus();
    }

    private void populateHandlerValidationStatus() {
        if (aggregateValidationStatus == null) {
            return;
        }

        Object value = aggregateValidationStatus.getValue();
        if (! (value instanceof IStatus)) return;
        IStatus handlerInfoStatus = (IStatus) value;

        boolean isHandlerInfoValid = (handlerInfoStatus.getSeverity() == IStatus.OK);

        if (isProjectNameValid && isHandlerInfoValid) {
            // always call super methods when handling our custom
            // validation status
            setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            if (!isProjectNameValid) {
                setErrorMessage("Enter a valid project name");
            } else {
                setErrorMessage(handlerInfoStatus.getMessage());
            }
            super.setPageComplete(false);
        }
    }

    private void runValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

}
