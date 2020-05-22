package com.amazonaws.eclipse.explorer.s3.dnd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.part.IDropActionDelegate;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3ObjectSummaryDropAction implements IDropActionDelegate {

    public static final String ID = "com.amazonaws.eclipse.explorer.s3.objectSummaryDropAction";

    @Override
    public boolean run(Object source, Object target) {

        BucketAndKey bk = new BucketAndKey((byte[]) source);

        IContainer dropFolder;
        if ( target instanceof IContainer ) {
            dropFolder = (IContainer) target;
        } else if ( target instanceof IFile ) {
            dropFolder = ((IFile) target).getParent();
        } else if ( target instanceof IJavaProject ) {
            dropFolder = ((IJavaProject) target).getProject();
        } else if ( target instanceof IJavaElement ) {
            IJavaElement j = (IJavaElement) target;
            try {
                return run(source, j.getUnderlyingResource());
            } catch ( JavaModelException e ) {
                AwsToolkitCore.getDefault().logError("Couldn't determine java resource", e);
                return false;
            }
        } else {
            return false;
        }

        final File f = dropFolder.getLocation().toFile();
        if ( !f.exists() )
            return false;

        String fileName = getOutputFileName(bk.key, f);

        if ( fileName == null || fileName.length() == 0 ) {
            return false;
        }

        final File outputFile = new File(fileName);

        new DownloadObjectJob("Downloading " + bk.key, bk.bucket, bk.key, dropFolder, outputFile).schedule();

        return true;
    }

    /**
     * Opens a save-file dialog to prompt the user for a file name.
     */
    private String getOutputFileName(final String key, final File f) {
        FileDialog dialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.SAVE);
        dialog.setFilterPath(f.getAbsolutePath());
        String keyToDisplay = key;
        if ( keyToDisplay.contains("/") ) {
            keyToDisplay = keyToDisplay.substring(keyToDisplay.lastIndexOf('/') + 1);
        }
        dialog.setFileName(keyToDisplay);
        dialog.setOverwrite(true);
        String fileName = dialog.open();
        return fileName;
    }

    private final class DownloadObjectJob extends Job {

        private final String key;
        private final String bucket;
        private final IResource dropFolder;
        private final File outputFile;

        private DownloadObjectJob(String name, String bucket, String key, IContainer dropFolder, File outputFile) {
            super(name);
            this.bucket = bucket;
            this.key = key;
            this.dropFolder = dropFolder;
            this.outputFile = outputFile;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            FileOutputStream fos = null;
            try {
                // TODO: this won't work if the current account doesn't have read permission for the bucket and key
                AmazonS3 client = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucket);
                S3Object object = client.getObject(bucket, key);

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

    /**
     * Encodes the object summary as a byte array.
     */
    public static byte[] encode(S3ObjectSummary s) {
        return BucketAndKey.encode(s);
    }

    static private class BucketAndKey {

        private String bucket;
        private String key;

        public static byte[] encode(S3ObjectSummary s) {
            StringBuilder b = new StringBuilder();
            b.append(s.getBucketName()).append('\t').append(s.getKey());
            return b.toString().getBytes();
        }

        public BucketAndKey(byte[] data) {
            String s = new String(data);
            int index = s.indexOf('\t');
            if ( index < 0 )
                throw new RuntimeException("Unable to decode bucket and key");
            bucket = s.substring(0, index);
            key = s.substring(index + 1);
        }

    }
}
