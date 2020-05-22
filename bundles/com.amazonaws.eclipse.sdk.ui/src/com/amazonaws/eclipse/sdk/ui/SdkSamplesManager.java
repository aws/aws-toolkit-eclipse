/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.core.util.BundleUtils;

public class SdkSamplesManager {

    public static List<SdkSample> getSamples() {
        File samplesPath = getSamplesBasedir();

        File[] sampleDirectories = samplesPath.listFiles(
                new FileFilter() {
                    /**
                     * @see java.io.FileFilter#accept(java.io.File)
                     */
                    @Override
                    public boolean accept(File pathname) {
                        return new File(pathname, "sample.properties").exists();
                    }
                });

        List<SdkSample> samples = new ArrayList<>();
        if (sampleDirectories == null || sampleDirectories.length == 0) {
            return samples;
        }

        for (File file : sampleDirectories) {
            try (FileInputStream inputStream = new FileInputStream(new File(file, "sample.properties"))) {
                Properties properties = new Properties();
                properties.load(inputStream);
                samples.add(new SdkSample(
                        properties.getProperty("name"),
                        properties.getProperty("description"),
                        new Path(file.getAbsolutePath())));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return samples;
    }

    private static File getSamplesBasedir() {
        Bundle bundle = JavaSdkPlugin.getDefault().getBundle();
        File file = null;
        try {
            file = BundleUtils.getFileFromBundle(bundle, "samples");
        } catch (IOException e) {
            throw new RuntimeException("Failed to find plugin bundle root.", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find plugin sample folder", e);
        }
        return file;
    }
}
