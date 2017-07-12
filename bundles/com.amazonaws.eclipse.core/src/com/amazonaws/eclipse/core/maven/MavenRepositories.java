/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.maven;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.repository.RepositoryRegistry;
import org.eclipse.m2e.core.repository.IRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Class to manage Maven repositories used in the Maven plugin.
 */
@SuppressWarnings("restriction")
public class MavenRepositories {

    private static final String DEFAULT_REMOTE_MAVEN_REPOSITORY_URL = "https://repo.maven.apache.org/maven2/";

    public static MavenRepository getRemoteMavenRepository() {
        String remoteRepositoryUrl = DEFAULT_REMOTE_MAVEN_REPOSITORY_URL;
        // We respect the repositories configured in the Maven plugin.
        List<IRepository> repositories = MavenPlugin.getRepositoryRegistry()
                .getRepositories(RepositoryRegistry.SCOPE_SETTINGS);
        for (IRepository repository : repositories) {
            if (repository.getUrl() != null) {
                remoteRepositoryUrl = repository.getUrl();
                break;
            }
        }
        return getRemoteMavenRepository(remoteRepositoryUrl);
    }

    private static MavenRepository getRemoteMavenRepository(String remoteUrl) {
        return new RemoteMavenRepository(remoteUrl);
    }

    public static MavenRepository getDefaultLocalMavenRepository() {
        return getLocalMavenRepository(MavenPlugin.getRepositoryRegistry().getLocalRepository().getBasedir());
    }
    private static MavenRepository getLocalMavenRepository(File root) {
        return new LocalMavenRepository(root);
    }

    private static class RemoteMavenRepository implements MavenRepository {
        private static final String MAVEN_METADATA_XML_FILE_NAME = "maven-metadata.xml";

        private final String remoteUrl;

        // Modeled artifact metadata xml file. See http://repo1.maven.org/maven2/junit/junit/maven-metadata.xml for example.
        private static class ArtifactMetadata {
            public String groupId;
            public String artifactId;
            public Versioning versioning;

            public String getLatest() {
                return versioning.latest;
            }
        }

        private static class Versioning {
            public String latest;
            public String release;
            public Long lastUpdated;
            public List<String> versions;
        }

        public RemoteMavenRepository(String remoteUrl) {
            if (!remoteUrl.endsWith("/")) {
                remoteUrl += "/";   // appending "/" to remote url.
            }
            // Use "HTTPS" explicitly to avoid 403 error code.
            URL url = null;
            try {
                url = new URL(remoteUrl);
                if ("http".equalsIgnoreCase(url.getProtocol())) {
                    url = new URL("https", url.getHost(), url.getPort(), url.getFile());
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.remoteUrl = url.toString();
        }

        @Override
        public String getLatestVersion(String groupId, String artifactId) {
            String metadataUrl = buildMavenMetadataXmlUrl(groupId, artifactId);
            try {
                InputStream inputStream = new URL(metadataUrl).openStream();
                ObjectMapper mapper = new XmlMapper();
                return mapper.readValue(inputStream, ArtifactMetadata.class)
                        .getLatest();
            } catch (Exception e) {
                return null;
            }
        }

        // See the following link for example. The URL is fixed given group id and artifact id
        // https://repo.maven.apache.org/maven2/com/amazonaws/aws-java-sdk/maven-metadata.xml
        private String buildMavenMetadataXmlUrl(String groupId, String artifactId) {
            return String.format("%s%s/%s/%s",
                    this.remoteUrl, groupId.replace('.', '/'), artifactId, MAVEN_METADATA_XML_FILE_NAME);
        }
    }

    private static class LocalMavenRepository implements MavenRepository {

        private final String rootPath;

        public LocalMavenRepository(File root) {
            this.rootPath = root.getAbsolutePath();
        }

        @Override
        public String getLatestVersion(String groupId, String artifactId) {

            File targetFile = getTargetFolder(groupId, artifactId);
            if (!targetFile.exists() || !targetFile.isDirectory()) {
                return null;
            }
            List<String> versions = Arrays.asList(targetFile.list());
            if (versions.isEmpty()) {
                return null;
            }
            Collections.sort(versions, new MavenArtifactVersionComparator());
            return versions.get(0);
        }

        private File getTargetFolder(String groupId, String artifactId) {
            return new File(String.format("%s/%s/%s", rootPath, groupId.replace('.', '/'), artifactId));
        }
    }
}
