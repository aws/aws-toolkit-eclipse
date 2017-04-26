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
package com.amazonaws.eclipse.lambda.serverless.ui;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.trackRegionComboChangeSelection;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.DeployServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.validator.StackNameValidator;
import com.amazonaws.eclipse.lambda.upload.wizard.dialog.CreateS3BucketDialog;
import com.amazonaws.eclipse.lambda.upload.wizard.page.S3BucketUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class DeployServerlessProjectPage extends WizardPage {

    private final DeployServerlessProjectDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    // Select region
    private Combo regionCombo;

    /* S3 bucket */
    private Combo bucketNameCombo;
    private ISWTObservableValue bucketNameComboObservable;
    private IObservableValue bucketNameModelObservable;
    private IObservableValue bucketNameLoadedObservable = new WritableValue();
    private LoadS3BucketsInFunctionRegionThread loadS3BucketsInFunctionRegionThread;
    private Button createBucketButton;

    /* Constants */
    private static final String NONE_FOUND = "None found";

    /* CloudFormation stack */
    private Text stackNameText;
    private ISWTObservableValue stackNameTextObservable;
    private IObservableValue stackNameModelObservable;

    public DeployServerlessProjectPage(DeployServerlessProjectDataModel dataModel) {
        super("ServerlessDeployWizardPage");
        setTitle("Deploy Serverless stack to AWS CloudFormation.");
        setDescription("Deploy your Serverless template to AWS CloudFormation as a stack.");
        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        createRegionSection(container);
        createS3BucketSection(container);
        createStackSection(container);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            public void handleChange(ChangeEvent arg0) {
                populateHandlerValidationStatus();
            }
        });

        onRegionSelectionChange();
        setControl(container);
    }

    private void populateHandlerValidationStatus() {
        if (aggregateValidationStatus == null) {
            return;
        }

        Object value = aggregateValidationStatus.getValue();
        if (! (value instanceof IStatus)) return;
        IStatus handlerInfoStatus = (IStatus) value;

        boolean isHandlerInfoValid = (handlerInfoStatus.getSeverity() == IStatus.OK);

        if (isHandlerInfoValid) {
            setErrorMessage(null);
            super.setPageComplete(true);
        } else {
            setErrorMessage(handlerInfoStatus.getMessage());
            super.setPageComplete(false);
        }
    }

    private void createRegionSection(Composite composite) {
        Group regionGroup = newGroup(composite, "Select AWS Region");
        regionGroup.setLayout(new GridLayout(1, false));

        newFillingLabel(regionGroup,
                "Select the AWS region where your CloudFormation stack is created:");

        Region initialRegion = null;

        regionCombo = newCombo(regionGroup);
        for (Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.CLOUD_FORMATION)) {
            regionCombo.add(region.getName());
            regionCombo.setData(region.getName(), region);
        }

        // Find the default region selection
        if (initialRegion == null) {
            if (RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.CLOUD_FORMATION) ) {
                initialRegion = RegionUtils.getCurrentRegion();
            } else {
                initialRegion = RegionUtils.getRegion(LambdaPlugin.DEFAULT_REGION);
            }
        }
        regionCombo.setText(initialRegion.getName());

        regionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                trackRegionComboChangeSelection();
                onRegionSelectionChange();
            }
        });
    }

    private void onRegionSelectionChange() {
        Region region = (Region)regionCombo.getData(regionCombo.getText());
        dataModel.setRegion(region);

        onUpdateRegion();
    }

    private void onUpdateRegion() {
        refreshBucketsInFunctionRegion();
    }

    public void refreshBucketsInFunctionRegion() {
        bucketNameLoadedObservable.setValue(false);

        if (bucketNameCombo != null) {
            bucketNameCombo.setItems(new String[] { "Loading buckets in "
                    + dataModel.getRegion().getName() });
            bucketNameCombo.select(0);
            bucketNameCombo.setEnabled(false);
        }

        CancelableThread.cancelThread(loadS3BucketsInFunctionRegionThread);
        loadS3BucketsInFunctionRegionThread = new LoadS3BucketsInFunctionRegionThread(null);
        loadS3BucketsInFunctionRegionThread.start();
    }

    private void createS3BucketSection(Composite parent) {
        Group group = newGroup(parent, "Select or Create S3 Bucket for Your Function Code");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "S3 Bucket Name:");

        Composite composite = new Composite(group, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 2;
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(2, false));

        bucketNameCombo = newCombo(composite, 1);
        bucketNameCombo.setEnabled(false);
        bucketNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                LambdaAnalytics.trackS3BucketComboSelectionChange();
            }
        });

        bucketNameComboObservable = SWTObservables
                .observeSelection(bucketNameCombo);
        bucketNameModelObservable = PojoObservables.observeValue(dataModel,
                DeployServerlessProjectDataModel.P_BUCKET_NAME);
        bindingContext.bindValue(bucketNameComboObservable,
                bucketNameModelObservable);

        createBucketButton = new Button(composite, SWT.PUSH);
        createBucketButton.setText("Create");
        createBucketButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                LambdaAnalytics.trackClickCreateNewBucketButton();

                CreateS3BucketDialog dialog = new CreateS3BucketDialog(
                        Display.getCurrent().getActiveShell(), dataModel.getRegion());
                int returnCode = dialog.open();

                if (returnCode == 0) {
                    String bucketName = dialog.getCreatedBucketName();

                    if (bucketNameLoadedObservable.getValue().equals(Boolean.TRUE)) {
                        bucketNameCombo.add(bucketName);
                        bucketNameCombo.select(bucketNameCombo.getItemCount() - 1);

                    } else {
                        CancelableThread.cancelThread(loadS3BucketsInFunctionRegionThread);
                        bucketNameLoadedObservable.setValue(true);

                        bucketNameCombo.setItems(new String[] {bucketName});
                        bucketNameCombo.setEnabled(true);
                        bucketNameCombo.select(0);
                        updateUIDataToModel();
                    }
                }
            }
        });
    }

    private void createStackSection(Composite parent) {
        Group group = newGroup(parent, "Select or Create CloudFormation Stack");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "CloudFormation Stack Name:");

        Composite composite = new Composite(group, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 2;
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(1, false));

        stackNameText = newText(composite);
        stackNameTextObservable = SWTObservables
                .observeText(stackNameText, SWT.Modify);
        stackNameModelObservable = PojoObservables.observeValue(dataModel,
                DeployServerlessProjectDataModel.P_STACK_NAME);
        bindingContext.bindValue(stackNameTextObservable,
                stackNameModelObservable);
        stackNameTextObservable.setValue(dataModel.getProjectName() + "-devstack");

        ControlDecoration handlerPackageTextDecoration = newControlDecoration(stackNameText, "");

        // bind validation of stack name
        ChainValidator<String> stackNameValidator = new ChainValidator<String>(
                stackNameTextObservable,
                new StackNameValidator());
        bindingContext.addValidationStatusProvider(stackNameValidator);
        new DecorationChangeListener(handlerPackageTextDecoration,
                stackNameValidator.getValidationStatus());
    }

    private GridLayout createSectionGroupLayout() {
        GridLayout layout = new GridLayout(3, true);
        layout.marginWidth = 15;
        return layout;
    }

    private static Label newLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    private final class LoadS3BucketsInFunctionRegionThread extends
            CancelableThread {

        private final String defaultBucket;

        /**
         * @param defaultBucket
         *            the bucket that should be selected by default after all
         *            buckets are loaded.
         */
        LoadS3BucketsInFunctionRegionThread(String defaultBucket) {
            this.defaultBucket = defaultBucket;
        }

        @Override
        public void run() {

            long startTime = System.currentTimeMillis();
            // Using the default global S3 client will not return the correct region for bucket in other regions
            AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientByEndpoint(
                    RegionUtils.getRegion("us-west-2").getServiceEndpoint(ServiceAbbreviations.S3));
            final List<Bucket> bucketsInFunctionRegion = S3BucketUtil
                    .listBucketsInRegion(s3, dataModel.getRegion());
            LambdaAnalytics.trackLoadBucketTimeDuration(System
                    .currentTimeMillis() - startTime);

            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    try {
                        synchronized (LoadS3BucketsInFunctionRegionThread.this) {
                            if (!isCanceled()) {
                                if (bucketsInFunctionRegion.isEmpty()) {
                                    bucketNameCombo
                                            .setItems(new String[] { NONE_FOUND });
                                    bucketNameLoadedObservable.setValue(false);
                                } else {
                                    bucketNameCombo.removeAll();
                                    for (Bucket bucket : bucketsInFunctionRegion) {
                                        bucketNameCombo.add(bucket.getName());
                                    }
                                    bucketNameCombo.setEnabled(true);
                                    bucketNameCombo
                                            .select(findDefaultBucket(bucketsInFunctionRegion));
                                    updateUIDataToModel();

                                    bucketNameLoadedObservable.setValue(true);
                                }
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }

        private int findDefaultBucket(List<Bucket> buckets) {
            for (int i = 0; i < buckets.size(); i++) {
                if (buckets.get(i).getName().equals(this.defaultBucket)) {
                    return i;
                }
            }
            return 0;
        }
    }

    public void updateUIDataToModel() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding) iterator.next();
            binding.updateTargetToModel();
        }
    }
}
