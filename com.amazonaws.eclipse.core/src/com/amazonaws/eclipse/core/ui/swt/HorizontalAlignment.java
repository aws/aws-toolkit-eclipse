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

/**
 * SWT.* is a mess, spliting this out into an enum for things that are
 * actually legal.
 */
public enum HorizontalAlignment {

    LEFT(SWT.LEFT),
    CENTER(SWT.CENTER),
    RIGHT(SWT.RIGHT),
    FILL(SWT.FILL);

    private final int value;

    private HorizontalAlignment(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}