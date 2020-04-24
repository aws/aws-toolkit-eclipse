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
package com.amazonaws.eclipse.explorer.s3;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;

public class OpenBucketEditorAction extends AwsAction {

    private final String bucketName;

    public OpenBucketEditorAction(String bucketName) {
        super(AwsToolkitMetricType.EXPLORER_S3_OPEN_BUCKET_EDITOR);
        this.bucketName = bucketName;

        this.setText("Open in S3 Bucket Editor");
    }

    @Override
    protected void doRun() {
        String endpoint = AwsToolkitCore.getClientFactory().getS3BucketEndpoint(bucketName);
        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();

        final IEditorInput input = new BucketEditorInput(bucketName, endpoint, accountId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, "com.amazonaws.eclipse.explorer.s3.bucketEditor");
                    actionSucceeded();
                } catch (PartInitException e) {
                    actionFailed();
                    AwsToolkitCore.getDefault().logError("Unable to open the Amazon S3 bucket editor", e);
                } finally {
                    actionFinished();
                }
            }
        });
    }
}
