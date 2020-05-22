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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Handles dropping an S3 key onto a supported resource in the project explorer.
 */
public class DownloadDropAssistant extends CommonDropAdapterAssistant {

    public DownloadDropAssistant() {
    }

    /**
     * All validation is left to the configuration in plugin.xml. Any match
     * between possibleChildren and possibleDropTargets will be considered
     * valid.
     */
    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
        return validatePluginTransferDrop((IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection(), target);
    }

    @Override
    public IStatus validatePluginTransferDrop(IStructuredSelection aDragSelection, Object aDropTarget) {
        if (aDropTarget instanceof IResource)
            return Status.OK_STATUS;
        else
            return Status.CANCEL_STATUS;
    }

    @Override
    public IStatus handlePluginTransferDrop(IStructuredSelection aDragSelection, Object aDropTarget) {
        return doDrop(aDropTarget, aDragSelection);
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter aDropAdapter, final DropTargetEvent aDropTargetEvent, Object aTarget) {

        if ( aDropTargetEvent.data instanceof StructuredSelection ) {
            IStructuredSelection s3ObjectSelection = (StructuredSelection) aDropTargetEvent.data;
            return doDrop(aTarget, s3ObjectSelection);
        }

        return Status.OK_STATUS;
    }

    protected IStatus doDrop(Object aTarget, IStructuredSelection s3ObjectSelection) {
        if ( !(aTarget instanceof IResource) ) {
            return Status.CANCEL_STATUS;
        }

        // Drop targets can be folders, projects, or files. In the case of
        // files, we just want to identify the parent directory.
        IResource resource = (IResource) aTarget;
        if ( resource instanceof IFile ) {
            resource = resource.getParent();
        }

        final IResource dropFolder = resource;

        final S3ObjectSummary s3object = (S3ObjectSummary) s3ObjectSelection
                .getFirstElement();

        final File f = dropFolder.getLocation().toFile();
        if ( !f.exists() )
            return Status.CANCEL_STATUS;

        String fileName = getOutputFileName(s3object, f);

        if ( fileName == null || fileName.length() == 0 ) {
            return Status.CANCEL_STATUS;
        }

        final File outputFile = new File(fileName);

        new DownloadObjectJob("Downloading " + s3object.getKey(), s3object, dropFolder, outputFile).schedule();

        return Status.OK_STATUS;
    }

    /**
     * Opens a save-file dialog to prompt the user for a file name.
     */
    private String getOutputFileName(final S3ObjectSummary s3object, final File f) {
        FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
        dialog.setFilterPath(f.getAbsolutePath());
        dialog.setFileName(s3object.getKey());
        dialog.setOverwrite(true);
        String fileName = dialog.open();
        return fileName;
    }

    /**
     * Async job to download an object from S3
     */
    private final class DownloadObjectJob extends Job {

        private final S3ObjectSummary s3object;
        private final IResource dropFolder;
        private final File outputFile;

        private DownloadObjectJob(String name, S3ObjectSummary s3object, IResource dropFolder, File outputFile) {
            super(name);
            this.s3object = s3object;
            this.dropFolder = dropFolder;
            this.outputFile = outputFile;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            FileOutputStream fos = null;
            try {
                AmazonS3 client = AwsToolkitCore.getClientFactory().getS3ClientForBucket(s3object.getBucketName());
                S3Object object = client.getObject(s3object.getBucketName(), s3object.getKey());

                // This number is used for reporting only; the download
                // will appear to complete early if the file is bigger
                // than 2GB.
                long totalNumBytes = object.getObjectMetadata().getContentLength();
                if ( totalNumBytes > Integer.MAX_VALUE )
                    totalNumBytes = Integer.MAX_VALUE;
                monitor.beginTask("Downloading", (int) totalNumBytes);

                // For a new file this is a no-op, but it truncates an
                // existing file for overwrite.
                try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
                    raf.setLength(0);
                }

                fos = new FileOutputStream(outputFile);
                InputStream is = object.getObjectContent();
                byte[] buffer = new byte[4096];
                int bytesRead = 0;
                while ( (bytesRead = is.read(buffer)) > 0 ) {
                    fos.write(buffer, 0, bytesRead);
                    monitor.worked(bytesRead);
                }
            } catch ( Exception e ) {
                return new Status(Status.ERROR, AwsToolkitCore.getDefault().getPluginId(), "Error downloading file from S3", e);
            } finally {
                if ( fos != null ) {
                    try {
                        fos.close();
                    } catch ( Exception e ) {
                        AwsToolkitCore.getDefault().logError("Couldn't close file output stream", e);
                    }
                }
                monitor.done();
            }

            // Refresh the drop folder
            // TODO: this won't work if they chose another folder in the
            // file selection dialog.
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        dropFolder.refreshLocal(1, monitor);
                    } catch ( CoreException e ) {
                        AwsToolkitCore.getDefault().logError("Couldn't refresh local files", e);
                    }
                }
            });

            return Status.OK_STATUS;
        }
    }
}
