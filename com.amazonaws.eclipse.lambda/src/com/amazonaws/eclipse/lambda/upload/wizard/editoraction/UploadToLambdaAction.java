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
package com.amazonaws.eclipse.lambda.upload.wizard.editoraction;

import org.eclipse.jface.action.IAction;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.*;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.lambda.upload.wizard.handler.UploadFunctionToLambdaCommandHandler;

public class UploadToLambdaAction extends AbstractLambdaEditorAction {

    public void run(IAction action) {
        trackUploadWizardOpenedFromEditorContextMenu();
        UploadFunctionToLambdaCommandHandler
                .doUploadFunctionProjectToLambda(javaProject.getProject());
    }

    private void trackUploadWizardOpenedFromEditorContextMenu() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addAttribute(ATTR_NAME_OPENED_FROM, ATTR_VALUE_FILE_EDITOR_CONTEXT_MENU)
                .build());
    }

}