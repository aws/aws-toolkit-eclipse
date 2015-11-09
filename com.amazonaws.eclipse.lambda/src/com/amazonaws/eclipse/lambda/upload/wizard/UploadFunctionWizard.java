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

import static com.amazonaws.eclipse.lambda.LambdaAnalytics.*;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_CANCELED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_FAILED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_SUCCEEDED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.EVENT_TYPE_UPLOAD_FUNCTION_WIZARD;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.upload.wizard.page.FunctionConfigurationPage;
import com.amazonaws.eclipse.lambda.upload.wizard.page.TargetFunctionSelectionPage;
import com.amazonaws.eclipse.lambda.upload.wizard.util.UploadFunctionUtil;

public class UploadFunctionWizard extends Wizard {

    private final UploadFunctionWizardDataModel dataModel;

    public UploadFunctionWizard(IProject project,
            List<String> requestHandlerImplementerClasses,
            LambdaFunctionProjectMetadata projectMetadata) {
        dataModel = new UploadFunctionWizardDataModel(project,
                requestHandlerImplementerClasses, projectMetadata);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new TargetFunctionSelectionPage(dataModel));
        addPage(new FunctionConfigurationPage(dataModel));
    }

    @Override
    public boolean performFinish() {

        try {
            Job uploadJob = new Job("Uploading function code to Lambda") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    trackMetrics();

                    IProject project = dataModel.getProject();

                    monitor.beginTask("Uploading AWS Lambda Function Project [" +
                            project.getName() + "]", 100);

                    try {
                        UploadFunctionUtil.performFunctionUpload(dataModel, monitor, 100);
                    } catch (Exception e) {
                        LambdaPlugin.getDefault().reportException(
                                "Failed to upload project to Lambda", e);
                        trackUploadFailed();
                        return new Status(Status.ERROR, LambdaPlugin.PLUGIN_ID,
                                e.getMessage(), e);
                    }

                    LambdaPlugin.getDefault().getProjectChangeTracker()
                            .markProjectAsNotDirty(project);

                    trackUploadSucceeded();
                    monitor.done();

                    return Status.OK_STATUS;
                }
            };

            uploadJob.setUser(true);
            uploadJob.schedule();

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Unexpected error during deployment", e.getCause());
        }

        return true;
    }

    @Override
    public boolean performCancel() {
        trackUploadCanceled();
        return true;
    }

    /*
     * Analytics
     */

    private void trackMetrics() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addBooleanMetric(METRIC_NAME_IS_CREATING_NEW_FUNCTION, dataModel.isCreatingNewFunction())
                .addMetric(METRIC_NAME_VALID_FUNCTION_HANDLER_CLASS_COUNT, dataModel.getRequestHandlerImplementerClasses().size())
                .build());
    }

    private void trackUploadSucceeded() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_SUCCEEDED)
                .build());
    }

    private void trackUploadFailed() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_FAILED)
                .build());
    }

    private void trackUploadCanceled() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_CANCELED)
                .build());
    }

}
