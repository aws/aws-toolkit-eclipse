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
package com.amazonaws.eclipse.lambda.project.wizard;

import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.addSourceToProject;
import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.addServerlessReadmeFileToProject;
import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.refreshProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageTwo;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewServerlessProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.util.BrowserUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.JavaPackageName;
import com.amazonaws.eclipse.lambda.serverless.NameUtils;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@SuppressWarnings("restriction")
public class NewServerlessProjectWizard extends NewElementWizard implements INewWizard {

    private final NewServerlessProjectDataModel dataModel = new NewServerlessProjectDataModel();
    private NewServerlessProjectWizardPageOne pageOne;
    private NewLambdaJavaFunctionProjectWizardPageTwo pageTwo;

    @Override
    public void addPages() {
        if (pageOne == null) {
            pageOne = new NewServerlessProjectWizardPageOne(dataModel);
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

        pageTwo.performFinish(monitor);

        monitor.setTaskName("Creating New Serverless Java project");

        IJavaProject javaProject = pageTwo.getJavaProject();
        final IProject project = javaProject.getProject();

        Display.getDefault().syncExec(new Runnable() {

            public void run() {

                File readmeFile = null;
                LambdaAnalytics.trackServerlessProjectSelection(dataModel);
                try {
                    dataModel.buildServerlessModel();
                    addSourceToProject(project, dataModel);
                    readmeFile = addServerlessReadmeFileToProject(project, dataModel);
                    refreshProject(project);

                } catch (Exception e) {
                    LambdaAnalytics.trackServerlessProjectCreationFailed();
                    LambdaPlugin.getDefault().reportException("Failed to create new Serverless project!", e);
                    StatusManager.getManager().handle(
                            new Status(IStatus.ERROR, LambdaPlugin.PLUGIN_ID,
                                    "Failed to create new Serverless project",
                                    e),
                                    StatusManager.SHOW);
                    return;
                }

                LambdaAnalytics.trackServerlessProjectCreationSucceeded();

                try {
                    IFile handlerClass = findHandlerClassFile(project, dataModel);
                    selectAndReveal(handlerClass); // show in explorer
                    openHandlerClassEditor(handlerClass); // show in editor
                    if (readmeFile != null) {
                        BrowserUtil.openInternalBrowserAsEditor(readmeFile.toURI().toURL());
                    }
                } catch (Exception e) {
                    LambdaPlugin.getDefault().warn(
                            "Failed to open the start up file.", e);
                }
            }
        });
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackServerlessProjectCreationCanceled();
        return true;
    }

    @Override
    public IJavaElement getCreatedElement() {
        return pageTwo.getJavaProject();
    }

    private static IFile findHandlerClassFile(IProject project,
            NewServerlessProjectDataModel dataModel)
                    throws JsonParseException, JsonMappingException, IOException {

        IPath handlerPath = new Path("");
        List<ServerlessHandlerTemplateData> templates = dataModel.getServerlessHandlerTemplateData();
        if (templates == null || templates.isEmpty()) {
            handlerPath = handlerPath.append(NameUtils.SERVERLESS_TEMPLATE_FILE_NAME);
        } else {
            ServerlessHandlerTemplateData template = templates.get(0);
            handlerPath = handlerPath.append("src");
            JavaPackageName handlerPackage = JavaPackageName.parse(template.getPackageName());
            for (String component : handlerPackage.getComponents()) {
                handlerPath = handlerPath.append(component);
            }
            handlerPath = handlerPath.append(template.getClassName() + ".java");
        }
        return project.getFile(handlerPath);
    }

    private static void openHandlerClassEditor(IFile handlerFile)
            throws PartInitException {

        IWorkbenchPage page = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();

        IDE.openEditor(page, handlerFile, true);
    }
}
