/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_NAME_END_RESULT;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_NAME_FUNCTION_INPUT_TYPE;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_NAME_FUNCTION_OUTPUT_TYPE;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_CANCELED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_FAILED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_SUCCEEDED;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD;
import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.refreshProject;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.StreamHandlerTestClassTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewLambdaJavaFunctionProjectWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageTwo;
import com.amazonaws.eclipse.lambda.project.wizard.util.BrowserUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.JavaPackageName;

import freemarker.template.Template;

@SuppressWarnings("restriction")
public class NewLambdaJavaFunctionProjectWizard extends NewElementWizard implements INewWizard {

    private final NewLambdaJavaFunctionProjectWizardDataModel dataModel = new NewLambdaJavaFunctionProjectWizardDataModel();
    private NewLambdaJavaFunctionProjectWizardPageOne pageOne;
    private NewLambdaJavaFunctionProjectWizardPageTwo pageTwo;

    @Override
    public void addPages() {
        if (pageOne == null) {
            pageOne = new NewLambdaJavaFunctionProjectWizardPageOne(dataModel);
        }
        addPage(pageOne);

        if (pageTwo == null) {
            pageTwo = new NewLambdaJavaFunctionProjectWizardPageTwo(pageOne);
        }
        // We create pageTwo so that we can use the APIs provided by the system wizard.
        // But in the UI, we hide this page to keep the wizard simple.
    }

    @Override
    protected void finishPage(IProgressMonitor monitor)
            throws InterruptedException, CoreException {

        trackNewProjectAttributes(dataModel);

        pageTwo.performFinish(monitor);

        monitor.setTaskName("Configuring AWS Lambda Java project");

        IJavaProject javaProject = pageTwo.getJavaProject();
        final IProject project = javaProject.getProject();

        Display.getDefault().syncExec(new Runnable() {

            public void run() {

                File readmeFile = null;

                try {
                    savePreferences(dataModel, LambdaPlugin.getDefault().getPreferenceStore());

                    addSourceToProject(project, dataModel);

                    if (dataModel.isShowReadmeFile()) {
                        readmeFile = addReadmeFileToProject(project, dataModel.collectHandlerTestTemplateData());
                    }

                    refreshProject(project);

                } catch (Exception e) {
                    trackProjectCreationFailed();
                    StatusManager.getManager().handle(
                            new Status(IStatus.ERROR, LambdaPlugin.PLUGIN_ID,
                                    "Failed to create new Lambda project",
                                    e),
                                    StatusManager.SHOW);
                    return;
                }

                trackProjectCreationSucceeded();

                try {
                    IFile handlerClass = findHandlerClassFile(project, dataModel);
                    selectAndReveal(handlerClass); // show in explorer
                    openHandlerClassEditor(handlerClass); // show in editor
                } catch (Exception e) {
                    LambdaPlugin.getDefault().warn(
                            "Failed to open the handler class", e);
                }

                if (readmeFile != null) {
                    try {
                        BrowserUtil.openInternalBrowserAsEditor(readmeFile.toURI().toURL());
                    } catch (Exception e) {
                        LambdaPlugin.getDefault().warn(
                                "Failed to open README.html for the new Lambda project", e);
                    }
                }
            }
        });
    }

    @Override
    public boolean performCancel() {
        trackProjectCreationCanceled();
        return true;
    }

    private static IFile findHandlerClassFile(IProject project,
            NewLambdaJavaFunctionProjectWizardDataModel dataModel) {

        IPath handlerPath = new Path("src");
        JavaPackageName handlerPackage = JavaPackageName.parse(dataModel
                .getHandlerPackageName());
        for (String component : handlerPackage.getComponents()) {
            handlerPath = handlerPath.append(component);
        }
        handlerPath = handlerPath.append(dataModel.getHandlerClassName()
                + ".java");

        return project.getFile(handlerPath);
    }

    private static void openHandlerClassEditor(IFile handlerFile)
            throws PartInitException {

        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IDE.openEditor(page, handlerFile, true);
    }

    @Override
    public IJavaElement getCreatedElement() {
        return pageTwo.getJavaProject();
    }

    private static void savePreferences(
            NewLambdaJavaFunctionProjectWizardDataModel dataModel,
            IPreferenceStore prefStore) {
        prefStore.setValue(
                LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT,
                dataModel.isShowReadmeFile());
    }

    private static void addSourceToProject(IProject project,
            NewLambdaJavaFunctionProjectWizardDataModel dataModel) {

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

    private static File addReadmeFileToProject(IProject project,
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

    /*
     * Analytics
     */

    private void trackProjectCreationSucceeded() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_SUCCEEDED)
                .build());
    }

    private void trackProjectCreationFailed() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_FAILED)
                .build());
    }

    private void trackProjectCreationCanceled() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD)
                .addAttribute(ATTR_NAME_END_RESULT, ATTR_VALUE_CANCELED)
                .build());

    }

    private void trackNewProjectAttributes(NewLambdaJavaFunctionProjectWizardDataModel dataModel) {

        String inputType = dataModel.getPredefinedHandlerInputType() == null
                ? dataModel.getCustomHandlerInputType()
                : dataModel.getPredefinedHandlerInputType().getFqcn();
        String outputType = dataModel.getHandlerOutputType();

        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_NEW_LAMBDA_PROJECT_WIZARD)
                .addAttribute(ATTR_NAME_FUNCTION_INPUT_TYPE, inputType)
                .addAttribute(ATTR_NAME_FUNCTION_OUTPUT_TYPE, outputType)
                .build());

    }

}
