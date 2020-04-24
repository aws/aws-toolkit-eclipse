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
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.s3.S3ObjectSummaryTable;
import com.amazonaws.eclipse.explorer.s3.acls.EditObjectPermissionsDialog;
import com.amazonaws.eclipse.explorer.s3.acls.EditPermissionsDialog;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class EditObjectPermissionsAction extends AwsAction {
    private final S3ObjectSummaryTable table;

    public EditObjectPermissionsAction(S3ObjectSummaryTable s3ObjectSummaryTable) {
        super(AwsToolkitMetricType.EXPLORER_S3_EDIT_OBJECT_PERMISSIONS);
        this.table = s3ObjectSummaryTable;
        setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_WRENCH));
        setText("Edit Permissions");
    }

    @Override
    public boolean isEnabled() {
        return table.getSelectedObjects().size() > 0;
    }

    @Override
    protected void doRun() {
        final Collection<S3ObjectSummary> selectedObjects = table.getSelectedObjects();

        final EditPermissionsDialog editPermissionsDialog = new EditObjectPermissionsDialog(selectedObjects);
        if (editPermissionsDialog.open() == Window.OK) {
            final AmazonS3 s3 = table.getS3Client();
            new Job("Updating object ACLs") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        AccessControlList newAcl = editPermissionsDialog.getAccessControlList();

                        Iterator<S3ObjectSummary> iterator = selectedObjects.iterator();
                        while (iterator.hasNext()) {
                            S3ObjectSummary obj = iterator.next();
                            s3.setObjectAcl(obj.getBucketName(), obj.getKey(), newAcl);
                        }
                        actionSucceeded();
                    } catch (AmazonClientException ace) {
                        actionFailed();
                        AwsToolkitCore.getDefault().reportException("Unable to update object ACL", ace);
                    } finally {
                        actionFinished();
                    }

                    return Status.OK_STATUS;
                }
            }.schedule();
        } else {
            actionCanceled();
            actionFinished();
        }
    }
}
