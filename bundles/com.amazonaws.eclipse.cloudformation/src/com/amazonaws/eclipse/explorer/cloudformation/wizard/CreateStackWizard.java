/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.BrowserUtils;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.CancelUpdateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostRequest;
import com.amazonaws.services.cloudformation.model.EstimateTemplateCostResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;

/**
 * Wizard to create a new cloud formation stack
 */
public class CreateStackWizard extends Wizard {

    private CreateStackWizardDataModel dataModel;

    private CreateStackWizardFirstPage firstPage;
    private CreateStackWizardSecondPage secondPage;
    private CreateStackWizardThirdPage thirdPage;

    public CreateStackWizard() {
        this(null, null, null);
    }

    public CreateStackWizard(String stackName, Mode mode) {
        this(stackName, null, mode);
    }

    public CreateStackWizard(IPath filePath, Mode mode) {
        this(null, filePath, mode);
    }

    public CreateStackWizard(String stackName, IPath filePath, Mode mode) {
           setNeedsProgressMonitor(false);
           if ( mode == Mode.Update ) {
               setWindowTitle("Update Cloud Formation Stack");
           } else if (mode == Mode.Create) {
               setWindowTitle("Create New Cloud Formation Stack");
           } else {
               setWindowTitle("Estimate the Cost of the Template");
           }
           setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                   .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
           dataModel = new CreateStackWizardDataModel();
           dataModel.setStackName(stackName);
           if (filePath != null) {
               dataModel.setUsePreselectedTemplateFile(true);
               dataModel.setUseTemplateFile(true);
               dataModel.setTemplateFile(filePath.toFile().getAbsolutePath());
           }
           if (mode != null) {
               dataModel.setMode(mode);
           }
    }

    @Override
    public boolean performFinish() {
        final CreateStackRequest createStackRequest = getCreateStackRequest();

        new Job("Creating stack") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    AmazonCloudFormation cloudFormationClient = AwsToolkitCore.getClientFactory()
                            .getCloudFormationClient();
                    if ( dataModel.getMode() == Mode.Create ) {
                        cloudFormationClient.createStack(createStackRequest);
                    } else if (dataModel.getMode() == Mode.Update ){
                        cloudFormationClient.updateStack(createUpdateStackRequest(createStackRequest));
                    }

                    if (dataModel.getMode() == Mode.EstimateCost) {
                        EstimateTemplateCostRequest estimateTemplateCostRequest = createEstimateTemplateCostRequest(createStackRequest);
                        EstimateTemplateCostResult estimateTemplateCostResult = cloudFormationClient.estimateTemplateCost(estimateTemplateCostRequest);

                        String url = estimateTemplateCostResult.getUrl();
                        BrowserUtils.openExternalBrowser(url);
                    } else {
                        // Let the user have the time to cancel the update
                        // operationin Progress.
                        Thread.sleep(1000 * 30);
                    }

                    return Status.OK_STATUS;
                } catch ( Exception e ) {
                    return new Status(Status.ERROR, CloudFormationPlugin.PLUGIN_ID, "Failed to " +
                            ((dataModel.getMode() == Mode.Create) ? "create" : "update") +
                            " stack", e);
                }
            }

            @Override
            protected void canceling() {
                try {
                    AmazonCloudFormation cloudFormationClient = AwsToolkitCore.getClientFactory()
                            .getCloudFormationClient();
                    cloudFormationClient.cancelUpdateStack(new CancelUpdateStackRequest().withStackName(dataModel.getStackName()));
                } catch ( Exception e ) {
                    CloudFormationPlugin.getDefault().logError("Couldn't cancel the stack update", e);
                    StatusManager.getManager().handle( new Status(Status.ERROR, CloudFormationPlugin.PLUGIN_ID,
                           "Couldn't cancel the stack update:", e));
                }

            }

        }.schedule();

        return true;
    }

    private UpdateStackRequest createUpdateStackRequest(CreateStackRequest createStackRequest) {
        UpdateStackRequest rq = new UpdateStackRequest();
        rq.setStackName(createStackRequest.getStackName());
        rq.setCapabilities(createStackRequest.getCapabilities());
        rq.setParameters(createStackRequest.getParameters());
        rq.setTemplateBody(createStackRequest.getTemplateBody());
        rq.setTemplateURL(createStackRequest.getTemplateURL());
        rq.setTags(createStackRequest.getTags());
        return rq;
    }

    private EstimateTemplateCostRequest createEstimateTemplateCostRequest(CreateStackRequest createStackRequest) {
        EstimateTemplateCostRequest rq = new EstimateTemplateCostRequest();
        rq.setParameters(createStackRequest.getParameters());
        rq.setTemplateBody(createStackRequest.getTemplateBody());
        rq.setTemplateURL(createStackRequest.getTemplateURL());
        return rq;
    }

    private boolean needsSecondPage = true;
    protected void setNeedsSecondPage(boolean needsSecondPage) {
        this.needsSecondPage = needsSecondPage;
        getContainer().updateButtons();
    }

    public boolean needsSecondPage() {
        return needsSecondPage;
    }

    public boolean needsThirdPage() {
        return Mode.EstimateCost != dataModel.getMode();
    }

    private CreateStackRequest getCreateStackRequest() {
        CreateStackRequest rq = new CreateStackRequest();
        rq.setDisableRollback(!dataModel.getRollbackOnFailure());
        if ( dataModel.getNotifyWithSNS() ) {
            List<String> arns = new ArrayList<>();
            arns.add(dataModel.getSnsTopicArn());
            rq.setNotificationARNs(arns);
        }
        rq.setStackName(dataModel.getStackName());
        if ( dataModel.getUseTemplateFile() ) {
            rq.setTemplateBody(dataModel.getTemplateBody());
        } else {
            rq.setTemplateURL(dataModel.getTemplateUrl());
        }
        if ( rq.getTimeoutInMinutes() != null && rq.getTimeoutInMinutes() > 0 ) {
            rq.setTimeoutInMinutes(dataModel.getTimeoutMinutes());
        }
        List<Parameter> params = new ArrayList<>();
        for ( TemplateParameter parameter : dataModel.getParametersDataModel().getTemplateParameters() ) {
            String value = (String) dataModel.getParametersDataModel().getParameterValues().get(parameter.getParameterKey());
            if ( value != null && value.length() > 0 ) {
                params.add(new Parameter().withParameterKey(parameter.getParameterKey()).withParameterValue(value));
            }
        }
        rq.setParameters(params);

        rq.setCapabilities(dataModel.getRequiredCapabilities());

        if (dataModel.getTagModel().getPairSet() != null) {
            List<Tag> tags = new ArrayList<>(dataModel.getTagModel().getPairSet().size());
            for (Pair pair : dataModel.getTagModel().getPairSet()) {
                tags.add(new Tag().withKey(pair.getKey()).withValue(pair.getValue()));
            }
            rq.setTags(tags);
        }

        return rq;
    }

    @Override
    public void addPages() {
        if (firstPage == null) {
            firstPage = new CreateStackWizardFirstPage(this);
        }
        if (secondPage == null) {
            secondPage = new CreateStackWizardSecondPage(this.getDataModel());
        }
        addPage(firstPage);
        addPage(secondPage);
        if (Mode.EstimateCost != dataModel.getMode()) {
            thirdPage = new CreateStackWizardThirdPage(this.getDataModel());
            addPage(thirdPage);
        }
    }

    CreateStackWizardDataModel getDataModel() {
        return dataModel;
    }

    @Override
    public int getPageCount() {
        return (needsSecondPage() ? 2 : 1) + (needsThirdPage() ? 1 : 0);
    }

    @Override
    public IWizardPage[] getPages() {
        if (needsSecondPage()) {
            return super.getPages();
        } else {
            List<IWizardPage> wizardPages = new ArrayList<>(super.getPageCount());
            for (IWizardPage wizardPage : super.getPages()) {
                if (wizardPage instanceof CreateStackWizardSecondPage) {
                    continue;
                }
                wizardPages.add(wizardPage);
            }
            return wizardPages.toArray(new IWizardPage[0]);
        }
    }

    @Override
    public IWizardPage getNextPage(IWizardPage currentPage) {
        if (currentPage == firstPage && needsSecondPage()) {
            return secondPage;
        }
        return thirdPage;
    }

    /**
     * There's no second page in some cases.
     */
    @Override
    public boolean canFinish() {
        if (needsSecondPage())
            return super.canFinish();
        else
            return getPages()[0].isPageComplete();
    }
}
