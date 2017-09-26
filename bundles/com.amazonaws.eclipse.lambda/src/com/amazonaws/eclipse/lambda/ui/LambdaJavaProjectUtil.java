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
package com.amazonaws.eclipse.lambda.ui;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

public class LambdaJavaProjectUtil {

    // Return the underlying selected JavaElement when performing a command action. Return null if the selected element
    // is not Java Element and log the error message.
    public static IJavaElement getSelectedJavaElementFromCommandEvent(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
                .getActivePage().getSelection();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structurredSelection = (IStructuredSelection)selection;
            Object firstSelection = structurredSelection.getFirstElement();

            if (firstSelection instanceof IJavaElement) {
                return (IJavaElement) firstSelection;
            } else {
                LambdaPlugin.getDefault().logInfo(
                        "Invalid selection: " + firstSelection + " is not Java Element.");
                return null;
            }
        }
        return null;
    }

    // Return the default Java handler FQCN from the selected Java element which must be an ICompilationUnit.
    public static String figureOutDefaultLambdaHandler(IJavaElement selectedJavaElement, Set<String> lambdaHandlerSet) {
        if (selectedJavaElement instanceof ICompilationUnit) {
            ICompilationUnit compilationUnit = (ICompilationUnit)selectedJavaElement;
            try {
                return Arrays.stream(compilationUnit.getTypes())
                        .map(IType::getFullyQualifiedName)
                        .filter(t -> lambdaHandlerSet.contains(t))
                        .findFirst()
                        .orElse(null);
            } catch (JavaModelException e) {
                return null;
            }
        }
        return null;
    }
}
