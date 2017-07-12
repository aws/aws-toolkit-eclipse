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
package com.amazonaws.eclipse.explorer.s3.dnd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressEventType;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * Background job to upload files to S3
 */
public class UploadFilesJob extends Job {

    private final String bucketName;
    private final File[] filesToUpload;
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

    public UploadFilesJob(String name, String bucketName, File[] toUpload, TransferManager transferManager) {
        super(name);
        this.bucketName = bucketName;
        this.filesToUpload = toUpload;
        this.transferManager = transferManager;
        this.setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        List<KeyFilePair> pairSet = getActualFilesToUpload();
        int totalFilesToUpload = pairSet.size();
        monitor.beginTask(String.format("Uploading %d files to Amazon S3!", totalFilesToUpload), 100 * totalFilesToUpload);
        List<IStatus> errorStatuses = new ArrayList<>();
        int uploadedFiles = 0;
        for (KeyFilePair pair : pairSet) {
            String name = pair.keyName;
            monitor.setTaskName(String.format("%d/%d uploaded! Uploading %s!", uploadedFiles++, totalFilesToUpload, name));
            doUpload(name, pair.file, errorStatuses, monitor);
        }
        monitor.done();
        if (!errorStatuses.isEmpty()) {
            String errorMessages = aggregateErrorMessages(errorStatuses);
            AwsToolkitCore.getDefault().reportException(errorMessages, null);
        }
        return Status.OK_STATUS;
    }

    private String aggregateErrorMessages(List<IStatus> statuses) {
        StringBuilder builder = new StringBuilder();
        for (IStatus status : statuses) {
            builder.append(status.getMessage() + "\n");
        }
        return builder.toString();
    }

    private List<KeyFilePair> getActualFilesToUpload() {
        List<KeyFilePair> pairSet = new ArrayList<>();
        for (File file : filesToUpload) {
            putFilesToList(null, file, pairSet);
        }
        return pairSet;
    }

    private void putFilesToList(String prefix, File file, List<KeyFilePair> pairSet) {
        String keyName = prefix == null ? file.getName() : prefix + "/" + file.getName();
        if (file.exists() && file.isFile()) {
            pairSet.add(new KeyFilePair(keyName, file));
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File subFile : files) {
                putFilesToList(keyName, subFile, pairSet);
            }
        }
    }

    private void doUpload(final String keyName, final File file, List<IStatus> statuses, final IProgressMonitor monitor) {
        final Upload upload = transferManager.upload(bucketName, keyName, file);
        upload.addProgressListener(new ProgressListener(){
            private final long totalBytes = file.length();
            private long transferredBytes = 0;
            private int percentTransferred = 0;
            @Override
            public void progressChanged(ProgressEvent event) {
                ProgressEventType type = event.getEventType();
                if (type == ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT) {
                    transferredBytes += event.getBytesTransferred();
                    int progress = Math.min((int) (100 * transferredBytes / totalBytes), 100);
                    if (progress <= percentTransferred) {
                        return;
                    }
                    monitor.worked(progress - percentTransferred);
                    percentTransferred = progress;
                } else if (type == ProgressEventType.TRANSFER_COMPLETED_EVENT
                        || type == ProgressEventType.TRANSFER_CANCELED_EVENT
                        || type == ProgressEventType.TRANSFER_FAILED_EVENT) {
                    if (percentTransferred < 100) {
                        monitor.worked(100 - percentTransferred);
                    }
                }
            }
        });

        try {
            upload.waitForCompletion();
        } catch (Exception e) {
            statuses.add(new Status(IStatus.ERROR, AwsToolkitCore.getDefault().getPluginId(),
                    String.format("Error uploading %s: %s", keyName, e.getMessage())));
        }

        if ( getRefreshRunnable() != null ) {
            Display.getDefault().syncExec(getRefreshRunnable());
        }
    }

    private static class KeyFilePair {
        String keyName;
        File file;
        public KeyFilePair(String keyName, File file) {
            this.keyName = keyName;
            this.file = file;
        }
    }
}
