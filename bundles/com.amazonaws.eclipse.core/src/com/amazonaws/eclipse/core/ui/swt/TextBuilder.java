/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Builder for text boxes, which contain copyable (and potentially user-
 * editable) text.
 */
public class TextBuilder extends AbstractWidgetBuilder<TextBuilder> {

    private PostBuildHook<Text> postBuildHook;

    private boolean readOnly;
    private Color backgroundColor;

    public TextBuilder() {
        // Default to having a border.
        super.withStyle(SWT.BORDER);
    }

    public TextBuilder withoutEditing() {
        readOnly = true;
        return this;
    }

    public TextBuilder withBackgroundColor(final Color value) {
        backgroundColor = value;
        return this;
    }

    public TextBuilder withPostBuildHook(
        final PostBuildHook<Text> value
    ) {
        postBuildHook = value;
        return this;
    }

    @Override
    public Text build(final Composite parent) {
        Text text = new Text(parent, getStyle());

        if (readOnly) {
            text.setEditable(true);
        }

        if (backgroundColor != null) {
            text.setBackground(backgroundColor);
        }

        if (getLayoutData() != null) {
            text.setLayoutData(getLayoutData());
        }

        if (postBuildHook != null) {
            postBuildHook.run(text);
        }

        return text;
    }
}
