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

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.S3ContentProvider;
import com.amazonaws.eclipse.explorer.s3.util.ObjectUtils;
import com.amazonaws.services.s3.model.Bucket;

public class DeleteBucketAction extends AwsAction {

    private Collection<Bucket> buckets;

    public DeleteBucketAction(Collection<Bucket> buckets) {
        super(AwsToolkitMetricType.EXPLORER_S3_DELETE_BUCKET);
        this.buckets = buckets;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor("remove");
    }

    @Override
    public String getText() {
        if ( buckets.size() > 1 )
            return "Delete Buckets";
        else
            return "Delete Bucket";
    }

    private Dialog newConfirmationDialog(String title, String message) {
        return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.QUESTION, new String[] {"No", "Yes"}, 1);
    }

    @Override
    protected void doRun() {
        Dialog dialog = newConfirmationDialog(getText() + "?", "Are you sure you want to delete the selected buckets?");

        if (dialog.open() != 1) {
            actionCanceled();
            actionFinished();
            return;
        }

        new Job("Deleting Buckets") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    ObjectUtils objectUtils = new ObjectUtils();

                    for ( Bucket bucket : buckets ) {
                        objectUtils.deleteBucketAndAllVersions(bucket.getName());
                    }

                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            S3ContentProvider.getInstance().refresh();
                        }
                    });

                    actionSucceeded();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    actionFailed();
                    return new Status(IStatus.ERROR, AwsToolkitCore.getDefault().getPluginId(),
                        "Unable to delete buckets: " + e.getMessage(), e);
                } finally {
                    actionFinished();
                }
            }
        }.schedule();
    }
}
