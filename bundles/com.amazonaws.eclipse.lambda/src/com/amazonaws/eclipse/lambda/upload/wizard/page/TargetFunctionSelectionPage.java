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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newComposite;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.trackRegionComboChangeSelection;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.RegionComposite;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionDeploymentMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionMetadata;
import com.amazonaws.eclipse.lambda.ui.SelectOrInputFunctionComposite;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;

public class TargetFunctionSelectionPage extends WizardPageWithOnEnterHook {

    /* Data model and binding */
    private final UploadFunctionWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* UI widgets */
    private ComboViewerComplex<String> handlerComboViewerComplex;
    private RegionComposite regionComposite;
    private SelectOrInputFunctionComposite functionComposite;

    public TargetFunctionSelectionPage(UploadFunctionWizardDataModel dataModel) {
        super("Select Target Lambda Function");
        setTitle("Select Target Lambda Function");
        setDescription("Choose the region and the target AWS Lambda function you want to create or update for your local lambda handler.");
        setPageComplete(false);

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createHandlerAndRegionSection(composite);
        createJavaFunctionSection(composite);

        initializeValidators();

        // Force refresh function list
        onHandlerSelectionChange();

        setControl(composite);
        setPageComplete(false);
    }

    private void createHandlerAndRegionSection(Composite composite) {
        Group handlerAndRegionGroup = newGroup(composite, "Select Lambda Handler and Target Region");
        handlerAndRegionGroup.setLayout(new GridLayout(1, false));

        Composite handlerComposite = newComposite(handlerAndRegionGroup, 1, 2);
        handlerComboViewerComplex = ComboViewerComplex.<String>builder()
                .composite(handlerComposite)
                .bindingContext(bindingContext)
                .pojoObservableValue(PojoProperties.value(UploadFunctionWizardDataModel.P_HANDLER).observe(dataModel))
                .defaultItem(dataModel.getHandler())
                .labelValue("Select the Handler:")
                .addListeners(e -> onHandlerSelectionChange())
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return element == null ? "" : element.toString();
                    }
                })
                .items(dataModel.getRequestHandlerImplementerClasses())
                .build();

        regionComposite = RegionComposite.builder()
                .parent(handlerAndRegionGroup)
                .bindingContext(bindingContext)
                .dataModel(dataModel.getRegionDataModel())
                .labelValue("Select the AWS Region:")
                .serviceName(ServiceAbbreviations.LAMBDA)
                .addListener(e -> {
                    trackRegionComboChangeSelection();
                    onRegionSelectionChange();
                })
                .build();
    }

    private void createJavaFunctionSection(Composite composite) {
        Group javaFunctionGroup = newGroup(composite, "Select or Create a Lambda Function:");
        javaFunctionGroup.setLayout(new GridLayout(1, false));
        functionComposite = new SelectOrInputFunctionComposite(
                javaFunctionGroup, bindingContext, dataModel.getFunctionDataModel());
    }

    private void onHandlerSelectionChange() {
        String handler = dataModel.getHandler();
        LambdaFunctionProjectMetadata projectMetadata = dataModel.getProjectMetadataBeforeUpload();
        if (projectMetadata != null && projectMetadata.getHandlerMetadata().get(handler) != null) {
            LambdaFunctionMetadata functionMetadata = projectMetadata.getHandlerMetadata().get(handler);
            LambdaFunctionDeploymentMetadata deployment = functionMetadata.getDeployment();
            if (deployment != null) {
                regionComposite.selectAwsRegion(RegionUtils.getRegion(deployment.getRegionId()));
            }
        }
        onRegionSelectionChange();
    }

    private void onRegionSelectionChange() {
        String handler = dataModel.getHandler();
        String defaultFunctionName = null;
        LambdaFunctionProjectMetadata projectMetadata = dataModel.getProjectMetadataBeforeUpload();
        if (projectMetadata != null && projectMetadata.getHandlerMetadata().get(handler) != null) {
            LambdaFunctionMetadata functionMetadata = projectMetadata.getHandlerMetadata().get(handler);
            LambdaFunctionDeploymentMetadata deployment = functionMetadata.getDeployment();
            if (deployment != null) {
                defaultFunctionName = deployment.getAwsLambdaFunctionName();
            }
        }
        functionComposite.refreshComposite(new AwsResourceScopeParamBase(
                AwsToolkitCore.getDefault().getCurrentAccountId(),
                dataModel.getRegionDataModel().getRegion().getId()), defaultFunctionName);
    }

    private void initializeValidators() {
        // Bind the validation status to the wizard page message
        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false) return;

                IStatus status = (IStatus)value;
                boolean success = (status.getSeverity() == IStatus.OK);
                if (success) {
                    setErrorMessage(null);
                } else {
                    setErrorMessage(status.getMessage());
                }
                TargetFunctionSelectionPage.super.setPageComplete(success);
            }
        });
    }

    @Override
    protected void onEnterPage() {
    }
}
