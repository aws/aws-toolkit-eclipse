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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

/**
 * Utilities for Bundle management.
 */
public class BundleUtils {

    /**
     * Return the actual file location in the bundle. The bundle could be a regular folder, or a jar file.
     */
    public static File getFileFromBundle(Bundle bundle, String... path) throws IOException, URISyntaxException {
        URL rootUrl = FileLocator.toFileURL(bundle.getEntry("/"));
        URI resolvedUrl = new URI(rootUrl.getProtocol(), rootUrl.getPath(), null);
        File file = new File(resolvedUrl);
        for (String segment : path) {
            file = new File(file, segment);
        }
        return file;
    }
}
