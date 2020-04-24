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
package com.amazonaws.eclipse.lambda.upload.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newComposite;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel.P_CREATE_NEW_VERSION_ALIAS;
import static com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel.P_KMS_ENCRYPTION;
import static com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel.P_NONE_ENCRYPTION;
import static com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel.P_PUBLISH_NEW_VERSION;
import static com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel.P_S3_ENCRYPTION;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.ui.SelectOrCreateBucketComposite;
import com.amazonaws.eclipse.core.ui.SelectOrCreateKmsKeyComposite;
import com.amazonaws.eclipse.core.widget.CheckboxComplex;
import com.amazonaws.eclipse.core.widget.RadioButtonComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.RangeValidator;
import com.amazonaws.eclipse.lambda.LambdaConstants;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionAliasesScopeParam;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionDeploymentMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionMetadata;
import com.amazonaws.eclipse.lambda.ui.SelectOrCreateBasicLambdaRoleComposite;
import com.amazonaws.eclipse.lambda.ui.SelectOrInputFunctionAliasComposite;
import com.amazonaws.eclipse.lambda.upload.wizard.model.FunctionConfigPageDataModel;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.services.lambda.model.FunctionConfiguration;

public class FunctionConfigurationPage extends WizardPageWithOnEnterHook {
    private static final int MIN_MEMORY = 128;
    private static final int MAX_MEMORY = 3008;
    private static final int DEFAULT_MEMORY = 512;

    private static final int MIN_TIMEOUT = 1;
    private static final int MAX_TIMEOUT = 900;
    private static final int DEFAULT_TIMEOUT = 15;

    /* Data model and binding */
    private UploadFunctionWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* Basic settings */
    private Label functionNameLabel;
    private TextComplex descriptionTextComplex;

    /* Function execution role setting */
    private SelectOrCreateBasicLambdaRoleComposite selectOrCreateBasicLambdaRoleComposite;

    /* Function versioning and alias setting */
    private CheckboxComplex publishNewVersionCheckbox;
    private CheckboxComplex newVersionAliasCheckbox;
    private SelectOrInputFunctionAliasComposite selectOrInputFunctionAliasComposite;

    /* Target S3 bucket setting */
    private SelectOrCreateBucketComposite selectOrCreateBucketComposite;
    private RadioButtonComplex noneEncryptionButton;
    private RadioButtonComplex amazonS3EncryptionButton;
    private RadioButtonComplex awsKmsEncryptionButton;
    private SelectOrCreateKmsKeyComposite selectOrCreateKmsKeyComposite;

    /* Advanced settings */
    private TextComplex memoryTextComplex;
    private TextComplex timeoutTextComplex;

    /**
     * The validation status listener to be registered to the composite
     */
    private final IChangeListener functionConfigValidationStatusListener = new IChangeListener() {
        @Override
        public void handleChange(ChangeEvent event) {
            Object observable = event.getObservable();
            if (observable instanceof AggregateValidationStatus == false) return;

            AggregateValidationStatus statusObservable = (AggregateValidationStatus)observable;
            Object statusObservableValue = statusObservable.getValue();
            if (statusObservableValue instanceof IStatus == false) return;

            IStatus status = (IStatus)statusObservableValue;
            boolean success = (status.getSeverity() == IStatus.OK);
            if (success) {
                setErrorMessage(null);
            } else {
                setErrorMessage(status.getMessage());
            }
            FunctionConfigurationPage.super.setPageComplete(success);
        }
    };

    public FunctionConfigurationPage(UploadFunctionWizardDataModel dataModel) {
        super("Function Configuration");
        setTitle("Function Configuration");
        setDescription("Configure this Lambda function and upload to AWS.");
        setPageComplete(false);

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public void createControl(Composite parent) {
        ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComposite);

        Composite functionConfigurationComposite = new Composite(scrolledComposite, SWT.NONE);
        functionConfigurationComposite.setLayout(new GridLayout(1, false));

        scrolledComposite.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (functionConfigurationComposite != null) {
                    Rectangle r = scrolledComposite.getClientArea();
                    scrolledComposite.setMinSize(functionConfigurationComposite.computeSize(r.width, SWT.DEFAULT));
                }
            }
        });
        scrolledComposite.setContent(functionConfigurationComposite);

        createBasicSettingSection(functionConfigurationComposite);
        createFunctionRoleSettingSection(functionConfigurationComposite);
        createFunctionVersioningAndAliasSettingSection(functionConfigurationComposite);
        createS3BucketSection(functionConfigurationComposite);
        createAdvancedSettingSection(functionConfigurationComposite);

        aggregateValidationStatus.addChangeListener(functionConfigValidationStatusListener);
        setControl(scrolledComposite);
    }

    @Override
    public void onEnterPage() {
        refreshIamRole();
        refreshFunctionAliases();
        refreshS3Bucket();
        refreshKmsKey();

        functionNameLabel.setText(dataModel.getFunctionDataModel().getFunctionName());

        if (dataModel.getFunctionDataModel().isSelectExistingResource()) {
            FunctionConfiguration function = dataModel.getFunctionDataModel().getExistingResource();
            descriptionTextComplex.setText(function.getDescription());
            memoryTextComplex.setText(String.valueOf(function.getMemorySize()));
            timeoutTextComplex.setText(String.valueOf(function.getTimeout()));
        }
    }

    private void createBasicSettingSection(Composite parent) {
        Group group = newGroup(parent, "Basic Settings");
        group.setLayout(createSectionGroupLayout());

        newLabel(group, "Name:");
        functionNameLabel = newFillingLabel(group, "", 2);
        descriptionTextComplex = TextComplex.builder(group,
                    bindingContext, PojoProperties.value(FunctionConfigPageDataModel.P_DESCRIPTION).observe(dataModel.getFunctionConfigPageDataModel()))
                .createLabel(true)
                .labelColSpan(1)
                .labelValue("Description:")
                .textColSpan(2)
                .defaultValue(dataModel.getFunctionConfigPageDataModel().getDescription())
                .textMessage("The description for the function (optional)")
                .build();
    }

    private void createFunctionRoleSettingSection(Composite parent) {
        Group group = newGroup(parent, "Function Role");
        setItalicFont(newLink(
                group,
                LambdaConstants.webLinkListener,
                "Select the IAM role that AWS Lambda can assume to execute the function on your behalf. <a href=\""
                        + LambdaConstants.LAMBDA_EXECUTION_ROLE_DOC_URL
                        + "\">Learn more</a> about Lambda execution roles.", 1, 100, 30));

        selectOrCreateBasicLambdaRoleComposite = new SelectOrCreateBasicLambdaRoleComposite(
                group, bindingContext, dataModel.getLambdaRoleDataModel());
    }

    private void createFunctionVersioningAndAliasSettingSection(Composite parent) {
        Group group = newGroup(parent, "Function Versioning and Alias");
        setItalicFont(newLink(
                group,
                LambdaConstants.webLinkListener,
                "You can publish a new immutable version and an alias to that version whenever you have a new revision of the Lambda function. <a href=\""
                        + LambdaConstants.LAMBDA_FUNCTION_VERSIONING_AND_ALIASES_URL
                        + "\">Learn more</a> about Lambda function versioning and aliases.", 1, 100, 30));

        publishNewVersionCheckbox = CheckboxComplex.builder()
                .composite(group)
                .dataBindingContext(bindingContext)
                .defaultValue(false)
                .labelValue("Publish new version")
                .pojoObservableValue(PojoProperties.value(P_PUBLISH_NEW_VERSION).observe(dataModel.getFunctionConfigPageDataModel()))
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onPublishNewVersionCheckboxSelected();
                    }
                })
                .build();

        newVersionAliasCheckbox = CheckboxComplex.builder()
                .composite(group)
                .dataBindingContext(bindingContext)
                .defaultValue(false)
                .labelValue("Provide an alias to this new version")
                .pojoObservableValue(PojoProperties.value(P_CREATE_NEW_VERSION_ALIAS).observe(dataModel.getFunctionConfigPageDataModel()))
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onNewVersionAliasCheckboxSelected();
                    }
                })
                .build();

        selectOrInputFunctionAliasComposite = new SelectOrInputFunctionAliasComposite(
                group, bindingContext, dataModel.getFunctionAliasDataModel());
        GridLayout gridLayout = (GridLayout)selectOrInputFunctionAliasComposite.getLayout();
        gridLayout.marginLeft = 20;
        selectOrInputFunctionAliasComposite.setLayout(gridLayout);
    }

    public void onPublishNewVersionCheckboxSelected() {
        newVersionAliasCheckbox.getCheckbox().setEnabled(publishNewVersionCheckbox.getCheckbox().getSelection());
        selectOrInputFunctionAliasComposite.setEnabled(publishNewVersionCheckbox.getCheckbox().getSelection() && newVersionAliasCheckbox.getCheckbox().getSelection());
    }

    private void onEncryptionRadioButtonsSelected() {
        selectOrCreateKmsKeyComposite.setEnabled(awsKmsEncryptionButton.getRadioButton().getSelection());
    }

    private void onNewVersionAliasCheckboxSelected() {
        selectOrInputFunctionAliasComposite.setEnabled(newVersionAliasCheckbox.getCheckbox().getSelection());
    }

    private void createS3BucketSection(Composite parent) {
        Group group = newGroup(parent, "S3 Bucket for Function Code");

        selectOrCreateBucketComposite = new SelectOrCreateBucketComposite(
                group, bindingContext, dataModel.getS3BucketDataModel());
        GridLayout gridLayout = (GridLayout) selectOrCreateBucketComposite.getLayout();
        gridLayout.verticalSpacing = 0;
        selectOrCreateBucketComposite.setLayout(gridLayout);

        setItalicFont(newLink(group, LambdaConstants.webLinkListener,
                "Upload Lambda zip file with encrytion to protect data at rest by using Amazon S3 master-key or by using AWS KMS master-key. <a href=\""
                        + LambdaConstants.LAMBDA_FUNCTION_ENCRYPTION_URL
                        + "\">Learn more</a> about Amazon S3 encryption.",
                1, 100, 30));

        Composite radioButtonComposite = newComposite(group, 1, 3, true);
        SelectionAdapter selectAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onEncryptionRadioButtonsSelected();
            }
        };
        noneEncryptionButton = createEncryptionButton(
                radioButtonComposite,
                P_NONE_ENCRYPTION,
                dataModel.getFunctionConfigPageDataModel().isNoneEncryption(),
                "None",
                selectAdapter
                );

        amazonS3EncryptionButton = createEncryptionButton(
                radioButtonComposite,
                P_S3_ENCRYPTION,
                dataModel.getFunctionConfigPageDataModel().isS3Encryption(),
                "Amazon S3 master-key",
                selectAdapter
                );

        awsKmsEncryptionButton = createEncryptionButton(
                radioButtonComposite,
                P_KMS_ENCRYPTION,
                dataModel.getFunctionConfigPageDataModel().isKmsEncryption(),
                "AWS KMS master-key",
                selectAdapter
                );

        selectOrCreateKmsKeyComposite = new SelectOrCreateKmsKeyComposite(
                group, bindingContext, dataModel.getKmsKeyDataModel());
    }

    private void createAdvancedSettingSection(Composite parent) {
        Group group = newGroup(parent, "Advanced Settings");
        group.setLayout(createSectionGroupLayout());

        String memoryErrMsg = String.format(
                "Please enter a memory size within the range of [%d, %d]",
                MIN_MEMORY, MAX_MEMORY);

        String timeoutErrMsg = String.format(
                "Please enter a timeout within the range of [%d, %d]",
                MIN_TIMEOUT, MAX_TIMEOUT);

        memoryTextComplex = TextComplex.builder(group,
                    bindingContext, PojoProperties.value(FunctionConfigPageDataModel.P_MEMORY).observe(dataModel.getFunctionConfigPageDataModel()))
                .addValidator(new RangeValidator(memoryErrMsg,
                        MIN_MEMORY, MAX_MEMORY))
                .createLabel(true)
                .defaultValue(Integer.toString(DEFAULT_MEMORY))
                .labelColSpan(1)
                .labelValue("Memory (MB):")
                .textColSpan(2)
                .build();

        timeoutTextComplex = TextComplex.builder(group,
                    bindingContext, PojoProperties.value(FunctionConfigPageDataModel.P_TIMEOUT).observe(dataModel.getFunctionConfigPageDataModel()))
                .addValidator(new RangeValidator(timeoutErrMsg,
                        MIN_TIMEOUT, MAX_TIMEOUT))
                .createLabel(true)
                .defaultValue(Integer.toString(DEFAULT_TIMEOUT))
                .labelColSpan(1)
                .labelValue("Timeout (s):")
                .textColSpan(2)
                .build();
    }

    private RadioButtonComplex createEncryptionButton(Composite composite, String propertyName, boolean defaultValue, String labelValue, SelectionListener listener) {
        return RadioButtonComplex.builder()
                .composite(composite)
                .dataBindingContext(bindingContext)
                .pojoObservableValue(PojoProperties.value(propertyName).observe(dataModel.getFunctionConfigPageDataModel()))
                .defaultValue(defaultValue)
                .labelValue(labelValue)
                .selectionListener(listener)
                .build();
    }

    private GridLayout createSectionGroupLayout() {
        GridLayout layout = new GridLayout(3, true);
        layout.marginWidth = 15;
        return layout;
    }

    private String getLastDeploymentBucketName() {
        String handler = dataModel.getHandler();
        String defaultS3Bucket = null;
        LambdaFunctionProjectMetadata projectMetadata = dataModel.getProjectMetadataBeforeUpload();
        if (projectMetadata != null && projectMetadata.getHandlerMetadata().get(handler) != null) {
            LambdaFunctionMetadata functionMetadata = projectMetadata.getHandlerMetadata().get(handler);
            LambdaFunctionDeploymentMetadata deployment = functionMetadata.getDeployment();
            if (deployment != null) {
                defaultS3Bucket = deployment.getAwsS3BucketName();
            }
        }
        return defaultS3Bucket;
    }

    private String getDefaultIamRole() {
        FunctionConfiguration function = dataModel.getFunctionDataModel().getExistingResource();
        String defaultRoleName = null;
        if (dataModel.getFunctionDataModel().isSelectExistingResource() && function != null) {
            defaultRoleName = function.getRole().split("/")[1];
        } else {
            defaultRoleName = dataModel.getProjectMetadataBeforeUpload() == null
                    ? null
                    : this.dataModel.getProjectMetadataBeforeUpload()
                            .getLastDeploymentRoleName();
        }
        return defaultRoleName;
    }

    private void refreshFunctionAliases() {
         selectOrInputFunctionAliasComposite.refreshComposite(
                 new LambdaFunctionAliasesScopeParam(
                         AwsToolkitCore.getDefault().getCurrentAccountId(),
                         dataModel.getRegionDataModel().getRegion().getId(),
                         dataModel.getFunctionDataModel().getFunctionName()),
                 null);
         onPublishNewVersionCheckboxSelected();
    }

    /*
     * Async loading of S3 buckets. S3 buckets might be loaded multiple times
     * since it depends on the current region being selected
     */
    private void refreshS3Bucket() {
        selectOrCreateBucketComposite.refreshComposite(new AwsResourceScopeParamBase(
                AwsToolkitCore.getDefault().getCurrentAccountId(),
                dataModel.getRegionDataModel().getRegion().getId()), getLastDeploymentBucketName());
    }

    private void refreshKmsKey() {
        selectOrCreateKmsKeyComposite.refreshComposite(new AwsResourceScopeParamBase(
                AwsToolkitCore.getDefault().getCurrentAccountId(),
                dataModel.getRegionDataModel().getRegion().getId()), null);

        onEncryptionRadioButtonsSelected();
    }

    private void refreshIamRole() {
        selectOrCreateBasicLambdaRoleComposite.refreshComposite(new AwsResourceScopeParamBase(
                AwsToolkitCore.getDefault().getCurrentAccountId(),
                dataModel.getRegionDataModel().getRegion().getId()), getDefaultIamRole());
    }

    private static Label newLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        return label;
    }

    /*
     * Italic font resource
     */
    private List<Font> italicFontList = new ArrayList<>();

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont().getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        Font italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
        italicFontList.add(italicFont);
    }

    @Override
    public void dispose() {
        italicFontList.forEach(Font::dispose);
        super.dispose();
    }
}