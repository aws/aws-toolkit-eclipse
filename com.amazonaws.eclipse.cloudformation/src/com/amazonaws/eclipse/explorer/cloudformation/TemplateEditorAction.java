/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;

import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

/**
 * Base class for context sensitive actions in the TemplateEditor
 */
public class TemplateEditorAction {

    protected IPath filePath;
    protected TemplateDocument templateDocument;

    public void selectionChanged(IAction action, ISelection selection) {}

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        if ( targetPart instanceof TemplateEditor ) {
            TemplateEditor templateEditor = (TemplateEditor)targetPart;
            templateDocument = templateEditor.getTemplateDocument();

            IEditorInput editorInput = templateEditor.getEditorInput();
            if ( editorInput instanceof FileEditorInput ) {
                filePath = ((FileEditorInput) editorInput).getPath();
            }
        }
    }
}