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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Builder for labels, which are non-editable and non-copyable text.
 */
public class LabelBuilder extends AbstractWidgetBuilder<LabelBuilder> {

    private final String text;

    public LabelBuilder(final String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }

        this.text = text;
    }

    public LabelBuilder(final String text, final Color foregroundColor) {
        this(text);
        withForegroundColor(foregroundColor);
    }

    @Override
    public Label build(final Composite parent) {
        Label label = new Label(parent, getStyle());
        label.setText(text);

        if (getForegroundColor() != null) {
            label.setForeground(getForegroundColor());
        }

        if (getLayoutData() != null) {
            label.setLayoutData(getLayoutData());
        }

        return label;
    }
}