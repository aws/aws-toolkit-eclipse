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
package com.amazonaws.eclipse.lambda.upload.wizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.plugin.AbstractAwsJobWizard;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.upload.wizard.page.FunctionConfigurationPage;
import com.amazonaws.eclipse.lambda.upload.wizard.page.TargetFunctionSelectionPage;
import com.amazonaws.eclipse.lambda.upload.wizard.util.UploadFunctionUtil;

public class UploadFunctionWizard extends AbstractAwsJobWizard {

    private final IProject project;
    private final IJavaElement selectedJavaElement;
    private UploadFunctionWizardDataModel dataModel;

    public UploadFunctionWizard(IJavaElement selectedJavaElement) {
        super("Upload Function to AWS Lambda");
        this.project = selectedJavaElement.getJavaProject().getProject();
        this.selectedJavaElement = selectedJavaElement;
        initDataModel();
    }

    @Override
    public void addPages() {
        addPage(new TargetFunctionSelectionPage(dataModel));
        addPage(new FunctionConfigurationPage(dataModel));
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackUploadCanceled();
        return true;
    }

    @Override
    protected void initDataModel() {
        // Load valid request handler classes
        List<String> handlerClasses = new ArrayList<>();
        handlerClasses.addAll(UploadFunctionUtil.findValidHandlerClass(project));
        handlerClasses.addAll(UploadFunctionUtil.findValidStreamHandlerClass(project));

        if (handlerClasses.isEmpty()) {
            MessageDialog.openError(
                    Display.getCurrent().getActiveShell(),
                    "Invalid AWS Lambda Project",
                    "No Lambda function handler class is found in the project. " +
                    "You need to have at least one concrete class that implements the " +
                    "com.amazonaws.services.lambda.runtime.RequestHandler interface.");
            return;
        }

        // Load existing lambda project metadata
        LambdaFunctionProjectMetadata md = null;
        try {
            md = ProjectMetadataManager.loadLambdaProjectMetadata(project);
        } catch (IOException e) {
            LambdaPlugin.getDefault().logInfo(
                  "Ignoring the existing metadata for project ["
                          + project.getName()
                          + "] since the content is invalid.");
        }

        dataModel = new UploadFunctionWizardDataModel(project, selectedJavaElement, handlerClasses, md);

        if (md != null && md.getLastDeploymentHandler() != null) {
            dataModel.setHandler(md.getLastDeploymentHandler());
        }
    }

    @Override
    protected IStatus doFinish(IProgressMonitor monitor) {
        LambdaAnalytics.trackMetrics(dataModel.getFunctionDataModel().isCreateNewResource(),
                dataModel.getRequestHandlerImplementerClasses().size());

        monitor.beginTask("Uploading AWS Lambda Function Project [" +
                project.getName() + "]", 100);
        long startTime = System.currentTimeMillis();

        try {
            UploadFunctionUtil.performFunctionUpload(dataModel, monitor, 100);
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to upload project to Lambda",
                    new AwsActionException(AwsToolkitMetricType.LAMBDA_UPLOAD_FUNCTION_WIZARD.getName(), e.getMessage(), e));
            LambdaAnalytics.trackUploadFailed();
            return Status.OK_STATUS;
        }

        LambdaPlugin.getDefault().getProjectChangeTracker()
                .markProjectAsNotDirty(project);

        LambdaAnalytics.trackUploadSucceeded();

        LambdaAnalytics.trackUploadTotalTime(System.currentTimeMillis() - startTime);
        monitor.done();

        return Status.OK_STATUS;
    }

    @Override
    protected String getJobTitle() {
        return "Uploading function code to Lambda";
    }
}
