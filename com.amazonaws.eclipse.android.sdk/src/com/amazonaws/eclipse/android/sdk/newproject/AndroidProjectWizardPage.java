/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.android.sdk.newproject;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.android.sdk.AndroidSDKPlugin;
import com.amazonaws.eclipse.android.sdk.AndroidSdkInstall;
import com.amazonaws.eclipse.android.sdk.AndroidSdkManager;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;

public class AndroidProjectWizardPage extends WizardPage {
    private static final class AndroidTargetValidator implements IValidator {
        public IStatus validate(Object value) {
            if (value instanceof IAndroidTarget) return Status.OK_STATUS;

            return new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Please select an Android platform version");
        }
    }

    private final static class PackageNameValidator implements IValidator {
        public IStatus validate(Object value) {
            if (value instanceof String) {
                String s = (String)value;
                if (s != null && s.length() > 0) return Status.OK_STATUS;
            }

            return new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Please enter a Java package name");
        }
    }

    private final static class ProjectNameValidator implements IValidator {
        public IStatus validate(Object value) {
            if (value instanceof String) {
                String s = (String)value;
                if (s != null && s.length() > 0) return Status.OK_STATUS;
            }

            return new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID, "Please enter a project name");
        }
    }

    private Text projectNameText;
    private Text packageNameText;

    protected DataBindingContext bindingContext = new DataBindingContext();

    private final NewAndroidProjectDataModel dataModel;
    private AggregateValidationStatus aggregateValidationStatus;
    private Composite androidTargetSelectorComposite;
    private SdkTargetSelector sdkTargetSelector;
    private Button sampleCodeButton;

    private boolean sdkInstalled = false;


    protected AndroidProjectWizardPage(NewAndroidProjectDataModel dataModel) {
        super("New AWS Android Project");
        this.dataModel = dataModel;
        setMessage("Enter a project name and select and Android target platform.");
    }

    public void createControl(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        setControl(composite);

        GridDataFactory factory = GridDataFactory.fillDefaults();
        factory.grab(true, false);

        new Label(composite, SWT.NONE).setText("Project Name:");
        projectNameText = new Text(composite, SWT.BORDER);
        factory.applyTo(projectNameText);

        new Label(composite, SWT.NONE).setText("Java Package Name:");
        packageNameText = new Text(composite, SWT.BORDER);
        factory.applyTo(packageNameText);

        sampleCodeButton = new Button(composite, SWT.CHECK);
        sampleCodeButton.setText("Start with sample application");
        sampleCodeButton.setSelection(true);
        factory.copy().span(2, 1).applyTo(packageNameText);

        androidTargetSelectorComposite = new Composite(composite, SWT.NONE);
        androidTargetSelectorComposite.setLayout(new GridLayout());
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        gridData.horizontalSpan = 2;
        gridData.heightHint = 200;
        androidTargetSelectorComposite.setLayoutData(gridData);


        sdkTargetSelector = new SdkTargetSelector(androidTargetSelectorComposite, null);

        IAndroidTarget[] targets = new IAndroidTarget[0];
        if (Sdk.getCurrent() != null) {
            targets = Sdk.getCurrent().getTargets();
        }
        sdkTargetSelector.setTargets(targets);

        // Check to see if we have an SDK. If we don't, we need to wait before
        // continuing
        AndroidSdkManager sdkManager = AndroidSdkManager.getInstance();
        synchronized ( sdkManager ) {
            AndroidSdkInstall defaultSDKInstall = sdkManager.getDefaultSdkInstall();

            if ( defaultSDKInstall != null ) {
                sdkInstalled = true;
            } else {
                setPageComplete(false);

                Job installationJob = sdkManager.getInstallationJob();
                if ( installationJob == null ) {
                    JavaSdkPlugin.getDefault().logError("Unable to check status of AWS SDK for Android download", null);
                    return;
                }

                final Composite pleaseWait = new Composite(composite, SWT.None);
                pleaseWait.setLayout(new GridLayout(1, false));
                GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
                layoutData.horizontalSpan = 2;
                pleaseWait.setLayoutData(layoutData);
                Label label = new Label(pleaseWait, SWT.None);
                label.setText("The AWS SDK for Android is currently downloading.  Please wait while it completes.");
                label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                ProgressBar progressBar = new ProgressBar(pleaseWait, SWT.INDETERMINATE);
                progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                installationJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                sdkInstalled = true;
                                pleaseWait.dispose();
                                composite.getParent().layout();
                                composite.getShell().pack(true);
                                composite.getParent().redraw();
                                updateErrorMessage();
                            }
                        });
                    }
                });
            }
        }


        bindControls();
        updateErrorMessage();
    }

    public static class AndroidTargetObservableValue extends AbstractObservableValue {

        private final SdkTargetSelector sdkTargetSelector;
        private IAndroidTarget androidTarget;

        public AndroidTargetObservableValue(final SdkTargetSelector sdkTargetSelector) {
            this.sdkTargetSelector = sdkTargetSelector;

            sdkTargetSelector.setSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    AndroidTargetObservableValue.this.setValue(sdkTargetSelector.getSelected());
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                    System.out.println("Default selected!");
                }
            });
        }

        public Object getValueType() {
            return IAndroidTarget.class;
        }

        @Override
        protected Object doGetValue() {
            return androidTarget;
        }

        @Override
        protected void doSetValue(final Object value) {
            final Object oldValue = androidTarget;
            this.androidTarget = (IAndroidTarget)value;

            AndroidTargetObservableValue.this.fireValueChange(new ValueDiff() {
                @Override
                public Object getOldValue() {
                    return oldValue;
                }

                @Override
                public Object getNewValue() {
                    return value;
                }
            });
        }
    }

    private void bindControls() {
        ISWTObservableValue projectNameTextObservableValue = SWTObservables.observeText(projectNameText, SWT.Modify);
        IObservableValue projectNameDataModelObservableValue = PojoObservables.observeValue(dataModel, "projectName");
        bindingContext.bindValue(projectNameTextObservableValue, projectNameDataModelObservableValue,
            new UpdateValueStrategy().setAfterConvertValidator(new ProjectNameValidator()), null);

        ISWTObservableValue packageNameTextObservableValue = SWTObservables.observeText(packageNameText, SWT.Modify);
        IObservableValue packageNameDataModelObservableValue = PojoObservables.observeValue(dataModel, "packageName");
        bindingContext.bindValue(packageNameTextObservableValue, packageNameDataModelObservableValue,
            new UpdateValueStrategy().setAfterConvertValidator(new PackageNameValidator()), null);

        ISWTObservableValue sampleCodeButtonObservableValue = SWTObservables.observeSelection(sampleCodeButton);
        IObservableValue sampleCodeDataModelObservableValue = PojoObservables.observeValue(dataModel, "sampleCodeIncluded");
        bindingContext.bindValue(sampleCodeButtonObservableValue, sampleCodeDataModelObservableValue, null, null);

        IObservableValue androidTargetObservableValue  = new AndroidTargetObservableValue(sdkTargetSelector);
        IObservableValue androidTargetModelObservableValue = PojoObservables.observeValue(dataModel, "androidTarget");
        bindingContext.bindValue(androidTargetObservableValue, androidTargetModelObservableValue,
            new UpdateValueStrategy().setAfterConvertValidator(new AndroidTargetValidator()), null);

        aggregateValidationStatus = new AggregateValidationStatus(
            bindingContext.getBindings(), AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            public void handleChange(ChangeEvent event) {
                updateErrorMessage();
            }
        });
    }

    private void updateErrorMessage() {
        if (aggregateValidationStatus == null) return;

        IStatus status = (IStatus)aggregateValidationStatus.getValue();
        if (sdkInstalled == false) {
            status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID,
                "No AWS SDK for Android available yet");
        }

        if (Sdk.getCurrent() == null ||
            Sdk.getCurrent().getTargets() == null ||
            Sdk.getCurrent().getTargets().length == 0) {
            status = new Status(IStatus.ERROR, AndroidSDKPlugin.PLUGIN_ID,
            "No Android platforms installed yet - Use the Android SDK manager to install target Android platforms");
        }

        if (status.getSeverity() == IStatus.OK) {
            AndroidProjectWizardPage.this.setErrorMessage(null);
            AndroidProjectWizardPage.this.setPageComplete(true);
        } else {
            AndroidProjectWizardPage.this.setErrorMessage(status.getMessage());
            AndroidProjectWizardPage.this.setPageComplete(false);
        }
    }
}