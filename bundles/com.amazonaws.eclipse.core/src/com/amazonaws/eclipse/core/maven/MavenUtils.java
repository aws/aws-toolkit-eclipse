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
package com.amazonaws.eclipse.core.maven;

import org.eclipse.core.resources.IFile;
import org.eclipse.m2e.core.internal.IMavenConstants;

@SuppressWarnings("restriction")
public class MavenUtils {

    // Returns whether the specified file is a Maven POM file.
    public static boolean isFilePom(IFile file) {
        return file != null && file.getFullPath().toFile().getAbsolutePath().endsWith(IMavenConstants.POM_FILE_NAME);
    }

}
