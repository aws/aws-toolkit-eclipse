/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Simply calling firePropertyChange(IEditorPart.PROP_DIRTY) won't mark the
 * editor as dirty. This is because ServerEditor (which is our EditorSite)
 * doesn't defer to its pages to determine dirty status, but just asks the
 * global command manager. We have to execute() this stub to achieve the
 * desired effect. It doesn't do anything.
 */
final class NullOperation extends AbstractOperation {

    public NullOperation() {
        super("");
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return Status.OK_STATUS;
    }

    @Override
    public boolean canRedo() {
        return false;
    }

    @Override
    public boolean canUndo() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }
}
