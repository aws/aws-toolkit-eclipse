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
package com.amazonaws.eclipse.lambda.upload.wizard.editoraction;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

@SuppressWarnings("restriction")
public abstract class AbstractLambdaEditorAction implements IObjectActionDelegate {

    protected IJavaElement selectedJavaElement;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if (!(targetPart instanceof JavaEditor)) {
            return;
        }
        JavaEditor javaEditor = (JavaEditor)targetPart;
        selectedJavaElement = JavaUI.getEditorInputJavaElement(javaEditor.getEditorInput());
    }
}
