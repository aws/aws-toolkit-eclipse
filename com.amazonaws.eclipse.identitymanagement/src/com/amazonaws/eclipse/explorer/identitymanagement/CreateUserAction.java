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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.identitymanagement.user.CreateUserWizard;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class CreateUserAction extends Action {

    private final IRefreshable refreshable;
    private AmazonIdentityManagement iam;


    public CreateUserAction(AmazonIdentityManagement iam, IRefreshable refreshable) {
        this.refreshable = refreshable;
        setToolTipText("Create New Users");
        this.iam = iam;
    }

    public CreateUserAction(IRefreshable refreshable) {
           this(null, refreshable);
    }

    public CreateUserAction() {
        this(null, null);
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD);
    }

    @Override
    public String getText() {
        return "Create New Users";
    }

    @Override
    public void run() {
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), new CreateUserWizard(iam, refreshable));
        dialog.open();
    }

}
