/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;

public class DeploymentConfigEditorSection extends ServerEditorSection {

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        String description = "Incremental deployment publishes only the changes in your project since your last deployment, which means faster deployments.";

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
            | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("AWS Elastic Beanstalk Deployment");
        section.setDescription(description);
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));



        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(composite);
        section.setClient(composite);




        final Button incrementalDeploymentCheckbox = toolkit.createButton(composite, "Use Incremental Deployments", SWT.CHECK);

        final Environment environment = (Environment)server.getAdapter(Environment.class);
        incrementalDeploymentCheckbox.setSelection(environment.getIncrementalDeployment());

        incrementalDeploymentCheckbox.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                execute(new AbstractOperation("Incremental Deployments") {

                    private boolean originalState;

                    @Override
                    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
                        environment.setIncrementalDeployment(originalState);
                        incrementalDeploymentCheckbox.setSelection(originalState);
                        return Status.OK_STATUS;
                    }

                    @Override
                    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
                        environment.setIncrementalDeployment(!originalState);
                        incrementalDeploymentCheckbox.setSelection(!originalState);
                        return Status.OK_STATUS;
                    }

                    @Override
                    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
                        originalState = environment.getIncrementalDeployment();
                        environment.setIncrementalDeployment(incrementalDeploymentCheckbox.getSelection());
                        return Status.OK_STATUS;
                    }
                });
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

    }

}
