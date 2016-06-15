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

import static com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil.refreshProject;

import java.io.File;

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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageOne;
import com.amazonaws.eclipse.lambda.project.wizard.page.NewLambdaJavaFunctionProjectWizardPageTwo;
import com.amazonaws.eclipse.lambda.project.wizard.util.BrowserUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.project.wizard.util.JavaPackageName;

@SuppressWarnings("restriction")
public class NewLambdaJavaFunctionProjectWizard extends NewElementWizard implements INewWizard {

    private final LambdaFunctionWizardDataModel dataModel = new LambdaFunctionWizardDataModel();
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

        LambdaAnalytics.trackNewProjectAttributes(dataModel);

        pageTwo.performFinish(monitor);

        monitor.setTaskName("Configuring AWS Lambda Java project");

        IJavaProject javaProject = pageTwo.getJavaProject();
        final IProject project = javaProject.getProject();

        Display.getDefault().syncExec(new Runnable() {

            public void run() {

                File readmeFile = null;

                try {
                    savePreferences(dataModel, LambdaPlugin.getDefault().getPreferenceStore());

                    FunctionProjectUtil.addSourceToProject(project, dataModel);

                    if (dataModel.isShowReadmeFile()) {
                        readmeFile = FunctionProjectUtil.addReadmeFileToProject(project, dataModel.collectHandlerTestTemplateData());
                    }

                    refreshProject(project);

                } catch (Exception e) {
                    LambdaAnalytics.trackProjectCreationFailed();
                    StatusManager.getManager().handle(
                            new Status(IStatus.ERROR, LambdaPlugin.PLUGIN_ID,
                                    "Failed to create new Lambda project",
                                    e),
                                    StatusManager.SHOW);
                    return;
                }

                LambdaAnalytics.trackProjectCreationSucceeded();

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
        LambdaAnalytics.trackProjectCreationCanceled();
        return true;
    }

    private static IFile findHandlerClassFile(IProject project,
            LambdaFunctionWizardDataModel dataModel) {

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
            LambdaFunctionWizardDataModel dataModel,
            IPreferenceStore prefStore) {
        prefStore.setValue(
                LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT,
                dataModel.isShowReadmeFile());
    }
}
