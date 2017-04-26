package com.amazonaws.eclipse.codedeploy.deploy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;


public class ZipUtils {

    public static void createZipFileOfDirectory(File srcDir, File zipOutput) throws IOException {
        if ( !srcDir.isDirectory() ) {
            throw new IllegalArgumentException(
                    srcDir.getAbsolutePath() + " is not a directory!");
        }

        ZipOutputStream zipOutputStream = null;
        String baseName = srcDir.getAbsolutePath() + File.pathSeparator;

        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(zipOutput));
            addDirToZip(srcDir, zipOutputStream, baseName);

        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }

    }

    private static void addDirToZip(File dir, ZipOutputStream zip, String baseName) throws IOException {
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                addDirToZip(file, zip, baseName);

            } else {
                String entryName = file.getAbsolutePath().substring(
                        baseName.length());
                ZipEntry zipEntry = new ZipEntry(entryName);
                zip.putNextEntry(zipEntry);

                FileInputStream fileInput = new FileInputStream(file);
                try {
                    IOUtils.copy(fileInput, zip);
                    zip.closeEntry();

                } finally {
                    IOUtils.closeQuietly(fileInput);
                }
            }
        }
    }

}
