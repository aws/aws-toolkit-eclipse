/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.serverless.wizard.editoraction;

import org.eclipse.jface.action.IAction;

import com.amazonaws.eclipse.lambda.launching.SamLocalExecution;
import com.amazonaws.eclipse.lambda.launching.SamLocalExecution.LaunchMode;
import com.amazonaws.eclipse.lambda.upload.wizard.editoraction.AbstractLambdaEditorAction;

/**
 * Action of local debugging SAM application triggered by right clicking Lambda function editor
 */
public class SamLocalAction {

    public static class RunSamLocalAction extends AbstractLambdaEditorAction {
        @Override
        public void run(IAction action) {
            SamLocalExecution.launch(selectedJavaElement, LaunchMode.RUN);
        }
    }

    public static class DebugSamLocalAction extends AbstractLambdaEditorAction {
        @Override
        public void run(IAction action) {
            SamLocalExecution.launch(selectedJavaElement, LaunchMode.DEBUG);
        }
    }
}