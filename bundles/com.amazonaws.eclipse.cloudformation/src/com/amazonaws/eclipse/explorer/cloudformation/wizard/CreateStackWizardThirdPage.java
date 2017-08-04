/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import static com.amazonaws.eclipse.cloudformation.CloudFormationConstants.MAX_ALLOWED_TAG_AMOUNT;
import static com.amazonaws.eclipse.cloudformation.CloudFormationConstants.MAX_TAG_KEY_LENGTH;
import static com.amazonaws.eclipse.cloudformation.CloudFormationConstants.MAX_TAG_VALUE_LENGTH;

import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.cloudformation.CloudFormationUtils;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite.KeyValueSetEditingCompositeBuilder;
import com.amazonaws.eclipse.core.validator.StringLengthValidator;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;
import com.amazonaws.services.cloudformation.model.Tag;

/**
 * Configuring stack tags page in create CloudFormation stack wizard.
 */
public class CreateStackWizardThirdPage extends WizardPage {
    private final CreateStackWizardDataModel dataModel;
    private KeyValueSetEditingComposite tagsEditingComposite;

    protected CreateStackWizardThirdPage(CreateStackWizardDataModel dataModel) {
        super("CloudFormation Stack Tags");
        setTitle("Options - Tags");
        setDescription(String.format("You can specify tags (key-value pairs) for resources in your stack. You can add up to %d unique key-value pairs for each stack.",
                MAX_ALLOWED_TAG_AMOUNT ));
        this.dataModel = dataModel;
        if (dataModel.getMode() == Mode.Update) {
            List<Tag> tags = CloudFormationUtils.getTags(dataModel.getStackName());
            if (tags != null && !tags.isEmpty()) {
                for (Tag tag : tags) {
                    dataModel.getTagModel().getPairSet().add(new Pair(tag.getKey(), tag.getValue()));
                }
            }
        }
    }

    @Override
    public void createControl(Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        createTagSection(composite);

        setControl(composite);
    }

    private void createTagSection(Composite parent) {
        tagsEditingComposite = new KeyValueSetEditingCompositeBuilder()
                .addKeyValidator(new StringLengthValidator(1, MAX_TAG_KEY_LENGTH,
                        String.format("The tag key length must be between %d and %d, inclusive.", 1, MAX_TAG_KEY_LENGTH)))
                .addValueValidator(new StringLengthValidator(1, MAX_TAG_VALUE_LENGTH,
                        String.format("The tag value length must be between %d and %d, inclusive", 1, MAX_TAG_VALUE_LENGTH)))
                .build(parent, dataModel.getTagModel());
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

}
