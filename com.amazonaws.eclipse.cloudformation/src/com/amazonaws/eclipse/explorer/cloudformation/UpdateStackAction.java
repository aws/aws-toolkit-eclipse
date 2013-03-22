package com.amazonaws.eclipse.explorer.cloudformation;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;

import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizard;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;


public class UpdateStackAction extends TemplateEditorAction implements IObjectActionDelegate {

    public UpdateStackAction() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateStackWizard(filePath, Mode.Update));
        dialog.open();
    }

}
