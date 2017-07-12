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
package com.amazonaws.eclipse.lambda.project.wizard.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.lambda.project.wizard.NewLambdaFunctionWizard;

public class NewLambdaFunctionHandler extends AbstractHandler {

    @Override
    @SuppressWarnings("restriction")
    public Object execute(ExecutionEvent event) throws ExecutionException {
        NewLambdaFunctionWizard newWizard = new NewLambdaFunctionWizard();
        newWizard.init(PlatformUI.getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
        return dialog.open();
    }
}
