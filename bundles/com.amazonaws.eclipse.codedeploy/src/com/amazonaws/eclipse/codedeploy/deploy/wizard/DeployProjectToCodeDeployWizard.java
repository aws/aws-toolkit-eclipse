/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.codedeploy.deploy.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.deploy.progress.DeploymentProgressTrackerDialog;
import com.amazonaws.eclipse.codedeploy.deploy.util.DeployUtils;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.model.DeployProjectToCodeDeployWizardDataModel;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.AppspecTemplateSelectionPage;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.DeploymentConfigurationPage;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.DeploymentGroupSelectionPage;
import com.amazonaws.eclipse.codedeploy.explorer.CodeDeployContentProvider;

public class DeployProjectToCodeDeployWizard extends Wizard {

    private final DeployProjectToCodeDeployWizardDataModel dataModel;

    /**
     * We keep a reference to this page so that we can pull the template
     * parameter values from it when the user clicks finish.
     */
    private AppspecTemplateSelectionPage appspecTemplateSelectionPage;

    public DeployProjectToCodeDeployWizard(IProject project) {
        dataModel = new DeployProjectToCodeDeployWizardDataModel(project);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new DeploymentGroupSelectionPage(dataModel));
        addPage(new DeploymentConfigurationPage(dataModel));
        addPage(appspecTemplateSelectionPage = new AppspecTemplateSelectionPage(dataModel));
    }

    @Override
    public boolean performFinish() {

        // Pull the template parameter values from the wizard page
        dataModel.setTemplateParameterValues(
                appspecTemplateSelectionPage.getParamValuesForSelectedTemplate());

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {

                    monitor.beginTask("Deploying web project [" +
                        dataModel.getProject().getName() + "] to CodeDeploy", 100);

                    // Initiate deployment (80/100 units)
                    final String deploymentId = DeployUtils.createDeployment(dataModel, monitor);

                    if (CodeDeployContentProvider.getInstance() != null) {
                        CodeDeployContentProvider.getInstance().refresh();
                    }

                    // Open deployment progress tracker (10/100)
                    monitor.subTask("Open deployment progress tracker...");

                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            DeploymentProgressTrackerDialog dialog = new DeploymentProgressTrackerDialog(
                                    Display.getDefault().getActiveShell(),
                                    deploymentId,
                                    dataModel.getDeploymentGroupName(),
                                    dataModel.getApplicationName(),
                                    dataModel.getRegion());
                            dialog.open();
                        }
                    });

                    monitor.worked(10);

                    monitor.done();
                }
            });

        } catch (InvocationTargetException e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Unexpected error during deployment", e.getCause());

        } catch (InterruptedException e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Unexpected InterruptedException during deployment", e.getCause());
        }

        return true;
    }

}
