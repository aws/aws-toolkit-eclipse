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
import org.eclipse.swt.layout.GridData;

/**
 * An abstract widget builder, adding setters for various GridData properties
 * controlling the layout of the widget.
 *
 * @param <T> the runtime type of this object
 */
public abstract class AbstractWidgetBuilder<T> implements WidgetBuilder {

    private int style;
    private Color foregroundColor;
    private GridData layoutData;

    public int getStyle() {
        return style;
    }

    public T withStyle(final int value) {
        style = value;
        return getThis();
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public T withForegroundColor(final Color value) {
        foregroundColor = value;
        return getThis();
    }

    public GridData getLayoutData() {
        return layoutData;
    }

    public GridData getOrCreateLayoutData() {
        if (layoutData == null) {
            layoutData = new GridData();
        }
        return layoutData;
    }

    public T withLayoutData(final GridData value) {
        layoutData = value;
        return getThis();
    }

    public T withVerticalAlignment(final VerticalAlignment value) {
        getOrCreateLayoutData().verticalAlignment = value.getValue();
        return getThis();
    }

    public T withHorizontalAlignment(final HorizontalAlignment value) {
        getOrCreateLayoutData().horizontalAlignment = value.getValue();
        return getThis();
    }

    public T withWidthHint(final int value) {
        getOrCreateLayoutData().widthHint = value;
        return getThis();
    }

    public T withHeightHint(final int value) {
        getOrCreateLayoutData().heightHint = value;
        return getThis();
    }

    public T withHorizontalIndent(final int value) {
        getOrCreateLayoutData().horizontalIndent = value;
        return getThis();
    }

    public T withVerticalIndent(final int value) {
        getOrCreateLayoutData().verticalIndent = value;
        return getThis();
    }

    public T withHorizontalSpan(final int value) {
        getOrCreateLayoutData().horizontalSpan = value;
        return getThis();
    }

    public T withVerticalSpan(final int value) {
        getOrCreateLayoutData().verticalSpan = value;
        return getThis();
    }

    public T withHorizontalLandGrab(final boolean value) {
        getOrCreateLayoutData().grabExcessHorizontalSpace = value;
        return getThis();
    }

    public T withVerticalLandGrab(final boolean value) {
        getOrCreateLayoutData().grabExcessVerticalSpace = value;
        return getThis();
    }

    public T withMinimumWidth(final int value) {
        getOrCreateLayoutData().minimumWidth = value;
        return getThis();
    }

    public T withMinimumHeight(final int value) {
        getOrCreateLayoutData().minimumHeight = value;
        return getThis();
    }

    public T withFullHorizontalFill() {
        withHorizontalAlignment(HorizontalAlignment.FILL);
        return withHorizontalLandGrab(true);
    }

    @SuppressWarnings("unchecked")
    protected T getThis() {
        return (T) this;
    }
}
