package com.amazonaws.eclipse.lambda.upload.wizard.editoraction;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

@SuppressWarnings("restriction")
abstract class AbstractLambdaEditorAction implements IObjectActionDelegate {

    protected IJavaProject javaProject;

    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if (!(targetPart instanceof JavaEditor)) {
            return;
        }
        JavaEditor javaEditor = (JavaEditor)targetPart;
        javaProject = EditorUtility.getJavaProject(javaEditor.getEditorInput());
    }
}
