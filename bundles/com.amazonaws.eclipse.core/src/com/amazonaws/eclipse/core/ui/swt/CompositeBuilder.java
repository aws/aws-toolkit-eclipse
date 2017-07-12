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

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A builder for Composites, which group together several widgets with some
 * rules for how to lay them out relative to each other.
 */
public final class CompositeBuilder
    extends AbstractWidgetBuilder<CompositeBuilder> {

    private final ArrayList<WidgetBuilder> children;
    private GridLayout layout;

    public CompositeBuilder() {
        children = new ArrayList<>();
    }

    public GridLayout getLayout() {
        return layout;
    }

    public GridLayout getOrCreateLayout() {
        if (layout == null) {
            layout = new GridLayout();
        }
        return layout;
    }

    public CompositeBuilder withLayout(final GridLayout value) {
        layout = value;
        return this;
    }

    public CompositeBuilder withColumns(final int value) {
        getOrCreateLayout().numColumns = value;
        return this;
    }

    public CompositeBuilder withEqualWidthColumns(final boolean value) {
        getOrCreateLayout().makeColumnsEqualWidth = value;
        return this;
    }

    public CompositeBuilder withMarginWidth(final int value) {
        getOrCreateLayout().marginWidth = value;
        return this;
    }

    public CompositeBuilder withMarginHeight(final int value) {
        getOrCreateLayout().marginHeight = value;
        return this;
    }

    public CompositeBuilder withLeftMargin(final int value) {
        getOrCreateLayout().marginLeft = value;
        return this;
    }

    public CompositeBuilder withRightMargin(final int value) {
        getOrCreateLayout().marginRight = value;
        return this;
    }

    public CompositeBuilder withTopMargin(final int value) {
        getOrCreateLayout().marginTop = value;
        return this;
    }

    public CompositeBuilder withBottomMargin(final int value) {
        getOrCreateLayout().marginBottom = value;
        return this;
    }

    public CompositeBuilder withMargins(final int width, final int height) {
        return withMarginWidth(width).withMarginHeight(height);
    }

    public CompositeBuilder withoutMargins() {
        return withMargins(0, 0);
    }

    public CompositeBuilder withHorizontalSpacing(final int value) {
        getOrCreateLayout().horizontalSpacing = value;
        return this;
    }

    public CompositeBuilder withVerticalSpacing(final int value) {
        getOrCreateLayout().verticalSpacing = value;
        return this;
    }

    public CompositeBuilder withChild(final WidgetBuilder value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        children.add(value);
        return this;
    }

    public CompositeBuilder withChildren(
        final WidgetBuilder... values
    ) {
        children.addAll(Arrays.asList(values));
        return this;
    }

    @Override
    public Composite build(final Composite parent) {
        Composite composite = new Composite(parent, getStyle());

        if (layout != null) {
            composite.setLayout(layout);
        }

        if (getLayoutData() != null) {
            composite.setLayoutData(getLayoutData());
        }

        for (WidgetBuilder child : children) {
            child.build(composite);
        }

        return composite;
    }
}