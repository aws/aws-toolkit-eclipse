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
package com.amazonaws.eclipse.explorer.lambda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.core.model.KeyValueSetDataModel;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite.KeyValueEditingUiText;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite.KeyValueSetEditingCompositeBuilder;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.EnvironmentResponse;
import com.amazonaws.services.lambda.model.GetFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;

public class FunctionEnvVarsTable extends Composite {

    private final FunctionEditorInput functionEditorInput;
    private final KeyValueSetEditingComposite envVarsEditingComposite;
    private final KeyValueSetDataModel envVarsDataModel;

    public FunctionEnvVarsTable(Composite parent, FormToolkit toolkit, FunctionEditorInput functionEditorInput) {
        super(parent, SWT.NONE);
        this.functionEditorInput = functionEditorInput;

        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        envVarsDataModel = new KeyValueSetDataModel(-1, new ArrayList<Pair>());
        envVarsEditingComposite = new KeyValueSetEditingCompositeBuilder()
                .saveListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onSaveEnvVars();
                    }
                })
                .addKeyValidator(new EnvVarNameValidator())
                .uiText(new KeyValueEditingUiText(
                        "Add Environment Variable",
                        "Add a new Environment Variable",
                        "Edit Environment Variable",
                        "Edit the existing Environment Variable",
                        "Key:",
                        "Value:"))
                .build(this, envVarsDataModel);

        Composite buttonComposite = new Composite(this, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        buttonComposite.setLayout(new GridLayout(1, false));

        refresh();
    }

    public void refresh() {

        envVarsDataModel.getPairSet().clear();

        EnvironmentResponse environment = functionEditorInput.getLambdaClient()
                .getFunctionConfiguration(new GetFunctionConfigurationRequest()
                        .withFunctionName(functionEditorInput.getFunctionName()))
                .getEnvironment();

        if (environment != null && environment.getVariables() != null) {
            Map<String, String> envVarsMap = environment.getVariables();

            for (Entry<String, String> entry : envVarsMap.entrySet()) {
                envVarsDataModel.getPairSet().add(new Pair(entry.getKey(), entry.getValue()));
            }
        }
        envVarsEditingComposite.refresh();
    }

    private void onSaveEnvVars() {
        try {
            AWSLambda lambda = functionEditorInput.getLambdaClient();
            Map<String, String> envVarsMap = new HashMap<>();
            for (Pair pair : envVarsDataModel.getPairSet()) {
                envVarsMap.put(pair.getKey(), pair.getValue());
            }
            lambda.updateFunctionConfiguration(new UpdateFunctionConfigurationRequest()
                    .withFunctionName(functionEditorInput.getFunctionName())
                    .withEnvironment(new Environment()
                            .withVariables(envVarsMap)));
        } catch (AWSLambdaException e) {
            LambdaPlugin.getDefault().reportException(e.getMessage(), e);
        }
    }
}
