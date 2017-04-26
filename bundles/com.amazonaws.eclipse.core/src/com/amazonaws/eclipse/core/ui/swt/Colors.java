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
import org.eclipse.swt.widgets.Display;

/**
 * Handy strongly-typed constants for colors, since SWT.COLOR_* is gross.
 */
public final class Colors {

    public static final Color GRAY      = getColor(SWT.COLOR_GRAY);
    public static final Color DARK_GRAY = getColor(SWT.COLOR_DARK_GRAY);

    // TODO: Add more colors here as needed.

    private Colors() {
    }

    /**
     * Get a strongly-typed representation of the given color id.
     *
     * @param id a color id (from SWT.COLOR_*)
     * @return the strongly typed Color representation
     */
    private static Color getColor(final int id) {
        return Display.getCurrent().getSystemColor(id);
    }
}
