package com.amazonaws.eclipse.lambda.upload.wizard.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarBuilder;
import org.eclipse.jdt.ui.jarpackager.IManifestProvider;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryComponent;
import com.amazonaws.eclipse.lambda.project.classpath.runtimelibrary.LambdaRuntimeLibraryManager;

public class LambdaFunctionJarBuilder extends FatJarBuilder {

    private static final String LAMBDA_FUNCTINO_JAR_BUILDER_ID = LambdaFunctionJarBuilder.class.getName();

    private final List<File> archiveFilesToExclude = new LinkedList<File>();

    public LambdaFunctionJarBuilder() {
        // exclude all the lambda runtime jars
        for (LambdaRuntimeLibraryComponent component : LambdaRuntimeLibraryManager
                .getInstance().getLatestVersion().getLibraryComponents()) {
            if (component.isShouldBeExcludedInFunctionCode()) {
                archiveFilesToExclude.add(component.getClassJarFile());
            }
        }
    }

    public String getId() {
        return LAMBDA_FUNCTINO_JAR_BUILDER_ID;
    }

    public IManifestProvider getManifestProvider() {
        // we don't need to bundle manifest file for the function zip file
        return null;
    }

    public void writeArchive(ZipFile zip, IProgressMonitor monitor) {

        String zipPath = zip.getName();
        File zipFile = new File(zipPath);
        String zipName = zipFile.getName();

        if (shouldArchiveFileBeExcluded(zipFile)) {
            return;
        }

        try {
            getJarWriter().write(zipFile, new Path("lib/" + zipName));
        } catch (CoreException e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to bundle dependency into the function jar file. " + zipPath, e);
        }
    }

    @Override
    public String getManifestClasspath() {
        return null;
    }

    @Override
    public boolean isMergeManifests() {
        return false;
    }

    @Override
    public boolean isRemoveSigners() {
        return true;
    }

    private boolean shouldArchiveFileBeExcluded(File archiveFile) {
        for (File toExclude : archiveFilesToExclude) {
            try {
                if (toExclude.getCanonicalPath().equals(archiveFile.getCanonicalPath())) {
                    return true;
                }
            } catch (IOException ioe) {
                continue;
            }
        }
        return false;
    }

}
