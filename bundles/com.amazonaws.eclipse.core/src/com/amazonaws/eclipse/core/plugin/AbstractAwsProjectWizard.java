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
package com.amazonaws.eclipse.core.plugin;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

/**
 * Base class for creating new project wizard.
 */
public abstract class AbstractAwsProjectWizard extends AbstractAwsWizard implements INewWizard {
    protected IStructuredSelection selection;
    protected IWorkbench workbench;
    private long actionStartTimeMilli;
    private long actionEndTimeMilli;

    protected AbstractAwsProjectWizard(String windowTitle) {
        super(windowTitle);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
        this.workbench = workbench;
    }

    @Override
    public final boolean performFinish() {
        beforeExecution();
        actionStartTimeMilli = System.currentTimeMillis();

        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                new WorkspaceModifyOperation() {

                    @Override
                    protected void execute(IProgressMonitor monitor) throws CoreException,
                            InvocationTargetException, InterruptedException {
                        IStatus status = doFinish(monitor);
                        actionEndTimeMilli = System.currentTimeMillis();
                        afterExecution(status);
                        if (status.getSeverity() == IStatus.ERROR) {
                            throw new InvocationTargetException(status.getException(), status.getMessage());
                        }
                    }
                }.run(monitor);
            }
        };
        IStatus status = Status.OK_STATUS;
        try {
            boolean fork = true;
            boolean cancelable = true;
            getContainer().run(fork, cancelable, runnable);
        } catch (InterruptedException ex) {
            status = Status.CANCEL_STATUS;
        } catch (InvocationTargetException ex) {
            Throwable exception = ex.getCause();
            if (exception instanceof OperationCanceledException) {
                status = Status.CANCEL_STATUS;
            } else {
                status = getPlugin().reportException(exception.getMessage(), exception);
            }
        }

        return status.isOK();
    }

    protected long getActionExecutionTimeMillis() {
        return actionEndTimeMilli - actionStartTimeMilli;
    }

    protected abstract AbstractAwsPlugin getPlugin();
}
