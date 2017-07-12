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

import static com.amazonaws.eclipse.lambda.LambdaConstants.MAX_LAMBDA_TAGS;
import static com.amazonaws.eclipse.lambda.LambdaConstants.MAX_LAMBDA_TAG_KEY_LENGTH;
import static com.amazonaws.eclipse.lambda.LambdaConstants.MAX_LAMBDA_TAG_VALUE_LENGTH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite.KeyValueSetEditingCompositeBuilder;
import com.amazonaws.eclipse.core.validator.StringLengthValidator;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.model.ListTagsRequest;
import com.amazonaws.services.lambda.model.TagResourceRequest;
import com.amazonaws.services.lambda.model.UntagResourceRequest;

public class FunctionTagsTable extends Composite {

    private final FunctionEditorInput functionEditorInput;
    private final KeyValueSetEditingComposite tagsEditingComposite;
    private final KeyValueSetDataModel tagsDataModel;

    public FunctionTagsTable(Composite parent, FormToolkit toolkit, FunctionEditorInput functionEditorInput) {
        super(parent, SWT.NONE);
        this.functionEditorInput = functionEditorInput;

        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        tagsDataModel = new KeyValueSetDataModel(MAX_LAMBDA_TAGS, new ArrayList<Pair>());
        tagsEditingComposite = new KeyValueSetEditingCompositeBuilder()
                .addKeyValidator(new StringLengthValidator(1, MAX_LAMBDA_TAG_KEY_LENGTH,
                        String.format("This field is too long. Maximum length is %d characters.", MAX_LAMBDA_TAG_KEY_LENGTH)))
                .addValueValidator(new StringLengthValidator(0, MAX_LAMBDA_TAG_VALUE_LENGTH,
                        String.format("This field is too long. Maximum length is %d characters.", MAX_LAMBDA_TAG_VALUE_LENGTH)))
                .addKeyValidator(new LambdaTagNameValidator())
                .saveListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onSaveTags();
                    }
                })
                .build(this, tagsDataModel);

        Composite buttonComposite = new Composite(this, SWT.NONE);
        buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        buttonComposite.setLayout(new GridLayout(1, false));

        refresh();
    }

    public void refresh() {
        Map<String, String> tagMap = functionEditorInput.getLambdaClient()
                .listTags(new ListTagsRequest()
                        .withResource(functionEditorInput.getFunctionArn()))
                .getTags();
        tagsDataModel.getPairSet().clear();
        for (Entry<String, String> entry : tagMap.entrySet()) {
            tagsDataModel.getPairSet().add(new Pair(entry.getKey(), entry.getValue()));
        }
        tagsEditingComposite.refresh();
    }

    private void onSaveTags() {
        try {
            AWSLambda lambda = functionEditorInput.getLambdaClient();
            Map<String, String> oldTagMap = lambda
                    .listTags(new ListTagsRequest()
                            .withResource(functionEditorInput.getFunctionArn()))
                    .getTags();
            List<String> tagKeysToBeRemoved = new ArrayList<>();
            for (String key : oldTagMap.keySet()) {
                if (!tagsDataModel.getPairSet().contains(key)) {
                    tagKeysToBeRemoved.add(key);
                }
            }
            Map<String, String> tagMap = new HashMap<>();
            for (Pair pair : tagsDataModel.getPairSet()) {
                tagMap.put(pair.getKey(), pair.getValue());
            }
            if (!tagKeysToBeRemoved.isEmpty()) {
                lambda.untagResource(new UntagResourceRequest()
                    .withResource(functionEditorInput.getFunctionArn())
                    .withTagKeys(tagKeysToBeRemoved));
            }
            lambda.tagResource(new TagResourceRequest()
                    .withResource(functionEditorInput.getFunctionArn())
                    .withTags(tagMap));
        } catch (AWSLambdaException e) {
            LambdaPlugin.getDefault().reportException(e.getMessage(), e);
        }
    }
}
