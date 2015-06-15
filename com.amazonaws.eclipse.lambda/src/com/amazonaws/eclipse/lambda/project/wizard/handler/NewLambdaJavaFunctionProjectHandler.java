package com.amazonaws.eclipse.lambda.project.wizard.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.lambda.project.wizard.NewLambdaJavaFunctionProjectWizard;

public class NewLambdaJavaFunctionProjectHandler extends AbstractHandler {

    @SuppressWarnings("restriction")
    public Object execute(ExecutionEvent event) throws ExecutionException {
        NewLambdaJavaFunctionProjectWizard newWizard = new NewLambdaJavaFunctionProjectWizard();
        newWizard.init(PlatformUI.getWorkbench(), null);
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
        return dialog.open();
    }

}
