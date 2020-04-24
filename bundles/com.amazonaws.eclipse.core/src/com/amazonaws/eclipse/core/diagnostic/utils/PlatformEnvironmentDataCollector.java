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

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.services.errorreport.model.PlatformDataModel;

/**
 * This class collects all the platform runtime environment data upon its
 * initialization.
 */
final public class PlatformEnvironmentDataCollector {

    private static final PlatformDataModel DATA = getPlatformDataModel(Constants.AWS_TOOLKIT_FOR_ECLIPSE_PRODUCT_NAME);
    private static final PlatformDataModel DATA_TEST = getPlatformDataModel(Constants.AWS_TOOLKIT_FOR_ECLIPSE_PRODUCT_NAME_TEST);

    public static PlatformDataModel getData() {
        return AwsToolkitCore.getDefault().isDebugMode() ? DATA_TEST : DATA;
    }

    private static PlatformDataModel getPlatformDataModel(final String productName) {
        return new PlatformDataModel()
                .awsProduct(productName)
                .awsProductVersion(AwsToolkitCore.getDefault().getBundle().getVersion().toString())
                .language("Java")
                .languageVersion(System.getProperty("java.version"))
                .languageVmName(System.getProperty("java.vm.name"))
                .osArch(System.getProperty("os.arch"))
                .osName(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .platform("Eclipse")
                .platformVersion(Platform.getBundle("org.eclipse.platform").getVersion().toString());
    }
}
