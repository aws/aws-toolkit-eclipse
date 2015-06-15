package com.amazonaws.eclipse.lambda.upload.wizard.editoraction;

import org.eclipse.jface.action.IAction;

import com.amazonaws.eclipse.lambda.upload.wizard.handler.UploadFunctionToLambdaCommandHandler;

public class UploadToLambdaAction extends AbstractLambdaEditorAction {

    public void run(IAction action) {
        UploadFunctionToLambdaCommandHandler
                .doUploadFunctionProjectToLambda(javaProject.getProject());
    }

}