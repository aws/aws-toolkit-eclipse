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

import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.upload.wizard.handler.UploadFunctionToLambdaCommandHandler;

public class UploadToLambdaAction extends AbstractLambdaEditorAction {

    @Override
    public void run(IAction action) {
        LambdaAnalytics.trackUploadWizardOpenedFromEditorContextMenu();
        UploadFunctionToLambdaCommandHandler
                .doUploadFunctionProjectToLambda(selectedJavaElement);
    }
}