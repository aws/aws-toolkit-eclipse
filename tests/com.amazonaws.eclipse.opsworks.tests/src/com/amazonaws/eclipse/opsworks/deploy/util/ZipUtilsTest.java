package com.amazonaws.eclipse.opsworks.deploy.util;

import static org.junit.Assert.assertEquals;
import static com.amazonaws.eclipse.opsworks.deploy.util.ZipUtils.unzipFileToDirectory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ZipUtilsTest {
	
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void canUnpackAZipFileToDirectory() throws IOException {
        File zipFile = folder.newFile("file.zip");
        File target = folder.newFolder("target");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            writeEntry(zipOutputStream, "foo/bar.txt", "hello foo-bar!");
            writeEntry(zipOutputStream, "baz.txt", "hello baz!");
            writeEntry(zipOutputStream, "foo/../root.txt", "hello root!");
        }

        unzipFileToDirectory(zipFile, target);

        Map<String, String> actual = Files.walk(target.toPath()).filter(p -> p.toFile().isFile()).collect(Collectors.toMap(p -> target.toPath().relativize(p).toString(), this::content));
        assertEquals("hello foo-bar!", actual.get("foo/bar.txt".replace('/', File.separatorChar)));
        assertEquals("hello baz!", actual.get("baz.txt"));
        assertEquals("hello root!", actual.get("root.txt"));
    }

    @Test(expected = RuntimeException.class)
    public void exceptionThrownIfRelativeFileAttemptsToLeaveParentDirectory() throws IOException {
        File zipFile = folder.newFile("file.zip");
        File target = folder.newFolder("target");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            writeEntry(zipOutputStream, "foo/bar.txt", "hello foo-bar!");
            writeEntry(zipOutputStream, "../baz.txt", "hello baz!");
        }

        unzipFileToDirectory(zipFile, target);
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        IOUtils.copy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private String content(Path p) {
        try {
            return IOUtils.toString(new FileInputStream(p.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
