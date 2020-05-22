/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.explorer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;

public class ElasticBeanstalkActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        boolean onlyEnvironmentsSelected = true;
        boolean onlyApplicationsSelected = true;
        StructuredSelection selection = (StructuredSelection)getActionSite().getStructuredViewer().getSelection();
        @SuppressWarnings("rawtypes")
        Iterator iterator = selection.iterator();
        List<EnvironmentDescription> environments = new ArrayList<>();
        List<ApplicationDescription> applications = new ArrayList<>();
        while ( iterator.hasNext() ) {
            Object obj = iterator.next();
            if ( obj instanceof EnvironmentDescription ) {
                environments.add((EnvironmentDescription) obj);
            } else {
                onlyEnvironmentsSelected = false;
            }
            if ( obj instanceof ApplicationDescription ) {
                applications.add((ApplicationDescription) obj);
            } else {
                onlyApplicationsSelected = false;
            }
        }

        if ( onlyEnvironmentsSelected ) {
            if ( environments.size() == 1 ) {
                menu.add(new OpenEnvironmentEditorAction(environments.get(0), RegionUtils.getCurrentRegion()));
                menu.add(new Separator());
            }

            menu.add(new TerminateEnvironmentsAction(environments));
        }

        if ( onlyApplicationsSelected ) {
            menu.add(new DeleteApplicationAction(applications));
        }
    }


    private static class TerminateEnvironmentsAction extends AwsAction {
        private final List<EnvironmentDescription> environments;

        public TerminateEnvironmentsAction(List<EnvironmentDescription> environments) {
            super(AwsToolkitMetricType.EXPLORER_BEANSTALK_TERMINATE_ENVIRONMENT);
            this.environments = environments;

            this.setText("Terminate Environment");
            this.setToolTipText("Terminate the selected environments");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        protected void doRun() {
            Dialog dialog = newConfirmationDialog("Terminate selected environments?", "Are you sure you want to terminate the selected AWS Elastic Beanstalk environments?");
            if (dialog.open() != 0) {
                actionCanceled();
                actionFinished();
                return;
            }

            Job terminateEnvironmentsJob = new Job("Terminating Environments") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);
                    AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(endpoint);

                    List<Exception> errors = new ArrayList<>();
                    for (EnvironmentDescription env : environments) {
                        try {
                            beanstalk.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentId(env.getEnvironmentId()));
                        } catch (Exception e) {
                            errors.add(e);
                        }
                    }

                    IStatus status = Status.OK_STATUS;
                    if (errors.size() > 0) {
                        status = new MultiStatus(ElasticBeanstalkPlugin.PLUGIN_ID, 0, "Unable to terminate environments", null);
                        for (Exception error : errors) {
                            ((MultiStatus)status).add(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Unable to terminate environment", error));
                        }
                        actionFailed();
                    } else {
                        actionSucceeded();
                    }
                    actionFinished();

                    ContentProviderRegistry.refreshAllContentProviders();

                    return status;
                }
            };

            terminateEnvironmentsJob.schedule();
        }

    }

    private static class DeleteApplicationAction extends AwsAction {
        private final List<ApplicationDescription> applications;

        public DeleteApplicationAction(List<ApplicationDescription> applications) {
            super(AwsToolkitMetricType.EXPLORER_BEANSTALK_DELETE_APPLICATION);
            this.applications = applications;

            this.setText("Delete Application");
            this.setToolTipText("Delete the selected application");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        protected void doRun() {
            Dialog dialog = newConfirmationDialog("Delete selected application?", "Are you sure you want to delete the selected AWS Elastic Beanstalk applications?");
            if (dialog.open() != 0) {
                actionCanceled();
                actionFinished();
                return;
            }

            Job deleteApplicationsJob = new Job("Delete Applications") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);
                    AWSElasticBeanstalk beanstalk = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(endpoint);

                    List<Exception> errors = new ArrayList<>();
                    for (ApplicationDescription app : applications) {
                        try {
                            beanstalk.deleteApplication(new DeleteApplicationRequest().withApplicationName(app.getApplicationName()));
                        } catch (Exception e) {
                            errors.add(e);
                        }
                    }

                    IStatus status = Status.OK_STATUS;
                    if (errors.size() > 0) {
                        status = new MultiStatus(ElasticBeanstalkPlugin.PLUGIN_ID, 0, "Unable to delete applications", null);
                        for (Exception error : errors) {
                            ((MultiStatus)status).add(new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, "Unable to delete application", error));
                        }
                        actionFailed();
                    } else {
                        actionSucceeded();
                    }
                    actionFinished();

                    ContentProviderRegistry.refreshAllContentProviders();

                    return status;
                }
            };

            deleteApplicationsJob.schedule();
        }
    }

    private static Dialog newConfirmationDialog(String title, String message) {
        return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.WARNING, new String[] { "OK", "Cancel" }, 0);
    }
}
