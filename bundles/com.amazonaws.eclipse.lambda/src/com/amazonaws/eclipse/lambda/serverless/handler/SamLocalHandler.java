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
package com.amazonaws.eclipse.lambda.serverless.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaElement;

import com.amazonaws.eclipse.lambda.launching.SamLocalExecution;
import com.amazonaws.eclipse.lambda.launching.SamLocalExecution.LaunchMode;
import com.amazonaws.eclipse.lambda.ui.LambdaJavaProjectUtil;

/**
 * Action of running/debugging SAM application when right clicking project explorer.
 */
public class SamLocalHandler {

    public static class RunSamLocalHandler extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) throws ExecutionException {
            IJavaElement selectedJavaElement = LambdaJavaProjectUtil.getSelectedJavaElementFromCommandEvent(event);
            SamLocalExecution.launch(selectedJavaElement, LaunchMode.RUN);
            return null;
        }
    }

    public static class DebugSamLocalHandler extends AbstractHandler {
        @Override
        public Object execute(ExecutionEvent event) throws ExecutionException {
            IJavaElement selectedJavaElement = LambdaJavaProjectUtil.getSelectedJavaElementFromCommandEvent(event);
            SamLocalExecution.launch(selectedJavaElement, LaunchMode.DEBUG);
            return null;
        }
    }
}