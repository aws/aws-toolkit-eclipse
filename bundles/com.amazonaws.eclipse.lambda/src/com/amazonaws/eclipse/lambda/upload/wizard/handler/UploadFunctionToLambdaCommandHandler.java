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
package com.amazonaws.eclipse.lambda.upload.wizard.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.util.WorkbenchUtils;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.ui.LambdaJavaProjectUtil;
import com.amazonaws.eclipse.lambda.upload.wizard.UploadFunctionWizard;

public class UploadFunctionToLambdaCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IJavaElement selectedJavaElement = LambdaJavaProjectUtil.getSelectedJavaElementFromCommandEvent(event);
        if (selectedJavaElement != null) {
            LambdaAnalytics.trackUploadWizardOpenedFromProjectContextMenu();
            doUploadFunctionProjectToLambda(selectedJavaElement);
        }
        return null;
    }

    public static void doUploadFunctionProjectToLambda(IJavaElement selectedJavaElement) {

        if (!WorkbenchUtils.openSaveFilesDialog(PlatformUI.getWorkbench())) {
            return;
        }

        WizardDialog wizardDialog = new WizardDialog(
                Display.getCurrent().getActiveShell(),
                new UploadFunctionWizard(selectedJavaElement));
        wizardDialog.open();
    }
}
