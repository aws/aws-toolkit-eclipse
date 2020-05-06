/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {

    /**
     * Create a new file with permission to POSIX permission 600 or equivalent, i.e owner-only readable and writable.
     *
     * @param fileLocation The file location
     * @return The newly created File with the permission.
     * @throws IOException When fails to set POSIX permission.
     * @throws FileAlreadyExistsException When the file already exists.
     */
    public static File createFileWithPermission600(String fileLocation) throws IOException, FileAlreadyExistsException {
        Path filePath = Paths.get(fileLocation);
        if (Files.exists(filePath)) {
            throw new FileAlreadyExistsException(filePath.toString());
        }
        Files.createFile(filePath);
        if (OsPlatformUtils.isWindows()) {
            File file = filePath.toFile();
            file.setReadable(true, true);
            file.setWritable(true, true);
        } else if (OsPlatformUtils.isLinux() || OsPlatformUtils.isMac()) {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(filePath, perms);
        }
        return filePath.toFile();
    }
}
