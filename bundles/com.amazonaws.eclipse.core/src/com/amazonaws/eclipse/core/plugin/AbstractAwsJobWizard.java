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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

public abstract class AbstractAwsJobWizard extends AbstractAwsWizard {

    protected AbstractAwsJobWizard(String windowTitle) {
        super(windowTitle);
    }

    @Override
    public final boolean performFinish() {

        beforeExecution();

        Job awsJob = new Job(getJobTitle()) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                IStatus status = doFinish(monitor);
                afterExecution(status);
                return status;
            }
        };
        awsJob.setUser(true);
        awsJob.schedule();
        return true;
    }
}
