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

/**
 * Utility functions for making SWT slightly more palatable.
 */
public final class WidgetUtils {

    /**
     * Wrap a widget in a composite that indents it a number of
     * pixels to the right.
     *
     * @param widget the widget to indent
     * @param pixels the number of pixels to indent by
     * @return a builder for the enclosing composite
     */
    public static CompositeBuilder indentRight(
        final WidgetBuilder widget,
        final int pixels
    ) {
        return new CompositeBuilder()
            .withoutMargins()
            .withLeftMargin(pixels)
            .withHorizontalAlignment(HorizontalAlignment.FILL)
            .withChild(widget);
    }

    /**
     * Wrap a widget in a composite that indents it a number of
     * pixels downwards.
     *
     * @param widget the widget to indent
     * @param pixels teh number of pixels to indent by
     * @return a builder for the enclosing composite
     */
    public static CompositeBuilder indentDown(
        final WidgetBuilder element,
        final int pixels
    ) {
        return new CompositeBuilder()
            .withoutMargins()
            .withTopMargin(pixels)
            .withVerticalAlignment(VerticalAlignment.FILL)
            .withChild(element);
    }

    /**
     * Wrap a set of widgets into a vertical column.
     *
     * @param widgets the widgets to wrap
     * @return a builder for the enclosing composite
     */
    public static CompositeBuilder column(final WidgetBuilder... widgets) {
        return new CompositeBuilder()
            .withoutMargins()
            .withFullHorizontalFill()
            .withChildren(widgets);
    }

    /**
     * Don't use me.
     */
    private WidgetUtils() {
    }
}
