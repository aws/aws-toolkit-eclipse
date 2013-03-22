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
package com.amazonaws.eclipse.android.sdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.amazonaws.eclipse.sdk.ui.AbstractSdkInstall;
import com.amazonaws.eclipse.sdk.ui.FilenameFilters;

public class AndroidSdkInstall extends AbstractSdkInstall {

    /** File path for SDK version/release info */
    protected static final String VERSION_INFO_PROPERTIES_PATH = "com/amazonaws/sdk/versionInfo.properties";

    /** The library directory within this SDK install */
    protected File libDirectory;

    /** The third-party library directory within this SDK install */
    protected File thirdPartyDirectory;


    /**
     * Filename filter accepting only the library jar from the AWS SDK for Android.
     */
    public static class AndroidSdkLibraryJarFilenameFilter implements FilenameFilter {
        private static final Pattern AWS_ANDROID_SDK_PATTERN =
            Pattern.compile("aws-android-sdk-(\\d+|\\.)+-debug\\.jar");

        public boolean accept(File dir, String name) {
            return AWS_ANDROID_SDK_PATTERN.matcher(name).matches();
        }
    }


    AndroidSdkInstall(File sdkRootDirectory) {
        super(sdkRootDirectory);
        libDirectory = new File(sdkRootDirectory, "lib");
        thirdPartyDirectory = new File(sdkRootDirectory, "third-party");
    }

    public File getSdkJar() throws FileNotFoundException {
        File[] files = libDirectory.listFiles(new AndroidSdkLibraryJarFilenameFilter());
        if (files == null || files.length != 1) {
            throw new FileNotFoundException(
                    "Could not uniquely identify an SDK jar in"
                            + this.libDirectory + ".  Found: " + files);
        }
        return files[0];
    }

    /**
     * Returns true if this object represents a valid AWS SDK for Java install (i.e.
     * the correct libraries are present).
     *
     * @return True if this object represents a valid AWS SDK for Java install.
     */
    @Override
    public boolean isValidSdkInstall() {
        return sdkRootDirectory.exists() && libDirectory.exists() && thirdPartyDirectory.exists();
    }

    /**
     * Returns a list of all the third-party dependency Jar files for this AWS
     * SDK for Java.
     * <p>
     * The Android SDK relies heavily on the third-party libraries that are
     * present in the Android platform, and bundles anything else in the main
     * SDK jar, so because of that, there are no third-party libraries required
     * to be added in addition to the main Android SDK jar.
     *
     * @return A list of all the third-party dependency Jar files for this AWS
     *         SDK for Java.
     */
    public List<File> getThirdPartyJars() {
        return Collections.emptyList();
    }

    /**
     * Returns the version identifier for this SDK install, if known.
     *
     * @return The version identifier for this SDK install, if known.
     */
    public String getVersion() {
        try {
            JarFile jarFile = new JarFile(getSdkJar());
            ZipEntry zipEntry = jarFile.getEntry(VERSION_INFO_PROPERTIES_PATH);

            Properties properties = new Properties();
            properties.load(jarFile.getInputStream(zipEntry));

            return properties.getProperty("version");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Unknown";
    }

}