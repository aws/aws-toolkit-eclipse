/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.ui;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.ui.SelectOrCreateComposite;
import com.amazonaws.eclipse.core.ui.dialogs.AbstractInputDialog;
import com.amazonaws.eclipse.lambda.model.SelectOrCreateBasicLambdaRoleDataModel;
import com.amazonaws.eclipse.lambda.upload.wizard.dialog.CreateBasicLambdaRoleDialog;
import com.amazonaws.services.identitymanagement.model.Role;

public class SelectOrCreateBasicLambdaRoleComposite
        extends SelectOrCreateComposite<Role, SelectOrCreateBasicLambdaRoleDataModel, AwsResourceScopeParamBase> {

    public SelectOrCreateBasicLambdaRoleComposite(Composite parent, DataBindingContext bindingContext,
            SelectOrCreateBasicLambdaRoleDataModel dataModel) {
        super(parent, bindingContext, dataModel, "IAM Role:");
    }

    @Override
    protected AbstractInputDialog<Role> createResourceDialog(AwsResourceScopeParamBase param) {
        return new CreateBasicLambdaRoleDialog(Display.getCurrent().getActiveShell(), param);
    }
}
