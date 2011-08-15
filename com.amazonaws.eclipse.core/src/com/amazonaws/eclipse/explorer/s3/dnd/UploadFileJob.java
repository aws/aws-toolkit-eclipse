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
package com.amazonaws.eclipse.explorer.s3.dnd;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;

/**
 * Background job to upload a file to S3
 */
public class UploadFileJob extends Job {

    private final String bucketName;
    private final File toUpload;
    private final String keyName;
    private final TransferManager transferManager;

    private Runnable refreshRunnable;

    public Runnable getRefreshRunnable() {
        return refreshRunnable;
    }

    /**
     * Sets a runnable to refresh a UI element after the upload has been
     * complete.
     */
    public void setRefreshRunnable(Runnable refreshRunnable) {
        this.refreshRunnable = refreshRunnable;
    }

    public UploadFileJob(String name, String bucketName, File toUpload, String keyName,
            TransferManager transferManager) {
        super(name);
        this.bucketName = bucketName;
        this.toUpload = toUpload;
        this.keyName = keyName;
        this.transferManager = transferManager;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Upload", 100);
        int percentTransfered = 0;
        final Upload upload = transferManager.upload(bucketName, keyName, toUpload);
        while ( !upload.isDone() ) {
            int progress = (int) upload.getProgress().getPercentTransfered();
            if ( progress > percentTransfered ) {
                monitor.worked(progress - percentTransfered);
                percentTransfered = progress;
            }
            try {
                Thread.sleep(500);
            } catch ( InterruptedException ignored ) {
            }
        }
        monitor.done();
        if ( !upload.getState().equals(TransferState.Completed) ) {
            Exception e = null;
            try {
                e = upload.waitForException();
            } catch ( InterruptedException ie ) {
                e = ie;
            }
            if ( e == null ) {
                e = new RuntimeException("Unhandled exception");
            }
            return new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, e.getMessage(), e);
        }

        if ( getRefreshRunnable() != null ) {
            Display.getDefault().syncExec(getRefreshRunnable());
        }

        return Status.OK_STATUS;
    }
}