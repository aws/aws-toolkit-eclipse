/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.upload.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.RangeValidator;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.eclipse.lambda.UrlConstants;
import com.amazonaws.eclipse.lambda.upload.wizard.dialog.CreateBasicLambdaRoleDialog;
import com.amazonaws.eclipse.lambda.upload.wizard.dialog.CreateS3BucketDialog;
import com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class FunctionConfigurationComposite extends Composite {

    private static final int MIN_MEMORY = 128;
    private static final int MAX_MEMORY = 1536;
    private static final int DEFAULT_MEMORY = 512;

    private static final int MIN_TIMEOUT = 1;
    private static final int MAX_TIMEOUT = 300;
    private static final int DEFAULT_TIMEOUT = 15;

    private UploadFunctionWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /**
     * @see #setValidationStatusChangeListener(IChangeListener)
     * @see #removeValidationStatusChangeListener()
     */
    private IChangeListener validationStatusChangeListener;

    /* Basic settings */
    private Label functionNameLabel;
    private Text descriptionText;
    private ISWTObservableValue descriptionTextObservable;
    private IObservableValue descriptionModelObservable;

    /* Function execution setting */
    private Combo handlerCombo;
    private ISWTObservableValue handlerComboObservable;
    private IObservableValue handlerModelObservable;

    private Combo roleNameCombo;
    private IObservableValue roleLoadedObservable = new WritableValue();
    private Button createRoleButton;

    /* S3 bucket */
    private Combo bucketNameCombo;
    private ISWTObservableValue bucketNameComboObservable;
    private IObservableValue bucketNameModelObservable;
    private IObservableValue bucketNameLoadedObservable = new WritableValue();
    private LoadS3BucketsInFunctionRegionThread loadS3BucketsInFunctionRegionThread;
    private Button createBucketButton;

    /* Advanced settings */
    private Text memoryText;
    private ControlDecoration memoryTextDecoration;
    private ISWTObservableValue memoryTextObservable;
    private IObservableValue memoryModelObservable;
    private Text timeoutText;
    private ControlDecoration timeoutTextDecoration;
    private ISWTObservableValue timeoutTextObservable;
    private IObservableValue timeoutModelObservable;

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    FunctionConfigurationComposite(Composite parent,
            UploadFunctionWizardDataModel dataModel) {
        super(parent, SWT.NONE);

        this.dataModel = dataModel;

        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        setLayout(new GridLayout(1, false));
        createControls(this);

        bindControls();
        initializeValidators();
        pupulateDefaultData();

        loadLambdaRolesAsync();

        // We defer loading S3 buckets until the user reaches the page
        // (after the function region is selected in the previous page).
    }

    /**
     * Set listener that will be notified whenever the validation status of this
     * composite is updated. This method removes the listener (if any) that is
     * currently registered to this composite - only one listener instance is
     * allowed at a time.
     */
    public synchronized void setValidationStatusChangeListener(
            IChangeListener listener) {
        removeValidationStatusChangeListener();
        validationStatusChangeListener = listener;
        aggregateValidationStatus.addChangeListener(listener);
    }

    /**
     * @see #setValidationStatusChangeListener(IChangeListener)
     */
    public synchronized void removeValidationStatusChangeListener() {
        if (validationStatusChangeListener != null) {
            aggregateValidationStatus
                    .removeChangeListener(validationStatusChangeListener);
            validationStatusChangeListener = null;
        }
    }

    public void updateUIDataToModel() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding) iterator.next();
            binding.updateTargetToModel();
        }
    }

    private void createControls(Composite parent) {
        createBasicSettingSection(parent);
        createFunctionExecutionSettingSection(parent);
        createS3BucketSection(parent);
        createAdvancedSettingSection(parent);
    }

    private void createBasicSettingSection(Composite parent) {
        Group group = newGroup(parent, "Basic Settings");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "Name:");
        functionNameLabel = newFillingLabel(group, "", 2);

        newLabel(group, "Description:");
        descriptionText = newText(group, "", 2);
        descriptionText
                .setMessage("The description for the function (optional)");
    }

    private void createFunctionExecutionSettingSection(Composite parent) {
        Group group = newGroup(parent, "Function Execution");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "Handler:");
        handlerCombo = newCombo(group, 2);
        for (String handler : dataModel.getRequestHandlerImplementerClasses()) {
            handlerCombo.add(handler);
        }
        handlerCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                LambdaAnalytics.trackFunctionHandlerComboSelectionChange();
            }
        });

        Label separator = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData separatorGridData = new GridData(GridData.FILL_HORIZONTAL);
        separatorGridData.horizontalSpan = 3;
        separator.setLayoutData(separatorGridData);

        setItalicFont(newLink(
                group,
                UrlConstants.webLinkListener,
                "Select the IAM role that AWS Lambda can assume to execute the function on your behalf. <a href=\""
                        + UrlConstants.LAMBDA_EXECUTION_ROLE_DOC_URL
                        + "\">Learn more</a> about Lambda execution roles.", 3));

        newLabel(group, "IAM Role:");

        Composite roleComposite = new Composite(group, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 2;
        roleComposite.setLayoutData(gridData);
        roleComposite.setLayout(new GridLayout(2, false));

        roleNameCombo = newCombo(roleComposite, 1);
        roleNameCombo.setEnabled(false);

        roleNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                LambdaAnalytics.trackRoleComboSelectionChange();
                onRoleSelectionChange();
            }
        });

        createRoleButton = new Button(roleComposite, SWT.PUSH);
        createRoleButton.setText("Create");
        createRoleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                LambdaAnalytics.trackClickCreateNewRoleButton();

                CreateBasicLambdaRoleDialog dialog = new CreateBasicLambdaRoleDialog(
                        Display.getCurrent().getActiveShell());
                int returnCode = dialog.open();

                if (returnCode == 0) {
                    Role createdRole = dialog.getCreatedRole();

                    if (roleLoadedObservable.getValue().equals(Boolean.FALSE)) {
                        roleLoadedObservable.setValue(true);
                        roleNameCombo.removeAll(); // remove the "none found" item
                    }

                    roleNameCombo.setEnabled(true);
                    roleNameCombo.add(createdRole.getRoleName());
                    roleNameCombo.setData(createdRole.getRoleName(), createdRole);
                    roleNameCombo.select(roleNameCombo.getItemCount() - 1);

                    onRoleSelectionChange();
                }
            }
        });
        createRoleButton.setEnabled(false); // re-enabled after the roles are loaded

    }

    private void createS3BucketSection(Composite parent) {
        Group group = newGroup(parent, "S3 Bucket for Function Code");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "S3 Bucket:");

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

    private void createAdvancedSettingSection(Composite parent) {
        Group group = newGroup(parent, "Advanced Settings");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "Memory (MB):");
        memoryText = newText(group, "", 2);
        memoryTextDecoration = newControlDecoration(memoryText, "");

        newLabel(group, "Timeout (s):");
        timeoutText = newText(group, "", 2);
        timeoutTextDecoration = newControlDecoration(timeoutText, "");
    }

    private GridLayout createSectionGroupLayout() {
        GridLayout layout = new GridLayout(3, true);
        layout.marginWidth = 15;
        return layout;
    }

    private void bindControls() {

        FunctionConfigPageDataModel createModel = dataModel
                .getFunctionConfigPageDataModel();

        descriptionTextObservable = SWTObservables.observeText(descriptionText,
                SWT.Modify);
        descriptionModelObservable = PojoObservables.observeValue(createModel,
                FunctionConfigPageDataModel.P_DESCRIPTION);
        bindingContext.bindValue(descriptionTextObservable,
                descriptionModelObservable);

        handlerComboObservable = SWTObservables.observeSelection(handlerCombo);
        handlerModelObservable = PojoObservables.observeValue(createModel,
                FunctionConfigPageDataModel.P_HANDLER);
        bindingContext
                .bindValue(handlerComboObservable, handlerModelObservable);

        bucketNameComboObservable = SWTObservables
                .observeSelection(bucketNameCombo);
        bucketNameModelObservable = PojoObservables.observeValue(createModel,
                FunctionConfigPageDataModel.P_BUCKET_NAME);
        bindingContext.bindValue(bucketNameComboObservable,
                bucketNameModelObservable);

        memoryTextObservable = SWTObservables.observeText(memoryText,
                SWT.Modify);
        memoryModelObservable = PojoObservables.observeValue(createModel,
                FunctionConfigPageDataModel.P_MEMORY);
        bindingContext.bindValue(memoryTextObservable, memoryModelObservable);

        timeoutTextObservable = SWTObservables.observeText(timeoutText,
                SWT.Modify);
        timeoutModelObservable = PojoObservables.observeValue(createModel,
                FunctionConfigPageDataModel.P_TIMEOUT);
        bindingContext.bindValue(timeoutTextObservable, timeoutModelObservable);
    }

    private void initializeValidators() {
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                roleLoadedObservable, new BooleanValidator(
                        "Please select an execution role for your function")));

        bindingContext
                .addValidationStatusProvider(new ChainValidator<Boolean>(
                        bucketNameLoadedObservable,
                        new BooleanValidator(
                                "Please select the S3 bucket where your function code will be uploaded")));

        String memoryErrMsg = String.format(
                "Please enter a memory size within the range of [%d, %d]",
                MIN_MEMORY, MAX_MEMORY);
        ChainValidator<Long> memoryValidator = new ChainValidator<Long>(
                memoryModelObservable, new RangeValidator(memoryErrMsg,
                        MIN_MEMORY, MAX_MEMORY));
        bindingContext.addValidationStatusProvider(memoryValidator);
        new DecorationChangeListener(memoryTextDecoration,
                memoryValidator.getValidationStatus());

        String timeoutErrMsg = String.format(
                "Please enter a timeout within the range of [%d, %d]",
                MIN_TIMEOUT, MAX_TIMEOUT);
        ChainValidator<Long> timeoutValidator = new ChainValidator<Long>(
                timeoutModelObservable, new RangeValidator(timeoutErrMsg,
                        MIN_TIMEOUT, MAX_TIMEOUT));
        bindingContext.addValidationStatusProvider(timeoutValidator);
        new DecorationChangeListener(timeoutTextDecoration,
                timeoutValidator.getValidationStatus());
    }

    public void populateNewFunctionName() {
        functionNameLabel.setText(dataModel.getNewFunctionName());
    }

    public void pupulateDefaultData() {
        descriptionTextObservable.setValue("");
        handlerModelObservable.setValue(dataModel
                .getRequestHandlerImplementerClasses().get(0));
        memoryTextObservable.setValue(Integer.toString(DEFAULT_MEMORY));
        timeoutTextObservable.setValue(Integer.toString(DEFAULT_TIMEOUT));
    }

    public void populateExistingFunctionConfig(FunctionConfiguration funcConfig) {
        functionNameLabel.setText(funcConfig.getFunctionName());

        if (funcConfig.getDescription() == null) {
            descriptionTextObservable.setValue("");
        } else {
            descriptionTextObservable.setValue(funcConfig.getDescription());
        }

        if (dataModel.getRequestHandlerImplementerClasses().contains(
                funcConfig.getHandler())) {
            handlerModelObservable.setValue(funcConfig.getHandler());
        } else {
            handlerModelObservable.setValue(dataModel
                    .getRequestHandlerImplementerClasses().get(0));
        }

        selectRoleByArn(funcConfig.getRole());

        memoryTextObservable.setValue(funcConfig.getMemorySize().toString());
        timeoutTextObservable.setValue(funcConfig.getTimeout().toString());
    }

    /*
     * Async loading of S3 buckets. S3 buckets might be loaded multiple times
     * since it depends on the current region being selected
     */

    public void refreshBucketsInFunctionRegion() {
        bucketNameLoadedObservable.setValue(false);

        if (bucketNameCombo != null) {
            bucketNameCombo.setItems(new String[] { "Loading buckets in "
                    + dataModel.getRegion().getName() });
            bucketNameCombo.select(0);
            bucketNameCombo.setEnabled(false);
        }

        CancelableThread.cancelThread(loadS3BucketsInFunctionRegionThread);
        loadS3BucketsInFunctionRegionThread = new LoadS3BucketsInFunctionRegionThread(
                getLastDeploymentBucketName());
        loadS3BucketsInFunctionRegionThread.start();
    }

    private String getLastDeploymentBucketName() {
        return this.dataModel.getProjectMetadataBeforeUpload() == null
                ? null
                : this.dataModel.getProjectMetadataBeforeUpload()
                        .getLastDeploymentBucketName();
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
            AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3Client();
            final List<Bucket> bucketsInFunctionRegion = S3BucketUtil
                    .listBucketsInRegion(s3, dataModel.getRegion());
            LambdaAnalytics.trackLoadBucketTimeDuration(System.currentTimeMillis() - startTime);

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

    /*
     * Async loading of IAM roles. IAM roles are only loaded once.
     */

    private List<Role> allRolesSortedByName = new LinkedList<Role>();
    private String roleArnToBeSelectedAfterRolesAreLoaded;

    private void selectRoleByArn(String roleArn) {
        if (roleLoadedObservable.getValue().equals(Boolean.TRUE)) {
            // role already loaded, directly select the item
            doSelectRoleByArn(roleArn);
        } else {
            // keep track the ARN and set the selection after the roles are
            // loaded
            roleArnToBeSelectedAfterRolesAreLoaded = roleArn;
        }
    }

    private void doSelectRoleByArn(String roleArn) {
        int index = 0;
        for (Role role : allRolesSortedByName) {
            if (role.getArn().equals(roleArn)) {
                break;
            }
            index++;
        }
        if (index < allRolesSortedByName.size()) {
            roleNameCombo.select(index);
        }
        onRoleSelectionChange();
    }

    private void loadLambdaRolesAsync() {
        roleLoadedObservable.setValue(false);

        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                roleNameCombo.setItems(new String[] { LOADING });
                roleNameCombo.select(0);
                roleNameCombo.setEnabled(false);
            }
        });

        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
                try {

                    long start = System.currentTimeMillis();
                    AmazonIdentityManagement iam = AwsToolkitCore
                            .getClientFactory().getIAMClient();
                    List<Role> roles = ServiceApiUtils.getAllLambdaRoles(iam);
                    LambdaAnalytics.trackLoadRoleTimeDuration(System.currentTimeMillis() - start);

                    roleNameCombo.removeAll();

                    if (roles.isEmpty()) {
                        roleNameCombo.setItems(new String[] { NONE_FOUND });
                        roleLoadedObservable.setValue(false);

                    } else {
                        List<String> allRoleNames = new LinkedList<String>();
                        Map<String, Role> allRolesMap = new HashMap<String, Role>();
                        for (Role role : roles) {
                            allRoleNames.add(role.getRoleName());
                            allRolesMap.put(role.getRoleName(), role);
                        }
                        Collections.sort(allRoleNames);

                        roleNameCombo.removeAll();
                        for (String roleName : allRoleNames) {
                            roleNameCombo.add(roleName);

                            Role role = allRolesMap.get(roleName);
                            roleNameCombo.setData(roleName, role);
                            allRolesSortedByName.add(role);
                        }
                        roleNameCombo.setEnabled(true);

                        if (roleArnToBeSelectedAfterRolesAreLoaded != null) {
                            doSelectRoleByArn(roleArnToBeSelectedAfterRolesAreLoaded);
                            roleArnToBeSelectedAfterRolesAreLoaded = null;
                        } else {
                            roleNameCombo.select(0);
                        }

                        roleLoadedObservable.setValue(true);
                        onRoleSelectionChange();
                    }

                    createRoleButton.setEnabled(true);

                } catch (Exception e) {
                    LambdaPlugin.getDefault().reportException(
                            "Failed to load IAM roles.", e);
                }
            }
        });
    }

    private void onRoleSelectionChange() {
        if (roleLoadedObservable.getValue().equals(Boolean.TRUE)) {
            Role role = (Role) roleNameCombo.getData(roleNameCombo.getText());
            dataModel.getFunctionConfigPageDataModel().setRole(role);
        }
    }

    private static Label newLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    /*
     * Italic font resource
     */

    private Font italicFont;

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont().getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    @Override
    public void dispose() {
        if (italicFont != null)
            italicFont.dispose();
        super.dispose();
    }

}
