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
package com.amazonaws.eclipse.opsworks.deploy.wizard;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.deploy.util.DeployUtils;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;
import com.amazonaws.eclipse.opsworks.deploy.wizard.page.AppConfigurationPage;
import com.amazonaws.eclipse.opsworks.deploy.wizard.page.DeploymentActionConfigurationPage;
import com.amazonaws.eclipse.opsworks.deploy.wizard.page.TargetAppSelectionPage;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Deployment;
import com.amazonaws.services.opsworks.model.DescribeAppsRequest;
import com.amazonaws.services.opsworks.model.DescribeDeploymentsRequest;

public class DeployProjectToOpsworksWizard extends Wizard {

    private final DeployProjectToOpsworksWizardDataModel dataModel;

    public DeployProjectToOpsworksWizard(IProject project) {
        dataModel = new DeployProjectToOpsworksWizardDataModel(project);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new TargetAppSelectionPage(dataModel));
        addPage(new AppConfigurationPage(dataModel));
        addPage(new DeploymentActionConfigurationPage(dataModel));
    }

    @Override
    public boolean performFinish() {

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException,
                        InterruptedException {

                    monitor.beginTask("Deploying local project [" +
                        dataModel.getProject().getName() + "] to OpsWorks", 110);

                    final String deploymentId = DeployUtils.runDeployment(dataModel, monitor);

                    // Open deployment progress tracker (10/110)
                    monitor.subTask("Waiting for the deployment to finish...");

                    Job trackProgressJob = new Job("Waiting for the deployment to finish") {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {

                            monitor.beginTask(String.format(
                                    "Waiting deployment [%s] to finish...",
                                    deploymentId), IProgressMonitor.UNKNOWN);

                            String endpoint = dataModel.getRegion().getServiceEndpoints()
                                    .get(ServiceAbbreviations.OPSWORKS);
                            AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                                    .getOpsWorksClientByEndpoint(endpoint);

                            try {
                                final Deployment deployment = waitTillDeploymentFinishes(client, deploymentId, monitor);

                                if ("successful".equalsIgnoreCase(deployment.getStatus())) {
                                    final App appDetail = client
                                            .describeApps(
                                                    new DescribeAppsRequest()
                                                            .withAppIds(deployment
                                                                    .getAppId()))
                                            .getApps().get(0);

                                    Display.getDefault().syncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                            MessageDialog.openInformation(Display
                                                    .getDefault().getActiveShell(),
                                                    "Deployment success",
                                                    String.format(
                                                            "Deployment [%s] succeeded (Instance count: %d). " +
                                                            "The application will be available at %s://{instance-public-endpoint}/%s/",
                                                            deployment.getDeploymentId(),
                                                            deployment.getInstanceIds().size(),
                                                            appDetail.isEnableSsl() ? "https" : "http",
                                                            appDetail.getShortname()));
                                        }
                                    });
                                    return ValidationStatus.ok();

                                } else {
                                    Display.getDefault().syncExec(new Runnable() {
                                        @Override
                                        public void run() {
                                        MessageDialog.openError(Display
                                                .getDefault().getActiveShell(),
                                                "Deployment failed", "");
                                        }
                                    });
                                    return ValidationStatus.error("The deployment failed.");
                                }

                            } catch (Exception e) {
                                return ValidationStatus.error("Unable to query the progress of the deployment", e);
                            }
                        }

                        private Deployment waitTillDeploymentFinishes(
                                AWSOpsWorks client, String deploymentId, IProgressMonitor monitor)
                                throws InterruptedException {

                            while (true) {
                                Deployment deployment = client
                                        .describeDeployments(new DescribeDeploymentsRequest()
                                                .withDeploymentIds(deploymentId))
                                        .getDeployments().get(0);

                                monitor.subTask(String.format(
                                        "Instance count: %d, Last status: %s",
                                        deployment.getInstanceIds().size(),
                                        deployment.getStatus()));

                                if ("successful".equalsIgnoreCase(deployment.getStatus())
                                        || "failed".equalsIgnoreCase(deployment.getStatus())) {
                                    return deployment;
                                }

                                Thread.sleep(5 * 1000);
                            }
                        }
                    };

                    trackProgressJob.setUser(true);
                    trackProgressJob.schedule();

                    monitor.worked(10);

                    monitor.done();
                }
            });

        } catch (InvocationTargetException e) {
            OpsWorksPlugin.getDefault().reportException(
                    "Unexpected error during deployment", e.getCause());

        } catch (InterruptedException e) {
            OpsWorksPlugin.getDefault().reportException(
                    "Unexpected InterruptedException during deployment", e.getCause());
        }

        return true;
    }

}
