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
package com.amazonaws.eclipse.core.ui;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.model.AbstractAwsResourceScopeParam.AwsResourceScopeParamBase;
import com.amazonaws.eclipse.core.model.SelectOrCreateKmsKeyDataModel;
import com.amazonaws.eclipse.core.model.SelectOrCreateKmsKeyDataModel.KmsKey;
import com.amazonaws.eclipse.core.ui.dialogs.AbstractInputDialog;
import com.amazonaws.eclipse.core.ui.dialogs.CreateKmsKeyDialog;

public class SelectOrCreateKmsKeyComposite extends SelectOrCreateComposite<KmsKey, SelectOrCreateKmsKeyDataModel, AwsResourceScopeParamBase> {

    public SelectOrCreateKmsKeyComposite(
            Composite parent,
            DataBindingContext bindingContext,
            SelectOrCreateKmsKeyDataModel dataModel) {
        super(parent, bindingContext, dataModel, "KMS Key:");
    }

    @Override
    protected AbstractInputDialog<KmsKey> createResourceDialog(AwsResourceScopeParamBase param) {
        return new CreateKmsKeyDialog(Display.getCurrent().getActiveShell(), param);
    }
}
