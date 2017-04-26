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
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Base class for all AWS Wizards. Subclasses should follow the pattern below to implement this class.
 * <li> it must have a DataModel member and initialize the DataModel by implementing {@link #initDataModel()} method.
 * <li> it must provide the Wizard Window Title
 * <li> it must provide the Job Title.
 */
abstract class AbstractAwsWizard extends Wizard {

    protected AbstractAwsWizard(String windowTitle) {
        super();
        setWindowTitle(windowTitle);
        setDefaultPageImageDescriptor(
                AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        setNeedsProgressMonitor(true);
    }

    // The actual implementation for finishing the wizard.
    protected abstract IStatus doFinish(IProgressMonitor monitor);

    // Initialize data model either with default values or load data from PreferenceStore/Project Metadata.
    // Subclasses should implement this method and call in an appropriate location, mostly at the end of Constructor.
    protected abstract void initDataModel();

    // The underlying Job Dialog title in the finish wizard.
    protected abstract String getJobTitle();

    protected void beforeExecution() {
        // Subclass should implement this method if actions are needed before performing finish.
        // Analytics goes here for recording the type of this action.
    }

    protected void afterExecution(IStatus status) {
        // Subclass should implement this method if actions are needed after performing finish.
        // Analytics goes here for recording whether the Wizard completion is problematic or not.
    }
}
