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

import java.util.Arrays;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.ui.SelectOrInputComposite;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionAliasesScopeParam;
import com.amazonaws.eclipse.lambda.model.SelectOrInputFunctionAliasDataModel;
import com.amazonaws.services.lambda.model.AliasConfiguration;

public class SelectOrInputFunctionAliasComposite
        extends SelectOrInputComposite<AliasConfiguration, SelectOrInputFunctionAliasDataModel, LambdaFunctionAliasesScopeParam> {

    public SelectOrInputFunctionAliasComposite(
            Composite parent,
            DataBindingContext bindingContext,
            SelectOrInputFunctionAliasDataModel dataModel) {

        super(parent, bindingContext, dataModel,
                "Choose an existing function alias:",
                "Create a new function alias:",
                Arrays.asList(new NotEmptyValidator("Alias name must not be empty")));
    }
}