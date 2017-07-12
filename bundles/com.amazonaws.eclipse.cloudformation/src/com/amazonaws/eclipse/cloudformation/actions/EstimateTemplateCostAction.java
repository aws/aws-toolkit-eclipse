/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizard;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;

public class EstimateTemplateCostAction extends TemplateEditorBaseAction {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateStackWizard(filePath, Mode.EstimateCost));
        dialog.open();
    }

}
