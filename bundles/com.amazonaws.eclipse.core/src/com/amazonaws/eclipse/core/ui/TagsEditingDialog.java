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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.model.KeyValueSetDataModel;
import com.amazonaws.eclipse.core.ui.KeyValueSetEditingComposite.KeyValueSetEditingCompositeBuilder;
import com.amazonaws.eclipse.core.validator.StringLengthValidator;

public class TagsEditingDialog extends TitleAreaDialog {
    private final KeyValueSetDataModel dataModel;

    private final String title;
    private final String message;
    private final int maxKeyLength;
    private final int maxValueLength;
    private final SelectionListener saveListener;

    private TagsEditingDialog(Shell parentShell, KeyValueSetDataModel dataModel,
            String title, String message,
            int maxKeyLength, int maxValueLength,
            SelectionListener saveListener) {
        super(parentShell);
        this.dataModel = dataModel;
        this.title = title;
        this.message = message;
        this.maxKeyLength = maxKeyLength;
        this.maxValueLength = maxValueLength;
        this.saveListener = saveListener;
    }

    @Override
    public void create() {
        super.create();
        setTitle(title);
        setMessage(message, IMessageProvider.INFORMATION);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        return new KeyValueSetEditingCompositeBuilder()
                .addKeyValidator(new StringLengthValidator(1, maxKeyLength,
                            String.format("This field is too long. Maximum length is %d characters.", maxKeyLength)))
                .addValueValidator(new StringLengthValidator(0, maxValueLength,
                            String.format("This field is too long. Maximum length is %d characters.", maxValueLength)))
                .saveListener(saveListener)
                .build(parent, dataModel);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    public static class TagsEditingDialogBuilder {
        private String title = "Edit Tags";
        private String message = "Tag objects to search, organize and manage access";
        private int maxKeyLength = 128;
        private int maxValueLength = 256;
        private SelectionListener saveListener;

        public TagsEditingDialog build(Shell parentShell, KeyValueSetDataModel dataModel) {
            return new TagsEditingDialog(parentShell, dataModel, title, message, maxKeyLength, maxValueLength, saveListener);
        }

        public TagsEditingDialogBuilder title(String title) {
            this.title = title;
            return this;
        }

        public TagsEditingDialogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public TagsEditingDialogBuilder maxKeyLength(int maxKeyLength) {
            this.maxKeyLength = maxKeyLength;
            return this;
        }

        public TagsEditingDialogBuilder maxValueLength(int maxValueLength) {
            this.maxValueLength = maxValueLength;
            return this;
        }

        public TagsEditingDialogBuilder saveListener(SelectionListener saveListener) {
            this.saveListener = saveListener;
            return this;
        }
    }
}
