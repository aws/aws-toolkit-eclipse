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
package com.amazonaws.eclipse.elasticbeanstalk.git;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.util.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;

public class AWSGitPushCommand {

    private final File repoLocation;
    private final File archiveFile;

    private final Environment environment;
    private boolean skipEnvironmentDeployment;

    private final String accessKey;
    private final String secretKey;

    public AWSGitPushCommand(File repoLocation, File archiveFile, Environment environment, AWSCredentials credentials) {
        this.repoLocation = repoLocation;
        this.archiveFile = archiveFile;
        this.environment = environment;
        this.accessKey = credentials.getAWSAccessKeyId();
        this.secretKey = credentials.getAWSSecretKey();
    }

    private static final Logger log = Logger.getLogger(AWSGitPushCommand.class.getCanonicalName());

    public void execute() throws CoreException {
        Iterable<PushResult> pushResults = null;
        try {
            Repository repository = initializeRepository();

            // Add the new files
            clearOutRepository(repository);
            extractZipFile(archiveFile, repoLocation);
            commitChanges(repository, "Incremental Deployment: " + new Date().toString());

            // Push to AWS
            String remoteUrl = getRemoteUrl();

            if (log.isLoggable(Level.FINE)) log.fine("Pushing to: " + remoteUrl);
            PushCommand pushCommand = new Git(repository).push().setRemote(remoteUrl).setForce(true).add("master");
            // TODO: we could use a ProgressMonitor here for reporting status back to the UI
            pushResults = pushCommand.call();
        } catch (Throwable t) {
            throwCoreException(null, t);
        }

        for (PushResult pushResult : pushResults) {
            String messages = pushResult.getMessages();
            if (messages != null && messages.trim().length() > 0) {
                throwCoreException(messages, null);
            }
        }
    }

    /**
     * Use this method to configure this request to only create a new
     * application version, and not automatically deploy it to the specified
     * environment.
     *
     * @param skipEnvironmentDeployment
     *            True if this Git Push should only create a new application
     *            version, and not automatically deploy it to the specified
     *            environment.
     */
    public void skipEnvironmentDeployment(boolean skipEnvironmentDeployment) {
        this.skipEnvironmentDeployment = skipEnvironmentDeployment;
    }

    private void throwCoreException(String customMessage, Throwable t) throws CoreException {
        if (customMessage != null) customMessage = ": " + customMessage;
        else customMessage = "";

        String errorMessage = "Unable to update environment with an incremental deployment" + customMessage
           + "\nIf you continue having problems with incremental deployments, try turning off incremental deployments in the server configuration editor.";
        throw new CoreException(new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, t));
    }

    private String getRemoteUrl() throws Exception {
        AWSElasticBeanstalkGitPushRequest request = new AWSElasticBeanstalkGitPushRequest();
        request.setHost(getGitPushHost());

        Region region = RegionUtils.getRegionByEndpoint(environment.getRegionEndpoint());
        request.setRegion(region.getId());
        request.setApplication(environment.getApplicationName());

        if (!skipEnvironmentDeployment) {
            request.setEnvironment(environment.getEnvironmentName());
        }
        AWSGitPushAuth auth = new AWSGitPushAuth(request);
        URI uri = auth.deriveRemote(accessKey, secretKey);

        return uri.toString();
    }

    private String getGitPushHost() {
        // TODO: it would be better to get this from the endpoints file at some point
        String regionEndpoint = environment.getRegionEndpoint();
        if (regionEndpoint.startsWith("https://")) {
            regionEndpoint = "git." + regionEndpoint.substring("https://".length());
        } else if (regionEndpoint.startsWith("http://")) {
            regionEndpoint = "git." + regionEndpoint.substring("http://".length());
        } else {
            regionEndpoint = "git." + regionEndpoint;
        }

        if (regionEndpoint.endsWith("/")) {
            regionEndpoint = regionEndpoint.substring(0, regionEndpoint.length() - 1);
        }

        return regionEndpoint;
    }

    private void clearOutRepository(Repository repository) throws IOException {
        // Delete all the old files before copying the new files
        final File gitMetadataDirectory = repository.getIndexFile().getParentFile();

        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                if (file.equals(gitMetadataDirectory)) return false;
                if (!file.getParentFile().equals(repoLocation)) return false;
                return true;
            }
        };

        for (File f : repoLocation.listFiles(fileFilter)) {
            FileUtils.delete(f, FileUtils.RECURSIVE);
        }
    }

    private Repository initializeRepository() throws IOException {
        if (repoLocation == null) {
            throw new RuntimeException("No repository location specified");
        }

        if ((!repoLocation.exists() && !repoLocation.mkdirs()) ||
            !repoLocation.isDirectory()) {
            throw new RuntimeException("Unable to initialize Git repository from location: " + repoLocation);
        }

        Repository repository = new RepositoryBuilder().setWorkTree(repoLocation).build();
        if (!repository.getObjectDatabase().exists()) {
            repository.create();
        }

        return repository;
    }

    // TODO: We could use a better util for this
    private void extractZipFile(File zipFile, File destination) throws IOException {
        int BUFFER = 2048;

        FileInputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        BufferedOutputStream dest = null;
        try {
            ZipEntry entry;
            while((entry = zis.getNextEntry()) != null) {
               int count;
               byte data[] = new byte[BUFFER];
               // write the files to the disk

               if (entry.isDirectory()) continue;

               File entryFile = new File(destination, entry.getName());
               if (!entryFile.getParentFile().exists()) {
                   entryFile.getParentFile().mkdirs();
               }
            FileOutputStream fos = new FileOutputStream(entryFile);
               dest = new BufferedOutputStream(fos, BUFFER);
               while ((count = zis.read(data, 0, BUFFER)) != -1) {
                  dest.write(data, 0, count);
               }
               dest.flush();
            }
        } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(zis);
            IOUtils.closeQuietly(dest);
        }
    }

    private void commitChanges(Repository repository, String message) throws GitAPIException, IOException {
        Git git = new Git(repository);
        // Add once (without the update flag) to add new and modified files
        git.add().addFilepattern(".").call();
        // Then again with the update flag to add deleted and modified files
        git.add().addFilepattern(".").setUpdate(true).call();

        git.commit().setMessage(message).call();
    }

}