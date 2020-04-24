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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.S3ObjectSummaryTable;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class DeleteObjectAction extends AwsAction {

    private final S3ObjectSummaryTable table;

    public DeleteObjectAction(S3ObjectSummaryTable s3ObjectSummaryTable) {
        super(AwsToolkitMetricType.EXPLORER_S3_DELETE_OBJECTS);
        table = s3ObjectSummaryTable;
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor("remove"));
    }

    @Override
    public String getText() {
        if ( table.getSelectedObjects().size() > 1 )
            return "Delete Objects";
        else
            return "Delete Object";
    }

    private Dialog newConfirmationDialog(String title, String message) {
        return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.QUESTION, new String[] {"No", "Yes"}, 1);
    }

    @Override
    protected void doRun() {
        Dialog dialog = newConfirmationDialog(getText() + "?", "Are you sure you want to delete the selected objects?");
        if (dialog.open() != 1) {
            actionCanceled();
            actionFinished();
            return;
        }

        new Job("Deleting Objects") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final Collection<S3ObjectSummary> selectedObjects = new ArrayList<>();
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        selectedObjects.addAll(table.getSelectedObjects());
                    }
                });
                monitor.beginTask("Deleting objects", selectedObjects.size());

                try {
                    AmazonS3 s3 = table.getS3Client();
                    for ( S3ObjectSummary summary : selectedObjects ) {
                        s3.deleteObject(summary.getBucketName(), summary.getKey());
                        monitor.worked(1);
                    }

                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            String prefix = null;
                            if (selectedObjects.size() == 1) {
                                String key = selectedObjects.iterator().next().getKey();
                                int slashIndex = key.lastIndexOf('/');
                                if (slashIndex > 0) {
                                    prefix = key.substring(0, slashIndex);
                                }
                            }
                            table.refresh(prefix);
                        }
                    });
                    actionSucceeded();
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    actionFailed();
                    return new Status(IStatus.ERROR, AwsToolkitCore.getDefault().getPluginId(),
                        "Unable to delete objects: " + e.getMessage(), e);
                } finally {
                    actionFinished();
                    monitor.done();
                }
            }
        }.schedule();
    }

    @Override
    public boolean isEnabled() {
        return table.getSelectedObjects().size() > 0;
    }
}
