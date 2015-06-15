package com.amazonaws.eclipse.lambda.project.wizard.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;

public class FunctionProjectUtil {

    private static final String LAMBDA_PROJECT_SETTING_FILE = "com.amazonaws.eclipse.lambda.project";

    /**
     * @param project
     *            the target project where the source is added
     * @param packageName
     *            the Java package name of the class being added
     * @param className
     *            the name of the class.
     * @param classContent
     *            the content of the class
     */
    public static void addSourceClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath srcRoot = getProjectDirectory(project, "src");
        String fileName = className + ".java";
        addClassToProject(srcRoot, packageName, fileName, classContent);
    }

    public static void addTestClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath tstRoot = getProjectDirectory(project, "tst");
        String fileName = className + ".java";
        addClassToProject(tstRoot, packageName, fileName, classContent);
    }

    public static void addTestResourceToProject(IProject project,
            JavaPackageName packageName, String fileName, String fileContent)
            throws FileNotFoundException, CoreException {

        IPath tstRoot = getProjectDirectory(project, "tst");
        addClassToProject(tstRoot, packageName, fileName, fileContent);
    }

    public static File addReadmeFileToProject(IProject project,
            String fileContent) throws FileNotFoundException, CoreException {

        IPath projectRoot = getProjectDirectory(project, null);
        return addFileToProject(projectRoot, "README.html", fileContent);
    }


    private static void addClassToProject(IPath root,
            JavaPackageName packageName, String fileName, String classContent)
            throws CoreException, FileNotFoundException {

        IPath targetPath = root;
        for (String component : packageName.getComponents()) {
            targetPath = targetPath.append(component);
        }
        addFileToProject(targetPath, fileName, classContent);
    }

    private static File addFileToProject(IPath targetPath, String fileName,
            String fileContent) throws CoreException, FileNotFoundException {

        IFileStore targetFileStore = EFS.getLocalFileSystem().fromLocalFile(
                targetPath.append(fileName).toFile());

        File targetFile = targetFileStore.toLocalFile(EFS.NONE, null);
        targetFile.getParentFile().mkdirs();
        PrintStream ps = new PrintStream(new FileOutputStream(targetFile));
        ps.print(fileContent);
        ps.close();

        return targetFile;
    }

    /**
     * This function overrides all the existing metadata for the project.
     */
    public static void addLambdaProjectMetadata(IProject project,
            LambdaFunctionProjectMetadata metadata) {

        if (!metadata.isValid()) {
            throw new IllegalArgumentException(
                    "Invalid Lambda project metadata.");
        }

        IPath settingsDir = getProjectDirectory(project, ".settings");
        settingsDir.toFile().mkdirs();

        File settingFile = settingsDir.append(LAMBDA_PROJECT_SETTING_FILE).toFile();

        OutputStream out = null;
        try {
            out = new FileOutputStream(settingFile);
            metadata.toProperties().store(out, "Lambda Function Project Metadata");
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to write project metadata.", e);
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                LambdaPlugin.getDefault().warn(
                        "Failed to close FileOutputStreama " +
                        "after writing project metadata.",
                        e);
            }
        }
    }

    public static LambdaFunctionProjectMetadata loadLambdaProjectMetadata(IProject project) {

        IPath settingsDir = getProjectDirectory(project, ".settings");
        File settingFile = settingsDir.append(LAMBDA_PROJECT_SETTING_FILE).toFile();

        if (!settingFile.exists()) {
            return null;
        }

        InputStream in = null;
        Properties props = new Properties();
        try {
            in = new FileInputStream(settingFile);
            props.load(in);
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to read project metadata.", e);
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LambdaPlugin.getDefault().warn(
                        "Failed to close FileInputStream " +
                        "after reading project metadata.",
                        e);
            }
        }

        return LambdaFunctionProjectMetadata.fromProperties(props);
    }

    private static IPath getProjectDirectory(IProject project, String path) {
        IPath workspaceRoot = project.getWorkspace().getRoot().getRawLocation();
        IPath projectRoot = workspaceRoot.append(project.getFullPath());

        if (path == null) {
            return projectRoot;
        } else {
            return projectRoot.append(path);
        }
    }

    public static void refreshProject(IProject project) {
        // Finally, refresh the project so that the new files show up
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            LambdaPlugin.getDefault().warn(
                    "Failed to refresh project " + project.getName(), e);
        }
    }
}
