/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.s3.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;

public class CreateBucketAction extends Action {

    public CreateBucketAction() {
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor("add"));
        setText("Create New Bucket");
    }

    @Override
    public void run() {
        WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), new CreateBucketWizard());
        dialog.open();
    }
}