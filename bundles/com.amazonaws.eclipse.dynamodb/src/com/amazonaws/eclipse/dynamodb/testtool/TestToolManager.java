/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.testtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.preferences.TestToolPreferencePage;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolVersion.InstallState;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * The singleton manager for DynamoDB Local Test Tool instances.
 */
public class TestToolManager {

    public static final TestToolManager INSTANCE = new TestToolManager();

    private static final String TEST_TOOL_BUCKET =
        "aws-toolkits-dynamodb-local";

    private static final String TEST_TOOL_MANIFEST = "manifest.xml";

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Set<String> installing =
        Collections.synchronizedSet(new HashSet<String>());

    private TransferManager transferManager;
    private List<TestToolVersion> versions;

    private TestToolVersion currentlyRunningVersion;
    private TestToolProcess currentProcess;

    private TestToolManager() {
    }

    /**
     * Get a list of all versions of the DynamoDB Local Test Tool which can
     * be installed on the system. The first time this method is called, it
     * attempts to pull the list of versions from S3 (falling back to an
     * on-disk cache in case of failure). On subsequent calls it returns the
     * in-memory cache (updated to reflect the current state of what is and
     * is not installed).
     *
     * @return the list of all local test tool versions
     */
    public synchronized List<TestToolVersion> getAllVersions() {
        if (versions == null) {
            versions = loadVersions();
        } else {
            versions = refreshInstallStates(versions);
        }
        return versions;
    }

    /**
     * Determines whether a Java 7 compatible JRE exists on the system.
     *
     * @return true if a Java 7 compatible JRE is found, false otherwise
     */
    public boolean isJava7Available() {
        return (getJava7VM() != null);
    }

    /**
     * Gets a Java 7 compatible VM, if one can be found.
     *
     * @return  the VM install, or null if none was found
     */
    private IVMInstall getJava7VM() {
        IExecutionEnvironmentsManager manager =
            JavaRuntime.getExecutionEnvironmentsManager();

        IExecutionEnvironment environment =
            manager.getEnvironment("JavaSE-1.8");
        if (environment == null) {
            // This version of Eclipse doesn't even know that Java 7 exists.
            return null;
        }

        IVMInstall defaultVM = environment.getDefaultVM();
        if (defaultVM != null) {
            // If the user has set a default VM to use for Java 7, go with
            // that.
            return defaultVM;
        }

        IVMInstall[] installs = environment.getCompatibleVMs();
        if (installs != null && installs.length > 0) {
            // Otherwise just pick the latest compatible VM.
            return installs[installs.length - 1];
        }

        // No compatible VMs installed.
        return null;
    }

    /**
     * Set the install state of the given instance to INSTALLING, so that
     * we disable both the install and uninstall buttons in the UI.
     *
     * @param version The version to mark as INSTALLING.
     */
    public synchronized void markInstalling(final TestToolVersion version) {
        if (!version.isInstalled()) {
            installing.add(version.getName());
        }
    }

    /**
     * Install the given version of the test tool.
     *
     * @param version The version of the test tool to install.
     * @param monitor A progress monitor to keep updated.
     */
    public void installVersion(final TestToolVersion version,
                               final IProgressMonitor monitor) {

        if (version.isInstalled()) {
            return;
        }

        try {

            File tempFile = File.createTempFile("dynamodb_local_", "");
            tempFile.delete();
            if (!tempFile.mkdirs()) {
                throw new RuntimeException("Failed to create temporary "
                                           + "directory for download");
            }

            File zipFile = new File(tempFile, "dynamodb_local.zip");

            download(version.getDownloadKey(), zipFile, monitor);

            File unzipped = new File(tempFile, "unzipped");
            if (!unzipped.mkdirs()) {
                throw new RuntimeException("Failed to create temporary "
                                           + "directory for unzipping");
            }

            unzip(zipFile, unzipped);

            File versionDir = getVersionDirectory(version.getName());
            FileUtils.copyDirectory(unzipped, versionDir);

        } catch (IOException exception) {
            throw new RuntimeException(
                "Error installing DynamoDB Local: " + exception.getMessage(),
                exception
            );
        } finally {
            monitor.done();
            installing.remove(version.getName());
        }
    }

    /**
     * @return  true if a local test tool process is currently running
     */
    public synchronized boolean isRunning() {
        return (currentProcess != null);
    }

    /**
     * @return  the port to which the current process is bound (or null if no
     *          process is currently running)
     */
    public synchronized Integer getCurrentPort() {
        if (currentProcess == null) {
            return null;
        }
        return currentProcess.getPort();
    }

    /**
     * Start the given version of the DynamoDBLocal test tool.
     *
     * @param version   the version of the test tool to start
     * @param port      the port to bind to
     */
    public synchronized void startVersion(final TestToolVersion version,
                                          final int port) {

        if (!version.isInstalled()) {
            throw new IllegalStateException("Cannot start a version which is "
                                            + "not installed.");
        }

        // We should have cleaned this up already, but just to be safe...
        if (currentProcess != null) {
            currentProcess.stop();
            currentProcess = null;
            currentlyRunningVersion = null;
        }

        IVMInstall jre = getJava7VM();
        if (jre == null) {
            throw new IllegalStateException("No Java 7 VM found!");
        }

        try {

            File installDirectory = getVersionDirectory(version.getName());
            final TestToolProcess process =
                new TestToolProcess(jre, installDirectory, port);

            currentProcess = process;
            currentlyRunningVersion = version;

            RegionUtils.addLocalService(ServiceAbbreviations.DYNAMODB,
                                        "dynamodb",
                                        port);

            // If the process dies for some reason other than that we killed
            // it, clear out our internal state so the user can start another
            // instance.
            process.start(new Runnable() {
                @Override
                public void run() {
                    synchronized (TestToolManager.this) {
                        if (process == currentProcess) {
                            cleanUpProcess();
                        }
                    }
                }
            });

        } catch (IOException exception) {
            throw new RuntimeException(
                "Error starting the DynamoDB Local Test Tool: "
                    + exception.getMessage(),
                exception
            );
        }

    }

    /**
     * Stop the currently-running DynamoDBLocal process.
     */
    public synchronized void stopVersion() {
        if (currentProcess != null) {
            currentProcess.stop();
            cleanUpProcess();
        }
    }

    private void cleanUpProcess() {
        currentProcess = null;
        currentlyRunningVersion = null;

        // Revert to a default port setting.

        DynamoDBPlugin.getDefault().setDefaultDynamoDBLocalPort();
    }

    /**
     * Download the given object from S3 to the given file, updating the
     * given progress monitor periodically.
     *
     * @param key The key of the object to download.
     * @param destination The destination file to download to.
     * @param monitor The progress monitor to update.
     */
    private void download(final String key,
                          final File destination,
                          final IProgressMonitor monitor) {
        try {

            TransferManager tm = getTransferManager();
            Download download = tm.download(
                TEST_TOOL_BUCKET,
                key,
                destination
            );

            int totalWork =
                (int) download.getProgress().getTotalBytesToTransfer();
            monitor.beginTask("Downloading DynamoDB Local", totalWork);

            int worked = 0;
            while (!download.isDone()) {
                int bytes = (int) download.getProgress().getBytesTransferred();
                if (bytes > worked) {
                    int newWork = bytes - worked;
                    monitor.worked(newWork);
                    worked = bytes;
                }
                Thread.sleep(500);
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Interrupted while installing DynamoDB Local",
                exception
            );
        } catch (AmazonServiceException exception) {
            throw new RuntimeException(
                "Error downloading DynamoDB Local: " + exception.getMessage(),
                exception
            );
        }
    }

    /**
     * Unzip the given file into the given directory.
     *
     * @param zipFile The zip file to unzip.
     * @param unzipped The directory to put the unzipped files into.
     * @throws IOException on file system error.
     */
    private void unzip(final File zipFile, final File unzipped)
            throws IOException {

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile))) {

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path path = new Path(entry.getName());

                File dest = new File(unzipped, path.toOSString());
                if (entry.isDirectory()) {
                    if (!dest.mkdirs()) {
                        throw new RuntimeException(
                            "Failed to create directory while unzipping"
                        );
                    }
                } else {
                    try (FileOutputStream output = new FileOutputStream(dest)) {
                        IOUtils.copy(zip, output);
                    }
                }
            }
        }
    }


    /**
     * Uninstall the given version of the test tool.
     *
     * @param version The version to uninstall.
     */
    public void uninstallVersion(final TestToolVersion version) {
        if (!version.isInstalled()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(
                getVersionDirectory(version.getName())
            );
        } catch (IOException exception) {
            throw new RuntimeException(
                "Error while uninstalling DynamoDB Local: "
                + exception.getMessage(),
                exception
            );
        }
    }


    /**
     * Load the set of test tool versions from S3, falling back to a cache
     * on the local disk in case of error.
     *
     * @return The loaded test tool version list.
     */
    private List<TestToolVersion> loadVersions() {
        try {

            return loadVersionsFromS3();

        } catch (IOException exception) {
            try {

                return loadVersionsFromLocalCache();

            } catch (IOException e) {
                // No local cache; throw the original exception.
                throw new RuntimeException(
                    "Error loading DynamoDB Local Test Tool version manifest "
                    + "from Amazon S3. Are you connected to the Internet?",
                    exception
                );
            }
        }
    }

    /**
     * Loop through the existing set of test tool versions and build a new
     * list with install states updated to reflect what's actually on disk.
     *
     * @param previous The existing list of versions.
     * @return The updated list of versions.
     */
    private List<TestToolVersion> refreshInstallStates(
        final List<TestToolVersion> previous
    ) {
        List<TestToolVersion> rval =
            new ArrayList<>(previous.size());

        for (TestToolVersion version : previous) {
            InstallState installState = getInstallState(version.getName());

            if (currentlyRunningVersion != null
                && currentlyRunningVersion.getName()
                            .equals(version.getName())) {
                installState = InstallState.RUNNING;
            } else if (installing.contains(version.getName())) {
                installState = InstallState.INSTALLING;
            }

            if (installState == version.getInstallState()) {
                rval.add(version);
            } else {
                rval.add(new TestToolVersion(
                    version.getName(),
                    version.getDescription(),
                    version.getDownloadKey(),
                    installState
                ));
            }
        }

        return rval;
    }

    /**
     * Attempt to load the manifest file describing available versions of the
     * test tool from S3. On success, update our local cache of the manifest
     * file.
     *
     * @return The list of versions.
     * @throws IOException on error.
     */
    private List<TestToolVersion> loadVersionsFromS3()
            throws IOException {

        File tempFile =
            File.createTempFile("dynamodb_local_manifest_", ".xml");
        tempFile.delete();

        TransferManager manager = getTransferManager();
        try {
            Download download = manager.download(
                TEST_TOOL_BUCKET,
                TEST_TOOL_MANIFEST,
                tempFile
            );

            download.waitForCompletion();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Interrupted while downloading DynamoDB Local version manifest "
                + "from S3",
                exception
            );
        } catch (AmazonServiceException exception) {
            throw new IOException("Error downloading DynamoDB Local manifest "
                                  + "from S3",
                                  exception);
        }

        List<TestToolVersion> rval = parseManifest(tempFile);

        try {
            FileUtils.copyFile(tempFile, getLocalManifestFile());
        } catch (IOException exception) {
            AwsToolkitCore.getDefault().logError(
                "Error caching manifest file to local disk; do you have "
                + "write permission to the configured install directory? "
                + exception.getMessage(),
                exception);
        }

        return rval;
    }

    /**
     * Attempt to load the list of test tool versions from local cache.
     *
     * @return The list of test tool versions.
     * @throws IOException on error.
     */
    private List<TestToolVersion> loadVersionsFromLocalCache()
            throws IOException {

        return parseManifest(getLocalManifestFile());
    }

    /**
     * Parse a manifest file describing a list of test tool versions.
     *
     * @param file The file to parse.
     * @return The parsed list of versions.
     * @throws IOException on error.
     */
    private List<TestToolVersion> parseManifest(final File file)
            throws IOException {

        try (FileInputStream stream = new FileInputStream(file)) {

            BufferedReader buffer = new BufferedReader(
                new InputStreamReader(stream)
            );

            ManifestContentHandler handler = new ManifestContentHandler();

            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(buffer));

            return handler.getResult();

        } catch (SAXException exception) {
            throw new IOException("Error parsing DynamoDB Local manifest file",
                                  exception);
        }
    }

    /**
     * Lazily initialize a transfer manager; keep it around to reuse in the
     * future if need be.
     *
     * @return The transfer manager.
     */
    private synchronized TransferManager getTransferManager() {
        if (transferManager == null) {
            AmazonS3 client = AWSClientFactory.getAnonymousS3Client();

            transferManager = new TransferManager(client);
        }
        return transferManager;
    }

    /**
     * @return The path to the file where we'll store the local manifest cache.
     * @throws IOException on error.
     */
    private static File getLocalManifestFile() throws IOException {
        return new File(getInstallDirectory(), "manifest.xml");
    }

    /**
     * Get the install state of a particular version by looking for the
     * presence of the DynamoDBLocal.jar file in the corresponding directory.
     *
     * @param version The version to check for.
     * @return The install state of the given version.
     */
    private static InstallState getInstallState(final String version) {
        try {

            File versionDir = getVersionDirectory(version);

            if (!versionDir.exists()) {
                return InstallState.NOT_INSTALLED;
            }
            if (!versionDir.isDirectory()) {
                return InstallState.NOT_INSTALLED;
            }

            File jar = new File(versionDir, "DynamoDBLocal.jar");
            if (!jar.exists()) {
                return InstallState.NOT_INSTALLED;
            }

            return InstallState.INSTALLED;

        } catch (IOException exception) {
            return InstallState.NOT_INSTALLED;
        }
    }

    /**
     * Get the path to the directory where we would install the given version
     * of the test tool.
     *
     * @param version The version in question.
     * @return The path to the install directory.
     * @throws IOException on error.
     */
    private static File getVersionDirectory(final String version)
            throws IOException {

        return new File(getInstallDirectory(), version);
    }

    /**
     * Get the path to the root install directory as configured in the test
     * tool preference page.
     *
     * @return The path to the root install directory.
     * @throws IOException on error.
     */
    private static File getInstallDirectory() throws IOException {
        IPreferenceStore preferences =
            DynamoDBPlugin.getDefault().getPreferenceStore();

        String directory = preferences.getString(
            TestToolPreferencePage.DOWNLOAD_DIRECTORY_PREFERENCE_NAME
        );

        File installDir = new File(directory);

        if (!installDir.exists()) {
            if (!installDir.mkdirs()) {
                throw new IOException("Could not create install directory: "
                                      + installDir.getAbsolutePath());
            }
        } else {
            if (!installDir.isDirectory()) {
                throw new IOException("Configured install directory is "
                                      + "not a directory: "
                                      + installDir.getAbsolutePath());
            }
        }

        return installDir;
    }

    /**
     * SAX handler for the local test tool manifest format.
     */
    private static class ManifestContentHandler extends DefaultHandler {
        private final List<TestToolVersion> versions =
            new ArrayList<>();

        private StringBuilder currText = new StringBuilder();

        private String name;
        private String description;
        private String downloadKey;

        /**
         * @return The loaded list of versions.
         */
        public List<TestToolVersion> getResult() {
            return versions;
        }

        @Override
        public void startElement(final String uri,
                                 final String localName,
                                 final String qName,
                                 final Attributes attributes) {

            if (localName.equals("version")) {
                // Null these out to be safe.
                name = null;
                description = null;
                downloadKey = null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri,
                               final String localName,
                               final String qName) {

            if (localName.equals("name")) {
                name = trim(currText.toString());
                currText = new StringBuilder();
            } else if (localName.equals("description")) {
                description = trim(currText.toString());
                currText = new StringBuilder();
            } else if (localName.equals("key")) {
                downloadKey = trim(currText.toString());
                currText = new StringBuilder();
            } else if (localName.equals("version")) {
                if (name != null || downloadKey != null) {
                    // Skip versions with no name or download key to be safe.
                    versions.add(new TestToolVersion(
                        name,
                        description,
                        downloadKey,
                        getInstallState(name)
                    ));
                }
                name = null;
                description = null;
                downloadKey = null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch,
                               final int start,
                               final int length) {

            currText.append(ch, start, length);
        }

        /**
         * Trim excess whitespace out of the given string.
         *
         * @param value The value to trim.
         * @return The trimmed value.
         */
        private String trim(final String value) {
            return WHITESPACE.matcher(value.trim()).replaceAll(" ");
        }
    }

}
