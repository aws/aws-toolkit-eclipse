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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;
import org.eclipse.wst.server.ui.internal.ImageResource;

import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.AwsAction;

@SuppressWarnings("restriction")
public class EventLogEditorPart extends ServerEditorPart {

    private ManagedForm managedForm;
    private EventLogEditorSection eventLog;

    @Override
    public void createPartControl(Composite parent) {
        managedForm = new ManagedForm(parent);
        setManagedForm(managedForm);
        ScrolledForm form = managedForm.getForm();
        FormToolkit toolkit = managedForm.getToolkit();
        toolkit.decorateFormHeading(form.getForm());
        form.setText("Event Log");
        form.setImage(ImageResource.getImage(ImageResource.IMG_SERVER));

        Composite columnComp = toolkit.createComposite(form.getBody());
        FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        columnComp.setLayout(new FillLayout());
        form.getBody().setLayout(layout);

        eventLog = new EventLogEditorSection();
        eventLog.setServerEditorPart(this);
        eventLog.init(this.getEditorSite(), this.getEditorInput());
        eventLog.createSection(columnComp);

        managedForm.getForm().getToolBarManager().add(new AwsAction(
                AwsToolkitMetricType.EXPLORER_BEANSTALK_REFRESH_ENVIRONMENT_EDITOR,
                "Refresh", SWT.None) {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh");
            }
            @Override
            protected void doRun() {
                eventLog.refresh();
                actionFinished();
            }
        });
        managedForm.getForm().getToolBarManager().update(true);

        form.reflow(true);
    }

    @Override
    public void setFocus() {
        managedForm.setFocus();
        eventLog.refresh();
    }
}
