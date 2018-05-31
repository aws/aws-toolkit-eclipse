package com.amazonaws.eclipse.opsworks.deploy.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;


public class ZipUtils {

    public static void createZipFileOfDirectory(File srcDir, File zipOutput) throws IOException {
        if ( !srcDir.exists() || !srcDir.isDirectory() ) {
            throw new IllegalArgumentException(
                    srcDir.getAbsolutePath() + " is not a directory!");
        }
        if ( zipOutput.exists() && !zipOutput.isFile() ) {
            throw new IllegalArgumentException(
                    zipOutput.getAbsolutePath() + " exists but is not a file!");
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

    public static void unzipFileToDirectory(File zipFile, File targetDirectory) throws IOException {
        if ( !zipFile.exists() || !zipFile.isFile() ) {
            throw new IllegalArgumentException(
                    zipFile.getAbsolutePath() + " is not a file!");
        }
        if ( !targetDirectory.exists() || !targetDirectory.isDirectory() ) {
            throw new IllegalArgumentException(
                    targetDirectory.getAbsolutePath() + " is not a directory!");
        }
        if ( targetDirectory.listFiles().length != 0 ) {
            throw new IllegalArgumentException(
                    targetDirectory.getAbsolutePath() + " is not empty!");
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();

        while (zipEntry != null) {

            String entryFileName = zipEntry.getName();
            File newFile = new File(targetDirectory, entryFileName);
            if (!newFile.getCanonicalPath().startsWith(targetDirectory.getCanonicalPath())) {
            	throw new RuntimeException(newFile.getAbsolutePath() + " is outside of targetDirectory: " + targetDirectory.getAbsolutePath());
            }
            
            if (zipEntry.isDirectory()) {
                if ( !newFile.exists() ) {
                    newFile.mkdirs();
                } else if ( !newFile.isDirectory() ) {
                    throw new RuntimeException(newFile.getAbsolutePath()
                            + " already exists and is not a directory!");
                }

            } else {
                // File entry might be visited before its parent folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                try {
                    IOUtils.copy(zis, fos);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }


            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();

        IOUtils.closeQuietly(zis);
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
