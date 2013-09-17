/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.identitymanagement;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class PasswordPolicyEditor extends EditorPart {

    private EditorInput editorInput;

    private PasswordPolicyForm passwordPolicyForm;

    private AmazonIdentityManagement iam;

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        this.editorInput = (EditorInput) input;
        iam = getClient();
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
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(getFormTitle());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_KEY));
        form.getBody().setLayout(new GridLayout());

        passwordPolicyForm = new PasswordPolicyForm(iam, form.getBody(), toolkit);
        passwordPolicyForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().update(true);

    }

    private String getFormTitle() {
        String formTitle = editorInput.getName();
        return formTitle;
    }

    @Override
    public void setFocus() {
    }

    protected AmazonIdentityManagement getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(editorInput.getAccountId());
        return clientFactory.getIAMClientByEndpoint(editorInput.getRegionEndpoint());
    }

    private class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            passwordPolicyForm.refresh();
        }
    }

}
