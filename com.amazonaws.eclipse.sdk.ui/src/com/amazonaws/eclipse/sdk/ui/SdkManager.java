/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;

/**
 * A manager for all the different copies of the AWS SDK for Java that are
 * installed.
 */
public class SdkManager {

    /** The relative path of subdirectory containing all SDK installs in the workspace */
    private static final String SDK_DIR = "SDK";

    /** The relative path of the subdirectory containing the AWS SDK for Java */
    private static final String AWS_JAVA_SDK_PATH = "aws-java-sdk";

    /** The ID of the plugin containing the actual AWS SDK for Java */
    private static final String SDK_PLUGIN_ID = "com.amazonaws.eclipse.sdk";

    private static SdkManager singleton;

    /**
     * Returns the singleton instance of SdkManager.
     *
     * @return The singleton instance of SdkManager.
     */
    public static SdkManager getInstance() {
        if ( singleton == null ) {
            singleton = new SdkManager();
        }
        return singleton;
    }

    /* Hide the default public constructor */
    private SdkManager() {
    }

    /**
     * Returns the default AWS SDK for Java install (i.e. the one currently
     * installed in Eclipse).
     *
     * @return The default AWS SDK for Java install.
     */
    public SdkInstall getDefaultSdkInstall() {
        List<SdkInstall> sdkInstalls = getSdkInstalls();
        Collections.sort(sdkInstalls, new LatestVersionComparator());

        if (sdkInstalls.size() > 0) return sdkInstalls.get(0);

        return getInternalSdkInstall();
    }

    /**
     * Returns the SDK install directly in the SDK plugin.  This SDK install
     * should only be used as a last resort since old plugin versions will
     * be removed eventually.
     */
    public SdkInstall getInternalSdkInstall() {
        URL url = null;
        try {
            Bundle sdkBundle = Platform.getBundle(SDK_PLUGIN_ID);
            url = FileLocator.resolve(sdkBundle.getEntry("/"));
        } catch (IOException e) {
            Status status = new Status(IStatus.ERROR, SdkPlugin.PLUGIN_ID,
                    "Unable to find SDK plugin install directory", e);
            StatusManager.getManager().handle(status, StatusManager.SHOW);
            return null;
        }
        IPath sdkRootDirectory = new Path(url.getFile(), AWS_JAVA_SDK_PATH);

        return new SdkInstall(sdkRootDirectory.toFile());
    }

    /**
     * Returns a list of all the existing installations of the AWS SDK for Java.
     */
    public List<SdkInstall> getSdkInstalls() {

        List<SdkInstall> sdkInstalls = new LinkedList<SdkInstall>();

        try {
            File sdkDir = getSDKInstallDir();
            if ( sdkDir.exists() && sdkDir.isDirectory() ) {
                for ( File versionDir : sdkDir.listFiles() ) {
                    SdkInstall sdkInstall = new SdkInstall(versionDir);
                    if ( sdkInstall.isValidSdkInstall() )
                        sdkInstalls.add(sdkInstall);
                }
            }
            return sdkInstalls;
        } catch ( IllegalStateException e ) {
            SdkPlugin.getDefault().getLog()
                    .log(new Status(Status.WARNING, SdkPlugin.PLUGIN_ID, "No state directory to cache SDK", e));
            sdkInstalls.add(getDefaultSdkInstall());
            return sdkInstalls;
        }
    }

    /**
     * Returns the SDK installation of the specified version, or
     * <code>null</code> if no such installation exists.
     *
     * @param version
     *            The version of the SDK to return.
     * @return The SDK installation of the specified version, or
     *         <code>null</code> if no such installation exists.
     */
    public SdkInstall getSdkInstall(String version) {

        for ( SdkInstall sdkInstall : getSdkInstalls() ) {
            if ( sdkInstall.getVersion().equals(version) ) {
                return sdkInstall;
            }
        }

        return null;
    }

    /**
     * Copies the SDK given into the workspace's private state storage for this
     * plugin.
     */
    private void copySdk(SdkInstall install) {
        try {
            File sdkDir = getSDKInstallDir();
            File versionDir = new File(sdkDir, install.getVersion());

            if ( versionDir.exists() && new SdkInstall(versionDir).isValidSdkInstall() )
                return;
            if ( !versionDir.exists() && !versionDir.mkdirs() )
                throw new Exception("Couldn't make SDK directory " + versionDir);

            FileUtils.copyDirectory(install.getRootDirectory(), versionDir);
        } catch ( IllegalStateException e ) {
            SdkPlugin.getDefault().getLog()
                    .log(new Status(Status.WARNING, SdkPlugin.PLUGIN_ID, "No state directory to cache SDK", e));
        } catch ( Exception e ) {
            SdkPlugin.getDefault().getLog().log(new Status(Status.ERROR, SdkPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    /**
     * Returns the sdk install dir for the current workspace. With some
     * command-line arguments, this directory will not exist and cannot be
     * created, in which case an {@link IllegalStateException} is thrown.
     */
    private File getSDKInstallDir() throws IllegalStateException {
        IPath stateLocation = Platform.getStateLocation(Platform.getBundle("com.amazonaws.eclipse.sdk"));
        File sdkDir = new File(stateLocation.toFile(), SDK_DIR);
        return sdkDir;
    }

    /**
     * Initializes the set of SDK installs
     */
    public void initializeSDKInstalls() {
        copySdk(getInternalSdkInstall());
    }

    /**
     * Comparator that sorts SDK installs from most recent version to oldest.
     */
    private final class LatestVersionComparator implements Comparator<SdkInstall> {
        public int compare(SdkInstall left, SdkInstall right) {
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

}
