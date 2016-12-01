/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.serverless.NameUtils;
import com.amazonaws.eclipse.lambda.serverless.blueprint.Blueprint;
import com.amazonaws.eclipse.lambda.serverless.blueprint.BlueprintProvider;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessInputTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessOutputTemplateData;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.Template;

public class FunctionProjectUtil {

    private static final String LAMBDA_PROJECT_SETTING_FILE = "com.amazonaws.eclipse.lambda.project";

    public static void addSourceToProject(IProject project,
            LambdaFunctionWizardDataModel dataModel) {

        if (dataModel.isUseStreamHandler()) {
            StreamHandlerClassTemplateData streamHandlerClassData = dataModel.collectStreamHandlerTemplateData();
            addStreamHandlerClassToProject(project, streamHandlerClassData);

            StreamHandlerTestClassTemplateData streamHandlerTestClassData = dataModel.collectStreamHandlerTestTemplateData();
            addStreamHandlerTestClassToProject(project, streamHandlerTestClassData);
        } else {
            // Add handler class
            HandlerClassTemplateData handlerClassData = dataModel.collectHandlerTemplateData();
            addHandlerClassToProject(project, handlerClassData);

            // Add handler test class
            HandlerTestClassTemplateData handlerTestClassData = dataModel.collectHandlerTestTemplateData();
            addHandlerTestClassToProject(project, handlerTestClassData);
            addTestContextToProject(project, handlerTestClassData);

            if (dataModel.getPredefinedHandlerInputType() != null) {
                addTestUtilsToProject(project, handlerTestClassData);
            }

            // Add input json file if the user selects the predefined input type
            if (dataModel.getPredefinedHandlerInputType() != null) {
                String jsonFileName = dataModel.getPredefinedHandlerInputType()
                        .getSampleInputJsonFile();
                addSampleInputJsonFileToProject(project,
                        handlerTestClassData.getPackageName(), jsonFileName);
            }
        }

        addTestDirectoryToClasspath(project);
    }

    public static void addSourceToProject(IProject project, NewServerlessProjectDataModel dataModel)
            throws JsonParseException, JsonMappingException, IOException {
        emitServerlessModels(project, dataModel);
        emitServerlessHandlers(project, dataModel);
        emitServerlessTemplate(project, dataModel);
    }

    private static void emitServerlessTemplate(IProject project, NewServerlessProjectDataModel dataModel) {
        try {
            FunctionProjectUtil.addSourceFileToProject(
                    project,
                    NameUtils.SERVERLESS_TEMPLATE_FILE_NAME,
                    dataModel.getServerlessFilePath());
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add Serverless template file to the new Serverless project",
                    e);
            throw new RuntimeException(e);
        }
    }

    public static File addServerlessReadmeFileToProject(IProject project, NewServerlessProjectDataModel dataModel) {
        try {
            return FunctionProjectUtil.addSourceFileToProject(
                    project,
                    "README.html",
                    CodeTemplateManager.getInstance().getServerlessReadmeFile().getAbsolutePath());
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add Serverless template file to the new Serverless project",
                    e);
            throw new RuntimeException(e);
        }
    }

    public static File getServerlessTemplateFile(IProject project) {
        return getProjectDirectory(project, NameUtils.SERVERLESS_TEMPLATE_FILE_NAME).toFile();
    }

    private static void emitServerlessModels(IProject project, NewServerlessProjectDataModel dataModel)
            throws JsonParseException, JsonMappingException, IOException {
        if (dataModel.isNeedLambdaProxyIntegrationModel()) {
            addServerlessInputModelClassToProject(project, dataModel.getServerlessInputTemplateData());
            addServerlessOutputModelClassToProject(project, dataModel.getServerlessOutputTemplateData());
        }
    }

    private static void emitServerlessHandlers(IProject project, NewServerlessProjectDataModel dataModel)
            throws JsonParseException, JsonMappingException, IOException {
        for (ServerlessHandlerTemplateData templateData : dataModel.getServerlessHandlerTemplateData()) {
            if (dataModel.isUseBlueprint()) {
                Blueprint blueprint = BlueprintProvider.getInstance().getBlueprint(dataModel.getBlueprintName());
                String templatePath = blueprint.getHandlerTemplatePaths().get(templateData.getClassName());
                addServerlessBlueprintHandlerModelClassToProject(project, templateData, templatePath);
            } else {
                addServerlessHandlerModelClassToProject(project, templateData);
            }
        }
    }

    private static void addServerlessBlueprintHandlerModelClassToProject(IProject project, ServerlessHandlerTemplateData templateData, String templatePath) {
        try {
            Template handlerTemplate = CodeTemplateManager.getInstance()
                    .getTemplate(templatePath);
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    handlerTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add source to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addHandlerClassToProject(IProject project,
            HandlerClassTemplateData templateData) {

        try {
            Template handlerTemplate = CodeTemplateManager.getInstance()
                    .getHandlerClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    handlerTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add source to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addHandlerTestClassToProject(IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template testTemplate = CodeTemplateManager.getInstance()
                    .getHandlerTestClassTemplate();

            String fileContent = CodeTemplateManager.processTemplateWithData(
                    testTemplate, templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerTestClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test class to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addStreamHandlerClassToProject(IProject project,
            StreamHandlerClassTemplateData templateData) {

        try {
            Template streamHandlerTemplate = CodeTemplateManager.getInstance()
                    .getStreamHandlderClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    streamHandlerTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add source to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addStreamHandlerTestClassToProject(IProject project,
            StreamHandlerTestClassTemplateData templateData) {

        try {
            Template testTemplate = CodeTemplateManager.getInstance()
                    .getStreamHandlerTestClassTemplate();

            String fileContent = CodeTemplateManager.processTemplateWithData(
                    testTemplate, templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getHandlerTestClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test class to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addTestContextToProject(
            IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template template = CodeTemplateManager.getInstance()
                    .getTestContextTemplate();

            String content = CodeTemplateManager.processTemplateWithData(
                    template,
                    templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    "TestContext",
                    content);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test context to the new Lambda function project",
                    e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static void addTestUtilsToProject(
            IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template template = CodeTemplateManager.getInstance()
                    .getTestUtilsTemplate();

            String content = CodeTemplateManager.processTemplateWithData(
                    template,
                    templateData);

            FunctionProjectUtil.addTestClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    "TestUtils",
                    content);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test utils to the new Lambda function project",
                    e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private static void addSampleInputJsonFileToProject(IProject project,
            String testPackageName, String jsonFileName) {

        try {
            Template jsonFileTemplate = CodeTemplateManager.getInstance()
                    .getTestInputJsonFileTemplate(jsonFileName);
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    jsonFileTemplate, null);

            FunctionProjectUtil.addTestResourceToProject(
                    project,
                    JavaPackageName.parse(testPackageName),
                    jsonFileName,
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add test resource to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    public static File addReadmeFileToProject(IProject project,
            HandlerTestClassTemplateData templateData) {

        try {
            Template readmeFileTemplate = CodeTemplateManager.getInstance()
                    .getReadmeHtmlFileTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    readmeFileTemplate, templateData);

            return FunctionProjectUtil.addReadmeFileToProject(project, fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add README.html to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addTestDirectoryToClasspath(IProject project) {

        try {
            IJavaProject javaProj = JavaCore.create(project);
            IFolder tstFolder = project.getFolder("tst");

            IPackageFragmentRoot tstRoot = javaProj.getPackageFragmentRoot(tstFolder);

            if (javaProj.isOnClasspath(tstRoot)) return;

            IClasspathEntry[] originalCp = javaProj.getRawClasspath();
            IClasspathEntry[] augmentedCp = new IClasspathEntry[originalCp.length + 1];
            System.arraycopy(originalCp, 0, augmentedCp, 0, originalCp.length);

            augmentedCp[originalCp.length] = JavaCore.newSourceEntry(tstRoot.getPath());
            javaProj.setRawClasspath(augmentedCp, null);

        } catch (Exception e) {
            LambdaPlugin.getDefault().warn(
                    "Failed to add tst directory to the classpath", e);
        }
    }

    private static void addServerlessInputModelClassToProject(IProject project,
            ServerlessInputTemplateData templateData) {
        try {
            Template serverlessInputTemplate = CodeTemplateManager.getInstance()
                    .getServerlessInputClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    serverlessInputTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add Serverless INput model file to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addServerlessOutputModelClassToProject(IProject project,
            ServerlessOutputTemplateData templateData) {
        try {
            Template serverlessOutputTemplate = CodeTemplateManager.getInstance()
                    .getServerlessOutputClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    serverlessOutputTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add Serverless Output model file to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

    private static void addServerlessHandlerModelClassToProject(IProject project,
            ServerlessHandlerTemplateData templateData) {
        try {
            Template serverlessHandlerTemplate = CodeTemplateManager.getInstance()
                    .getServerlessHandlerClassTemplate();
            String fileContent = CodeTemplateManager.processTemplateWithData(
                    serverlessHandlerTemplate, templateData);

            FunctionProjectUtil.addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getClassName(),
                    fileContent);

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to add Serverless Output model file to the new Lambda function project",
                    e);
            throw new RuntimeException(e);
        }
    }

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
    private static void addSourceClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath srcRoot = getProjectDirectory(project, "src");
        String fileName = className + ".java";
        addClassToProject(srcRoot, packageName, fileName, classContent);
    }

    private static void addTestClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath tstRoot = getProjectDirectory(project, "tst");
        String fileName = className + ".java";
        addClassToProject(tstRoot, packageName, fileName, classContent);
    }

    private static void addTestResourceToProject(IProject project,
            JavaPackageName packageName, String fileName, String fileContent)
            throws FileNotFoundException, CoreException {

        IPath tstRoot = getProjectDirectory(project, "tst");
        addClassToProject(tstRoot, packageName, fileName, fileContent);
    }

    private static File addReadmeFileToProject(IProject project,
            String fileContent) throws FileNotFoundException, CoreException {

        IPath projectRoot = getProjectDirectory(project, null);
        return addFileToProject(projectRoot, "README.html", fileContent);
    }

    private static File addSourceFileToProject(IProject project, String fileName,
            String filePath) throws FileNotFoundException, IOException, CoreException {
        IPath targetPath = getProjectDirectory(project, null);
        String fileContent = IOUtils.toString(new FileInputStream(new File(filePath)));
        return addFileToProject(targetPath, fileName, fileContent);
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
