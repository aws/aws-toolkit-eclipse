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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.trackRegionComboChangeSelection;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.core.ui.MultipleSelectionListComposite;
import com.amazonaws.eclipse.core.ui.RegionComposite;
import com.amazonaws.eclipse.core.ui.SelectOrCreateBucketComposite;
import com.amazonaws.eclipse.core.util.S3BucketUtil;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.lambda.LambdaConstants;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.model.SelectOrInputStackDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.DeployServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.ui.SelectOrInputStackComposite;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;

public class DeployServerlessProjectPage extends WizardPage {
    private static final String VALIDATING = "validating";
    private static final String INVALID = "invalid";
    private static final String VALID = "valid";

    private final DeployServerlessProjectDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    private RegionComposite regionComposite;
    private SelectOrCreateBucketComposite bucketComposite;
    private SelectOrInputStackComposite stackComposite;
    private MultipleSelectionListComposite<Capability> capabilitiesSelectionComposite;

    private IObservableValue templateValidated = new WritableValue();
    private ValidateTemplateThread validateTemplateThread;
    private Exception templateValidationException;

    public DeployServerlessProjectPage(DeployServerlessProjectDataModel dataModel) {
        super("ServerlessDeployWizardPage");
        setTitle("Deploy Serverless stack to AWS CloudFormation.");
        setDescription("Deploy your Serverless template to AWS CloudFormation as a stack.");
        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        createRegionSection(container);
        createS3BucketSection(container);
        createStackSection(container);
        createCapabilities(container);

        createValidationBinding();

        aggregateValidationStatus.addChangeListener(new IChangeListener() {
            @Override
            public void handleChange(ChangeEvent arg0) {
                populateHandlerValidationStatus();
            }
        });

        dataModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // We skip this event for inputing a new Stack name. We only listener to changes of Bucket, Existing Stack
                if (!evt.getPropertyName().equals(SelectOrInputStackDataModel.P_NEW_RESOURCE_NAME)) {
                    onDataModelPropertiesChange();
                }
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

    private void createRegionSection(Composite parent) {
        Group regionGroup = newGroup(parent, "Select AWS Region for the AWS CloudFormation stack");
        regionGroup.setLayout(new GridLayout(1, false));
        regionComposite = RegionComposite.builder()
            .parent(regionGroup)
            .bindingContext(bindingContext)
            .serviceName(ServiceAbbreviations.CLOUD_FORMATION)
            .dataModel(dataModel.getRegionDataModel())
            .labelValue("Select AWS Region:")
            .addListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    trackRegionComboChangeSelection();
                    onRegionSelectionChange();
                }
            })
            .build();
    }

    private void onRegionSelectionChange() {
        Region currentSelectedRegion = regionComposite.getCurrentSelectedRegion();
        if (bucketComposite != null) {
            String defaultBucket = dataModel.getMetadata().getLastDeploymentBucket(currentSelectedRegion.getId());
            bucketComposite.refreshComposite(new AwsResourceScopeParamBase(
                    AwsToolkitCore.getDefault().getCurrentAccountId(),
                    currentSelectedRegion.getId()), defaultBucket);
        }
        if (stackComposite != null) {
            String defaultStackName = dataModel.getMetadata().getLastDeploymentStack(currentSelectedRegion.getId());
            stackComposite.refreshComposite(new AwsResourceScopeParamBase(
                    AwsToolkitCore.getDefault().getCurrentAccountId(),
                    currentSelectedRegion.getId()), defaultStackName);
        }
    }

    private void createS3BucketSection(Composite parent) {
        Group group = newGroup(parent, "Select or Create S3 Bucket for Your Function Code");
        group.setLayout(new GridLayout(1, false));
        bucketComposite = new SelectOrCreateBucketComposite(
                group, bindingContext, dataModel.getBucketDataModel());

    }

    private void createStackSection(Composite parent) {
        Group group = newGroup(parent, "Select or Create CloudFormation Stack");
        group.setLayout(new GridLayout(1, false));
        stackComposite = new SelectOrInputStackComposite(
                group, bindingContext, dataModel.getStackDataModel());
    }

    private void createCapabilities(Composite parent) {
        Group group = newGroup(parent, "Configure capabilities");
        group.setLayout(new GridLayout(1, false));

        newLink(group,
                LambdaConstants.webLinkListener,
                    "If you have IAM resources, you can specify either capability. If you " +
                    "have IAM resources with custom names, you must specify CAPABILITY_NAMED_IAM. " +
                    "You must select at least one capability. " +
                    "For more information, see <a href=\"" +
                    LambdaConstants.CLOUDFORMATION_CAPABILITIES +
                    "\">Acknowledging IAM Resources in AWS CloudFormation Templates</a>.", 1, 100, 50);

        capabilitiesSelectionComposite = new MultipleSelectionListComposite<>(
                group, bindingContext, dataModel.getCapabilitiesDataModel(),
                Arrays.asList(Capability.values()),
                Arrays.asList(Capability.CAPABILITY_IAM),
                "You must select at least one capability");
    }

    private void createValidationBinding() {
        templateValidated.setValue(null);
        bindingContext.addValidationStatusProvider(new ChainValidator<String>(templateValidated, new IValidator() {
            @Override
            public IStatus validate(Object value) {
                if ( value == null ) {
                    return ValidationStatus.error("No template selected");
                }

                if ( ((String) value).equals(VALID) ) {
                    return ValidationStatus.ok();
                } else if ( ((String) value).equals(VALIDATING) ) {
                    return ValidationStatus.warning("Validating template...");
                } else if ( ((String) value).equals(INVALID) ) {
                    if ( templateValidationException != null ) {
                        return ValidationStatus.error("Invalid template: " + templateValidationException.getMessage());
                    } else {
                        return ValidationStatus.error("No template selected");
                    }
                }

                return ValidationStatus.ok();
            }
        }));
    }

    private void onDataModelPropertiesChange() {
        CancelableThread.cancelThread(validateTemplateThread);
        validateTemplateThread = new ValidateTemplateThread();
        validateTemplateThread.start();
    }

    // The IObservable value could only be updated in the UI thread.
    private void updateTemplateValidatedStatus(final CancelableThread motherThread, final String newStatus) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (motherThread) {
                    if (!motherThread.isCanceled()) {
                        templateValidated.setValue(newStatus);
                    }
                }
            }
        });
    }

    /**
     * Cancelable thread to validate a template and update the validation
     * status.
     */
    private final class ValidateTemplateThread extends CancelableThread {

        @Override
        public void run() {

            try {
                updateTemplateValidatedStatus(this, VALIDATING);

                // Update serverless template upon provided bucket name
                String lambdaFunctionJarFileKeyName = dataModel.getStackDataModel().getStackName() + "-" + System.currentTimeMillis() + ".zip";
                File serverlessTemplateFile = FunctionProjectUtil.getServerlessTemplateFile(dataModel.getProject());
                ServerlessModel model = Serverless.load(serverlessTemplateFile);

                model = Serverless.cookServerlessModel(model, dataModel.getMetadata().getPackagePrefix(),
                        S3BucketUtil.createS3Path(dataModel.getBucketDataModel().getBucketName(), lambdaFunctionJarFileKeyName));

                validateHandlersExist(model);

                String generatedServerlessFilePath = File.createTempFile(
                        "serverless-template", ".json").getAbsolutePath();
                File serverlessGeneratedTemplateFile = Serverless.write(model, generatedServerlessFilePath);
                serverlessGeneratedTemplateFile.deleteOnExit();
                dataModel.setLambdaFunctionJarFileKeyName(lambdaFunctionJarFileKeyName);
                dataModel.setUpdatedServerlessTemplate(serverlessGeneratedTemplateFile);

                // Validate the updated serverless template
                AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClientByRegion(
                        dataModel.getRegionDataModel().getRegion().getId());
                List<TemplateParameter> templateParameters = cloudFormation.validateTemplate(new ValidateTemplateRequest()
                        .withTemplateBody(IOUtils.toString(new FileInputStream(serverlessGeneratedTemplateFile))))
                        .getParameters();
                dataModel.getParametersDataModel().setTemplateParameters(templateParameters);

                updateTemplateValidatedStatus(this, VALID);
            } catch (Exception e) {
                templateValidationException = e;
                updateTemplateValidatedStatus(this, INVALID);
                LambdaPlugin.getDefault().logError(e.getMessage(), e);
            }
        }

        // Validate the Lambda handlers defined in the serverless template exist in the project.
        private void validateHandlersExist(ServerlessModel model) {
            for (Entry<String, ServerlessFunction> handler : model.getServerlessFunctions().entrySet()) {
                if (!dataModel.getHandlerClasses().contains(handler.getValue().getHandler())) {
                    throw new IllegalArgumentException(String.format(
                            "The configured handler class %s for Lambda handler %s doesn't exist!",
                            handler.getValue().getHandler(), handler.getKey()));
                }
            }
        }
    }
}
