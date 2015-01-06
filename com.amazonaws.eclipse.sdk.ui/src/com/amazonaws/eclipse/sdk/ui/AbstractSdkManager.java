/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.sdk.ui;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * Abstract base class for managing installs of AWS SDKs. Concrete subclasses
 * add support for each specific SDK.
 */
public abstract class AbstractSdkManager<X extends AbstractSdkInstall> {

    private SdkDownloadJob installationJob = null;
    private String sdkBucketName;
    private String sdkFilenamePrefix;
    private String initializingSdkJobName;
    private Pattern sdkFilenameVersionPattern;

    private final SdkInstallFactory<X> sdkInstallFactory;


    /**
     * Constructs a new SDK manager that can be used to list SDK installs and
     * install new versions.
     *
     * @param sdkName
     *            The public name of the SDK this SDK manager works with.
     * @param sdkBucketName
     *            The name of the Amazon S3 bucket where SDK versions are
     *            stored.
     * @param sdkFilenamePrefix
     *            The filename prefix (without the version component) of the SDK
     *            files in S3. For example, 'aws-java-sdk'.
     */
    public AbstractSdkManager(String sdkName, String sdkBucketName, String sdkFilenamePrefix, SdkInstallFactory<X> sdkInstallFactory) {
        this.sdkInstallFactory = sdkInstallFactory;
        if (sdkName == null) throw new IllegalArgumentException("No SDK name specified");
        if (sdkFilenamePrefix == null) throw new IllegalArgumentException("No SDK filename prefix specified");

        this.sdkFilenamePrefix = sdkFilenamePrefix;
        sdkFilenameVersionPattern = Pattern.compile("filename\\s*=\\s*" + sdkFilenamePrefix + "-(.*?)\\.zip");
        initializingSdkJobName = "Initializing " + sdkName;
        this.sdkBucketName = sdkBucketName;
    }


    /**
     * Returns the SDK installation of the specified version, or
     * <code>null</code> if no such installation exists.
     *
     * @param version
     *            The version of the SDK to return.
     *
     * @return The SDK installation of the specified version, or
     *         <code>null</code> if no such installation exists.
     */
    public AbstractSdkInstall getSdkInstall(String version) {
        for ( AbstractSdkInstall sdkInstall : getSdkInstalls() ) {
            if ( sdkInstall.getVersion().equals(version) ) {
                return sdkInstall;
            }
        }

        return null;
    }

    /**
     * Returns a list of all the existing SDK installations.
     */
    public List<X> getSdkInstalls() {
        List<X> sdkInstalls = new LinkedList<X>();

        try {
            File sdkDir = getSDKInstallDir();
            if ( sdkDir.exists() && sdkDir.isDirectory() ) {
                for ( File versionDir : sdkDir.listFiles() ) {
                    X sdkInstall = sdkInstallFactory.createSdkInstallFromDisk(versionDir);
                    if ( sdkInstall.isValidSdkInstall() )
                        sdkInstalls.add(sdkInstall);
                }
            }

            return sdkInstalls;
        } catch ( IllegalStateException e ) {
            JavaSdkPlugin.getDefault().getLog()
                    .log(new Status(Status.WARNING, JavaSdkPlugin.PLUGIN_ID, "No state directory to cache SDK", e));
            return sdkInstalls;
        }
    }

    /**
     * Returns the default SDK install, which is the latest
     * available, or null if no SDKs are available yet.
     *
     * @return The default SDK install.
     */
    public X getDefaultSdkInstall() {
        List<X> sdkInstalls = getSdkInstalls();
        Collections.sort(sdkInstalls, new LatestVersionComparator());

        if (sdkInstalls.size() > 0) return sdkInstalls.get(0);

        return null;
    }

    /**
     * Copies the SDK given into the workspace's private state storage for this
     * plugin.
     */
    private void copySdk(AbstractSdkInstall install, IProgressMonitor monitor) {
        monitor.subTask("Copying SDK to workspace metadata");
        try {
            File sdkDir = getSDKInstallDir();
            File versionDir = new File(sdkDir, install.getVersion());

            if ( versionDir.exists() && sdkInstallFactory.createSdkInstallFromDisk(versionDir).isValidSdkInstall() )
                return;
            if ( !versionDir.exists() && !versionDir.mkdirs() )
                throw new Exception("Couldn't make SDK directory " + versionDir);

            FileUtils.copyDirectory(install.getRootDirectory(), versionDir);
            monitor.worked(20);
        } catch ( IllegalStateException e ) {
            JavaSdkPlugin.getDefault().getLog()
                    .log(new Status(Status.WARNING, JavaSdkPlugin.PLUGIN_ID, "No state directory to cache SDK", e));
        } catch ( Exception e ) {
            JavaSdkPlugin.getDefault().getLog().log(new Status(Status.ERROR, JavaSdkPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    /**
     * Returns the version number of the latest SDK in the publicly readable S3 bucket.
     */
    private String getLatestS3Version(IProgressMonitor monitor) {
        monitor.subTask("Checking latest version in S3");

        AmazonS3 client = AWSClientFactory.getAnonymousS3Client();
        ObjectMetadata objectMetadata = client.getObjectMetadata(sdkBucketName, "latest/" + sdkFilenamePrefix + ".zip");
        String filename = (String) objectMetadata.getRawMetadata().get(Headers.CONTENT_DISPOSITION);
        Matcher matcher = sdkFilenameVersionPattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1);
        }

        IStatus status = new Status(IStatus.ERROR, AwsToolkitCore.PLUGIN_ID, "Unable to detect latest plugin version (Content-Disposition: " + filename + ")");
        StatusManager.getManager().handle(status, StatusManager.LOG);
        return null;
    }

    /**
     * Comparator that sorts SDK installs from most recent version to oldest.
     */
    protected final class LatestVersionComparator implements Comparator<AbstractSdkInstall> {
        public int compare(AbstractSdkInstall left, AbstractSdkInstall right) {
            int[] leftVersion = parseVersion(left.getVersion());
            int[] rightVersion = parseVersion(right.getVersion());

            int min = Math.min(leftVersion.length, rightVersion.length);
            for (int i = 0; i < min; i++) {
                if (leftVersion[i] < rightVersion[i]) return 1;
                if (leftVersion[i] > rightVersion[i]) return -1;
            }

            return 0;
        }

        private int[] parseVersion(String version) {
            if (version == null) return new int[0];

            String[] components = version.split("\\.");
            int[] ints = new int[components.length];

            int counter = 0;
            for (String component : components) {
                ints[counter++] = Integer.parseInt(component);
            }

            return ints;
        }
    }

    /**
     * Returns the installation job if it exists, or null otherwise.
     */
    public SdkDownloadJob getInstallationJob() {
        return installationJob;
    }

    /**
     * Initializes the set of SDK installs
     */
    public void initializeSDKInstalls() {
        synchronized ( this ) {
            if ( installationJob != null )
                return;
            installationJob = new SdkDownloadJob();
        }

        installationJob.schedule();
    }

    public final class SdkDownloadJob extends Job {

        public SdkDownloadJob() {
            super(initializingSdkJobName);
            this.setUser(true);
            this.setProperty(IProgressConstants.ACTION_PROPERTY, getHyperlinkAction());
            this.setProperty(IProgressConstants.KEEPONE_PROPERTY, true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                monitor.beginTask("Updating SDK (click details to configure)", 101);
                String latestSdkVersion = getLatestS3Version(monitor);
                if ( getSdkInstall(latestSdkVersion) == null ) {
                    downloadSDK(monitor);
                }
            } catch ( Exception e ) {
                StatusManager.getManager().handle(
                        new Status(IStatus.ERROR, JavaSdkPlugin.PLUGIN_ID,
                                "Couldn't download latest SDK", e),
                                StatusManager.SHOW);
            } finally {
                monitor.done();
                synchronized ( AbstractSdkManager.this ) {
                    installationJob = null;
                }
            }
            return new Status(IStatus.OK, JavaSdkPlugin.PLUGIN_ID, "Click to configure");
        }


    }

    /**
     * Returns the action to associate with this job, which will be represented
     * by a clickable link.
     */
    protected abstract Action getHyperlinkAction();

    /**
     * Returns the default sdk install directory for the current workspace. With some
     * command-line arguments, this directory will not exist and cannot be
     * created, in which case an {@link IllegalStateException} is thrown.
     */
    public File getDefaultSDKInstallDir() throws IllegalStateException {
        File userHome = new File(System.getProperty("user.home"));
        return new File(userHome, sdkFilenamePrefix);
    }

    /**
     * Returns the base directory to install SDKs in
     */
    abstract protected File getSDKInstallDir();

    /**
     * Downloads the latest copy of the SDK from s3 and caches it in the workspace metadata directory
     */
    private void downloadSDK(IProgressMonitor monitor) throws IOException {
        AmazonS3 client = AWSClientFactory.getAnonymousS3Client();
        File tempFile = File.createTempFile(sdkFilenamePrefix, "");
        tempFile.delete();
        tempFile.mkdirs();
        File zipFile = new File(tempFile, "sdk.zip");

        TransferManager manager = new TransferManager(client);

        try {
            JavaSdkPlugin.getDefault().getLog()
                .log(new Status(Status.INFO, JavaSdkPlugin.PLUGIN_ID,
                        "Downloading the SDK to temporary location " + zipFile.getAbsolutePath()));

            Download download = manager.download(sdkBucketName, "latest/" + sdkFilenamePrefix + ".zip", zipFile);

            // 30 units of work to download the zip
            monitor.subTask("Downloading latest SDK");
            int worked = 0;
            int unitWorkInBytes = (int) (download.getProgress().getTotalBytesToTransfer() / 30);

            while ( !download.isDone() ) {
                if ( download.getProgress().getBytesTransferred() / unitWorkInBytes > worked ) {
                    int newWork = (int) (download.getProgress().getBytesTransferred() / unitWorkInBytes) - worked;
                    monitor.worked(newWork);
                    worked += newWork;
                }
            }
            if ( worked < 30 )
                monitor.worked(30 - worked);

            AmazonClientException ace;
            try {
                if ( (ace = download.waitForException()) != null) {
                    throw ace;
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(
                        "The SDK download should have already been completed " +
                        "when waitForException was called, and the method should always " +
                        "directly return.",
                        e);
            }

            if ( !zipFile.exists() ) {
                throw new IllegalStateException(
                        zipFile.getAbsolutePath() + " does not exist " +
                        "after the SDK download completes.");
            }

            JavaSdkPlugin.getDefault().getLog()
                .log(new Status(Status.INFO, JavaSdkPlugin.PLUGIN_ID,
                    "SDK download completes. Location: " + zipFile.getAbsolutePath() + ", " +
                    "File-length: " + zipFile.length()));


            // Unzip the sdk.zip file
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));

            // 50 units of work to unzip the sdk
            monitor.subTask("Extracting SDK to workspace metadata directory");
            File temp = new File(tempFile, "unzipped");
            worked = 0;
            long totalSize = zipFile.length();
            long totalUnzipped = 0;
            unitWorkInBytes = (int) (totalSize / 50);
            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                IPath path = new Path(zipEntry.getName());

                File destinationFile = new File(temp, path.toOSString());
                if ( zipEntry.isDirectory() ) {
                    destinationFile.mkdirs();
                } else {
                    long compressedSize = zipEntry.getCompressedSize();

                    FileOutputStream outputStream = new FileOutputStream(destinationFile);
                    try {
                        IOUtils.copy(zipInputStream, outputStream);
                    } catch (EOFException eof) {
                        /*
                         * There is a bug in ZipInputStream, where it might
                         * incorrectly throw EOFException if the read exceeds
                         * the current zip-entry size.
                         *
                         * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6519463
                         */
                        JavaSdkPlugin.getDefault().getLog()
                            .log(new Status(
                                    Status.WARNING, JavaSdkPlugin.PLUGIN_ID,
                                    "Ignore EOFException when unpacking zip-entry " +
                                            zipEntry.getName(),
                                    eof));
                    }
                    outputStream.close();

                    totalUnzipped += compressedSize;
                    if (totalUnzipped / unitWorkInBytes > worked) {
                        int newWork = (int) (totalUnzipped / unitWorkInBytes) - worked;
                        monitor.worked(newWork);
                        worked += newWork;
                    }
                }
            }

            zipInputStream.close();

            File sdkDir = temp.listFiles()[0];
            AbstractSdkInstall latest = sdkInstallFactory.createSdkInstallFromDisk(sdkDir);

            copySdk(latest, monitor);

        } finally {

            // Leave the shared anonymous s3 client open
            manager.shutdownNow(false);
        }

    }

}