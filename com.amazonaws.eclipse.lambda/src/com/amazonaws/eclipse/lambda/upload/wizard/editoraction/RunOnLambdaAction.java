package com.amazonaws.eclipse.lambda.upload.wizard.editoraction;

import org.eclipse.jface.action.IAction;

import com.amazonaws.eclipse.lambda.invoke.handler.InvokeFunctionHandler;

public class RunOnLambdaAction extends AbstractLambdaEditorAction {

    public void run(IAction action) {
        InvokeFunctionHandler.invokeLambdaFunctionProject(javaProject.getProject());
    }

}
