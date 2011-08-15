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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.s3.BucketEditor;
import com.amazonaws.eclipse.explorer.s3.BucketEditorInput;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * Handles dropping a resource into a bucket, uploading it.
 */
public class UploadDropAssistant extends CommonDropAdapterAssistant {

    public UploadDropAssistant() {
    }

    @Override
    public IStatus validateDrop(final Object target, final int operation, final TransferData transferType) {
        if ( target instanceof Bucket ) {
            return Status.OK_STATUS;
        } else {
            return Status.CANCEL_STATUS;
        }
    }

    @Override
    public boolean isSupportedType(TransferData aTransferType) {
        return LocalSelectionTransfer.getTransfer().isSupportedType(aTransferType)
                || FileTransfer.getInstance().isSupportedType(aTransferType);
    }

    @Override
    public IStatus handleDrop(CommonDropAdapter aDropAdapter, final DropTargetEvent aDropTargetEvent, Object aTarget) {

        final Bucket bucket = (Bucket) aTarget;

        File f = getFileToDrop(aDropAdapter.getCurrentTransfer());

        if ( f == null || !f.exists() )
            return Status.CANCEL_STATUS;

        KeySelectionDialog dialog = new KeySelectionDialog(aDropTargetEvent.display.getActiveShell(), f);
        if ( dialog.open() != 0 ) {
            return Status.CANCEL_STATUS;
        }
        final String keyName = dialog.getKeyName();

        final TransferManager transferManager = new TransferManager(AwsToolkitCore.getClientFactory().getS3Client());
            UploadFileJob uploadFileJob = new UploadFileJob("Uploading " + f.getAbsolutePath().toString(),
                    bucket.getName(), f, keyName, transferManager);

        uploadFileJob.setRefreshRunnable(new Runnable() {

            public void run() {
                try {
                    for ( IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().getEditorReferences() ) {
                        if ( ref.getEditorInput() instanceof BucketEditorInput ) {
                            if ( bucket.getName()
                                    .equals(((BucketEditorInput) ref.getEditorInput()).getBucketName()) ) {
                                BucketEditor editor = (BucketEditor) ref.getEditor(false);
                                editor.getObjectSummaryTable().refresh(null);
                                return;
                            }
                        }

                    }
                } catch ( PartInitException e ) {
                    AwsToolkitCore.getDefault().logException("Unable to open the Amazon S3 bucket editor: ", e);
                }
            }
        });

        uploadFileJob.schedule();

        return Status.OK_STATUS;
    }

    public static File getFileToDrop(TransferData transfer) {
        File f = null;
        if ( LocalSelectionTransfer.getTransfer().isSupportedType(transfer) ) {
            IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().nativeToJava(
                    transfer);
            IResource resource = (IResource) selection.getFirstElement();
            f = resource.getLocation().toFile();
        } else if ( FileTransfer.getInstance().isSupportedType(transfer) ) {
            String[] files = (String[]) FileTransfer.getInstance().nativeToJava(transfer);
            f = new File(files[0]);
        }
        return f;
    }

}
