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
package com.amazonaws.eclipse.explorer.lambda;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class OpenFunctionEditorAction extends Action {
    private final String functionArn;
    private final String functionName;

    public OpenFunctionEditorAction(String functionArn, String functionName) {
        this.setText("Open in Function Editor");
        this.functionArn = functionArn;
        this.functionName = functionName;
    }

    @Override
    public void run() {
        String regionId = RegionUtils.getCurrentRegion().getId();
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = new FunctionEditorInput(accountId, regionId, functionArn, functionName);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, "com.amazonaws.eclipse.explorer.lambda.FunctionEditor");
                } catch (PartInitException e) {
                    LambdaPlugin.getDefault().reportException("Unable to open the AWS Lambda function editor.", e);
                }
            }
        });
    }

}
