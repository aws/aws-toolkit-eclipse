/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;

public class CreateBucketAction extends AwsAction {

    public CreateBucketAction() {
        super(AwsToolkitMetricType.EXPLORER_S3_CREATE_BUCKET);
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor("add"));
        setText("Create New Bucket");
    }

    @Override
    protected void doRun() {
        WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), new CreateBucketWizard());
        if (Window.OK == dialog.open()) {
            actionSucceeded();
        } else {
            actionCanceled();
        }
        actionFinished();
    }
}
