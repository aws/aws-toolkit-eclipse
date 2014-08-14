/*
 * Copyright 2008-2014 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.diagnostic.utils;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.diagnostic.model.PlatformEnvironmentDataModel;

/**
 * This class collects all the platform runtime environment data upon its
 * initialization.
 */
public class PlatformEnvironmentDataCollector {

    private static PlatformEnvironmentDataModel data;

    static {
        data = collectData();
    }

    public static PlatformEnvironmentDataModel getData() {
        return data;
    }

    private static PlatformEnvironmentDataModel collectData() {
        PlatformEnvironmentDataModel data = new PlatformEnvironmentDataModel();

        data.setOsName(        System.getProperty("os.name")        );
        data.setOsVersion(     System.getProperty("os.version")     );
        data.setOsArch(        System.getProperty("os.arch")        );
        data.setJavaVmName(    System.getProperty("java.vm.name")   );
        data.setJavaVmVersion( System.getProperty("java.vm.version"));
        data.setJavaVersion(   System.getProperty("java.version")   );

        Bundle mainPlatformBundle = Platform.getBundle("org.eclipse.platform");

        if (mainPlatformBundle != null) {
            Object eclipsePlatformVersion = mainPlatformBundle.getHeaders()
                    .get("Bundle-Version");
            if (eclipsePlatformVersion instanceof String) {
                data.setEclipsePlatformVersion((String)eclipsePlatformVersion);
            }
        }

        BundleContext ctx = AwsToolkitCore.getDefault().getBundle()
                .getBundleContext();
        for (Bundle bundle : ctx.getBundles()) {
            data.addInstalledBundle(bundle);
        }

        return data;
    }

    private PlatformEnvironmentDataCollector() {}
}
