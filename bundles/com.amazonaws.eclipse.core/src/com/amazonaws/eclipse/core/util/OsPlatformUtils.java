/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.util;

import org.eclipse.core.runtime.Platform;

public class OsPlatformUtils {

    public static boolean isWindows() {
        return Platform.getOS().equals(Platform.OS_WIN32);
    }

    public static boolean isMac() {
        return Platform.getOS().equals(Platform.OS_MACOSX);
    }

    public static boolean isLinux() {
        return Platform.getOS().equals(Platform.OS_LINUX);
    }

    public static String currentUser() {
        return System.getProperty("user.name");
    }
}