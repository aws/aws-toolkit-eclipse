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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;

/**
 * A builder for widgets. This lets us write generic decorator methods (eg
 * "wrap these two widgets together one on top of each other") even though
 * SWT enforces a top-down UI creation model where each widget needs to know
 * its parent at construction time. Instead of directly taking a widget,
 * such methods can take a WidgetBuilder and create the wrapped widgets
 * on demand after they've created the container.
 */
public interface WidgetBuilder {
    /**
     * Build a widget with the given parent
     *
     * @param parent the parent for the widget
     * @return the new widget
     */
    Widget build(final Composite parent);
}