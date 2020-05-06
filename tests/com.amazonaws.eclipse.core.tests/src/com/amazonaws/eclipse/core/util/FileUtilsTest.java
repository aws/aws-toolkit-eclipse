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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class FileUtilsTest {
    @Test (expected = FileAlreadyExistsException.class)
    public void testCreateFileWithPermission600_FileAlreadyExists() throws IOException {
        Path file = Files.createTempFile("foo", "txt");
        FileUtils.createFileWithPermission600(file.toString());
    }

    @Test
    public void testCreateFileWithPermission600_NewFile() throws IOException {
        Path directory = Files.createTempDirectory("foo");
        Path file = Paths.get(directory.toString(), "bar");
        File newFile = FileUtils.createFileWithPermission600(file.toString());
        if (OsPlatformUtils.isLinux() || OsPlatformUtils.isMac()) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(newFile.toPath());
            Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
            Assert.assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
            Assert.assertFalse(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
            Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
            Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
            Assert.assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
            Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
            Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
            Assert.assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
        } else if (OsPlatformUtils.isWindows()) {
            Assert.assertTrue(Files.isReadable(newFile.toPath()));
            Assert.assertTrue(Files.isWritable(newFile.toPath()));
        }
    }
}
