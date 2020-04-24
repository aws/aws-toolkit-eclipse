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
package com.amazonaws.eclipse.codedeploy.explorer.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView;
import com.amazonaws.eclipse.codedeploy.explorer.image.CodeDeployExplorerImages;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.model.AutoScalingGroup;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.EC2TagFilter;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;

public class DeploymentGroupEditor extends EditorPart {

    public final static String ID = "com.amazonaws.eclipse.codedeploy.explorer.editor.deploymentGroupEditor";

    private DeploymentGroupEditorInput deploymentGroupEditorInput;

    private DeploymentsTableView deploymentsTable;

    public DeploymentsTableView getDeploymentsTableView() {
        return deploymentsTable;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        deploymentGroupEditorInput = (DeploymentGroupEditorInput) input;
        setPartName(input.getName());
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {
        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(deploymentGroupEditorInput.getName());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry()
                .get(CodeDeployExplorerImages.IMG_DEPLOYMENT_GROUP));
        form.getBody().setLayout(new GridLayout(1, false));

        createDeploymentGroupSummary(form, toolkit);
        createDeploymentHistoryTable(form, toolkit);

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().update(true);
    }

    private class RefreshAction extends AwsAction {
        public RefreshAction() {
            super(AwsToolkitMetricType.EXPLORER_CODEDEPLOY_REFRESH_DEPLOYMENT_GROUP_EDITOR);
            this.setText("Refresh");
            this.setToolTipText("Refresh deployment history");
            this.setImageDescriptor(AwsToolkitCore.getDefault()
                    .getImageRegistry()
                    .getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        protected void doRun() {
            deploymentsTable.refreshAsync();
            actionFinished();
        }
    }

    /**
     * Creates the table of deployment histories
     */
    private void createDeploymentHistoryTable(final ScrolledForm form,
            final FormToolkit toolkit) {
        deploymentsTable = new DeploymentsTableView(
                deploymentGroupEditorInput,
                form.getBody(),
                toolkit,
                SWT.None);

        deploymentsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    /**
     * Creates a summary of a the deployment group
     */
    private void createDeploymentGroupSummary(final ScrolledForm form, final FormToolkit toolkit) {

        final Composite parent = toolkit.createComposite(form.getBody(), SWT.None);
        parent.setLayout(new GridLayout(2, false));

        toolkit.createLabel(parent, "Deployment Group info loading");
        toolkit.createLabel(parent, "");

        new Thread() {
            @Override
            public void run() {

                AmazonCodeDeploy codeDeployClient = deploymentGroupEditorInput
                        .getCodeDeployClient();

                DeploymentGroupInfo deployGroupInfo = codeDeployClient.getDeploymentGroup(
                        new GetDeploymentGroupRequest()
                                .withApplicationName(deploymentGroupEditorInput.getApplicationName())
                                .withDeploymentGroupName(deploymentGroupEditorInput.getDeploymentGroupName())
                                )
                                .getDeploymentGroupInfo();

                if ( deployGroupInfo == null )
                    return;

                updateComposite(form, toolkit, deployGroupInfo);
            }

            protected void updateComposite(final ScrolledForm form,
                    final FormToolkit toolkit,
                    final DeploymentGroupInfo deployGroup) {

                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        for ( Control c : parent.getChildren() ) {
                            c.dispose();
                        }

                        toolkit.createLabel(parent, "Application Name: ");
                        toolkit.createText(parent, deployGroup.getApplicationName(), SWT.READ_ONLY);
                        toolkit.createLabel(parent, "Deployment Group Name: ");
                        toolkit.createText(parent, deployGroup.getDeploymentGroupName(), SWT.READ_ONLY);
                        toolkit.createLabel(parent, "Deployment Group ID: ");
                        toolkit.createText(parent, deployGroup.getDeploymentGroupId(), SWT.READ_ONLY);
                        toolkit.createLabel(parent, "Service Role ARN: ");
                        toolkit.createText(parent, deployGroup.getServiceRoleArn(), SWT.READ_ONLY);
                        toolkit.createLabel(parent, "Deployment Configuration: ");
                        toolkit.createText(parent, deployGroup.getDeploymentConfigName(), SWT.READ_ONLY);

                        if (deployGroup.getEc2TagFilters() != null &&
                                !deployGroup.getEc2TagFilters().isEmpty()) {
                            toolkit.createLabel(parent, "Amazon EC2 Tags: ");
                            StringBuilder tags = new StringBuilder();
                            boolean first = true;
                            for (EC2TagFilter tag : deployGroup.getEc2TagFilters()) {
                                if (first) {
                                    first = false;
                                } else {
                                    tags.append(", ");
                                }

                                if ("KEY_AND_VALUE".equals(tag.getType())) {
                                    tags.append(tag.getKey() + ":" + tag.getValue());
                                } else if ("KEY_ONLY".equals(tag.getType())) {
                                    tags.append(tag.getKey() + "(KEY_ONLY)");
                                } else if ("VALUE_ONLY".equals(tag.getType())) {
                                    tags.append(tag.getValue() + "(VALUE_ONLY)");
                                }
                            }
                            toolkit.createText(parent, tags.toString(), SWT.READ_ONLY);
                        }

                        if (deployGroup.getAutoScalingGroups() != null &&
                                !deployGroup.getAutoScalingGroups().isEmpty()) {
                            toolkit.createLabel(parent, "Associated Auto Scaling Groups: ");
                            StringBuilder groups = new StringBuilder();
                            boolean first = true;
                            for (AutoScalingGroup group : deployGroup.getAutoScalingGroups()) {
                                if (first) {
                                    first = false;
                                } else {
                                    groups.append(", ");
                                }
                                groups.append(group.getName() + ":" + group.getHook());
                            }
                            toolkit.createText(parent, groups.toString(), SWT.READ_ONLY);
                        }

                        form.reflow(true);
                    }
                });
            }

        }.start();
    }

    @Override
    public void setFocus() {
    }
}
