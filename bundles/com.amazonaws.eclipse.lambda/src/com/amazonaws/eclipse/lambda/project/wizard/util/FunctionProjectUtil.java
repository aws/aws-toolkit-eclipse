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
import java.io.PrintStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.validator.JavaPackageName;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.blueprint.ServerlessBlueprint;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Class that manages adding files (Java files, resource files, or configuration files) to project.
 */
public class FunctionProjectUtil {

    public static void createLambdaBlueprintProject(IProject project, LambdaFunctionWizardDataModel dataModel)
            throws TemplateException, IOException, CoreException {

        LambdaFunctionDataModel lambdaFunctionDataModel = dataModel.getLambdaFunctionDataModel();

        emitLambdaHandler(project, lambdaFunctionDataModel);
        emitLambdaTestUtils(project, lambdaFunctionDataModel);
        emitLambdaTestContext(project, lambdaFunctionDataModel);
        emitLambdaHandlerTest(project, lambdaFunctionDataModel);
        emitLambdaTestJson(project, lambdaFunctionDataModel);

        emitLambdaPom(project, dataModel);
    }

    /**
     * TODO consider to add Lambda function to any project, not only to Lambda project.
     */
    public static void createLambdaHandler(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        emitLambdaHandler(project, dataModel);
        emitLambdaTestUtils(project, dataModel);
        emitLambdaTestContext(project, dataModel);
        emitLambdaHandlerTest(project, dataModel);
        emitLambdaTestJson(project, dataModel);
    }

    public static File emitLambdaProjectReadme(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        Template readmeTemplate = CodeTemplateManager.getInstance().getlambdaProjectReadmeTemplate();
        LambdaBlueprintTemplateData templateData = dataModel.collectLambdaBlueprintTemplateData();
        String readmeContent = CodeTemplateManager.processTemplateWithData(readmeTemplate, templateData);
        return addFileToProject(project, "README.html", readmeContent);
    }

    public static void createServerlessBlueprintProject(IProject project, NewServerlessProjectDataModel dataModel)
            throws JsonParseException, JsonMappingException, IOException, CoreException, TemplateException {
        emitServerlessModels(project, dataModel);
        emitServerlessHandlers(project, dataModel);
        emitServerlessSam(project, dataModel);
        emitServerlessPom(project, dataModel);
    }

    private static void emitLambdaHandler(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        Template handlerTemplate = CodeTemplateManager.getInstance().getLambdaHandlerTemplate(
                dataModel.getSelectedBlueprint());
        LambdaBlueprintTemplateData freeMarkerDataModel = dataModel.collectLambdaBlueprintTemplateData();
        String handlerContent = CodeTemplateManager.processTemplateWithData(handlerTemplate, freeMarkerDataModel);
        addSourceClassToProject(
                project,
                JavaPackageName.parse(freeMarkerDataModel.getPackageName()),
                freeMarkerDataModel.getHandlerClassName(),
                handlerContent);
    }

    private static void emitLambdaTestUtils(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        Template testUtilTemplate = CodeTemplateManager.getInstance().getTestUtilsTemplate();
        LambdaBlueprintTemplateData freeMarkerDataModel = dataModel.collectLambdaBlueprintTemplateData();
        String testUtilContent = CodeTemplateManager.processTemplateWithData(testUtilTemplate, freeMarkerDataModel);
        addTestClassToProject(
                project,
                JavaPackageName.parse(freeMarkerDataModel.getPackageName()),
                "TestUtils",
                testUtilContent);
    }

    private static void emitLambdaTestContext(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        Template testContextTemplate = CodeTemplateManager.getInstance().getTestContextTemplate();
        LambdaBlueprintTemplateData freeMarkerDataModel = dataModel.collectLambdaBlueprintTemplateData();
        String testContextContent = CodeTemplateManager.processTemplateWithData(testContextTemplate, freeMarkerDataModel);
        addTestClassToProject(
                project,
                JavaPackageName.parse(freeMarkerDataModel.getPackageName()),
                "TestContext",
                testContextContent);
    }

    private static void emitLambdaHandlerTest(IProject project, LambdaFunctionDataModel dataModel)
            throws TemplateException, IOException, CoreException {
        Template handlerTestTemplate = CodeTemplateManager.getInstance().getLambdaHandlerTestTemplate(
                dataModel.getSelectedBlueprint());
        LambdaBlueprintTemplateData freeMarkerDataModel = dataModel.collectLambdaBlueprintTemplateData();
        String handlerTestContent = CodeTemplateManager.processTemplateWithData(handlerTestTemplate, freeMarkerDataModel);
        addTestClassToProject(
                project,
                JavaPackageName.parse(freeMarkerDataModel.getPackageName()),
                freeMarkerDataModel.getHandlerTestClassName(),
                handlerTestContent);
    }

    private static void emitLambdaTestJson(IProject project, LambdaFunctionDataModel dataModel)
            throws FileNotFoundException, IOException, CoreException {
        File testJsonFile = CodeTemplateManager.getInstance().getLambdaTestJsonFile(
                dataModel.getSelectedBlueprint());
        if (testJsonFile != null) {
            addTestResourceToProject(project, testJsonFile.getName(), testJsonFile);
        }
    }

    private static void emitLambdaPom(IProject project, LambdaFunctionWizardDataModel dataModel)
            throws CoreException, TemplateException, IOException {
        LambdaFunctionDataModel lambdaFunctionDataModel = dataModel.getLambdaFunctionDataModel();
        Template pomTemplate = CodeTemplateManager.getInstance().getLambdaBlueprintPomTemplate(
                lambdaFunctionDataModel.getSelectedBlueprint());
        PomFileTemplateData pomTemplateData = dataModel.collectPomTemplateData();
        String pomContent = CodeTemplateManager.processTemplateWithData(pomTemplate, pomTemplateData);
        addFileToProject(project, "pom.xml", pomContent);
    }

    private static void emitServerlessModels(IProject project, NewServerlessProjectDataModel dataModel)
            throws TemplateException, IOException, CoreException {

        if (dataModel.getSelectedBlueprint().isNeedLambdaProxyIntegrationModel()) {
            ServerlessDataModelTemplateData templateData = dataModel.getServerlessDataModelTemplateData();
            addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getServerlessInputClassName(),
                    CodeTemplateManager.processTemplateWithData(
                            CodeTemplateManager.getInstance().getServerlessInputClassTemplate(),
                            templateData));
            addSourceClassToProject(
                    project,
                    JavaPackageName.parse(templateData.getPackageName()),
                    templateData.getServerlessOutputClassName(),
                    CodeTemplateManager.processTemplateWithData(
                            CodeTemplateManager.getInstance().getServerlessOutputClassTemplate(),
                            templateData));
        }
    }

    private static void emitServerlessHandlers(IProject project, NewServerlessProjectDataModel dataModel)
            throws IOException, CoreException, TemplateException {
        ServerlessBlueprint blueprint = dataModel.getSelectedBlueprint();
        for (ServerlessHandlerTemplateData templateData : dataModel.getServerlessHandlerTemplateData()) {
            if (dataModel.isUseBlueprint()) {
                addSourceClassToProject(
                        project,
                        JavaPackageName.parse(templateData.getPackageName()),
                        templateData.getClassName(),
                        CodeTemplateManager.processTemplateWithData(
                                CodeTemplateManager.getInstance().getServerlessHandlerClassTemplate(blueprint, templateData.getClassName()),
                                templateData));
            } else {
                addSourceClassToProject(
                        project,
                        JavaPackageName.parse(templateData.getPackageName()),
                        templateData.getClassName(),
                        CodeTemplateManager.processTemplateWithData(
                                CodeTemplateManager.getInstance().getServerlessHandlerClassTemplate(),
                                templateData));
            }
        }
    }

    private static void emitServerlessSam(IProject project, NewServerlessProjectDataModel dataModel)
            throws FileNotFoundException, IOException, CoreException, TemplateException {
        addFileToProject(project, CodeTemplateManager.SAM_FILE_NAME,
                CodeTemplateManager.processTemplateWithData(CodeTemplateManager.getInstance().getServerlessSamTemplate(dataModel.getSelectedBlueprint()),
                        dataModel.getServerlessSamTemplateData()));
    }

    private static void emitServerlessPom(IProject project, NewServerlessProjectDataModel dataModel)
            throws CoreException, TemplateException, IOException {
        addFileToProject(project, "pom.xml",
                CodeTemplateManager.processTemplateWithData(
                        CodeTemplateManager.getInstance().getServerlessPomFile(dataModel.getSelectedBlueprint()),
                        dataModel.getServerlessPomTemplateData()));
    }

    public static File emitServerlessReadme(IProject project, NewServerlessProjectDataModel dataModel)
            throws FileNotFoundException, IOException, CoreException {
        return addFileToProject(
                project,
                "README.html",
                CodeTemplateManager.getInstance().getServerlessReadmeFile());
    }

    public static File getServerlessTemplateFile(IProject project) {
        return getProjectDirectory(project, CodeTemplateManager.SAM_FILE_NAME).toFile();
    }

    private static void addSourceClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath srcRoot = getProjectDirectory(project, MavenFactory.getMavenSourceFolder());
        String fileName = className + ".java";
        addClassToProject(srcRoot, packageName, fileName, classContent);
    }

    private static void addTestClassToProject(IProject project,
            JavaPackageName packageName, String className, String classContent)
            throws CoreException, FileNotFoundException {

        IPath tstRoot = getProjectDirectory(project, MavenFactory.getMavenTestFolder());
        String fileName = className + ".java";
        addClassToProject(tstRoot, packageName, fileName, classContent);
    }

    private static void addTestResourceToProject(IProject project, String fileName, File file)
            throws FileNotFoundException, IOException, CoreException {
        IPath testResourcePath = getProjectDirectory(project, MavenFactory.getMavenTestResourceFolder());
        addFileToProject(testResourcePath, fileName, file);
    }

    /**
     * Add a Java class to project.
     *
     * @param root - The package root path for the Java class.
     * @param packageName - The package name model
     * @param fileName - The Java class file name
     * @param classContent - The Java class file content.
     */
    private static void addClassToProject(IPath root,
            JavaPackageName packageName, String fileName, String classContent)
            throws CoreException, FileNotFoundException {

        IPath targetPath = root;
        for (String component : packageName.getComponents()) {
            targetPath = targetPath.append(component);
        }
        addFileToProject(targetPath, fileName, classContent);
    }

    /*
     * Add file to the root of the specified project.
     */
    private static File addFileToProject(IProject project, String fileName, File file)
            throws FileNotFoundException, IOException, CoreException {
        IPath targetPath = getProjectDirectory(project, null);
        String fileContent = IOUtils.toString(new FileInputStream(file));
        return addFileToProject(targetPath, fileName, fileContent);
    }

    /*
     * Add a file to the root of the specified project.
     */
    private static File addFileToProject(IProject project, String fileName, String fileContent)
            throws FileNotFoundException, CoreException {
        IPath targetPath = getProjectDirectory(project, null);
        return addFileToProject(targetPath, fileName, fileContent);
    }

    private static File addFileToProject(IPath targetPath, String fileName, File file)
            throws FileNotFoundException, IOException, CoreException {
        String fileContent = IOUtils.toString(new FileInputStream(file));
        return addFileToProject(targetPath, fileName, fileContent);
    }

    /**
     * Add a file with the specified content to the target path.
     *
     * @param targetPath - The target location for the file to be added in.
     * @param fileName - The file name
     * @param fileContent - The file content
     * @return The file in the target location.
     */
    private static File addFileToProject(IPath targetPath, String fileName, String fileContent)
            throws CoreException, FileNotFoundException {

        IFileStore targetFileStore = EFS.getLocalFileSystem().fromLocalFile(
                targetPath.append(fileName).toFile());

        File targetFile = targetFileStore.toLocalFile(EFS.NONE, null);
        targetFile.getParentFile().mkdirs();
        try (PrintStream ps = new PrintStream(new FileOutputStream(targetFile))) {
            ps.print(fileContent);
        }

        return targetFile;
    }

    /*
     * Return the absolute path of the specified location relative to the project.
     */
    public static IPath getProjectDirectory(IProject project, String path) {
        IPath projectRoot = project.getLocation();

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
            LambdaPlugin.getDefault().logWarning(
                    "Failed to refresh project " + project.getName(), e);
        }
    }
}
