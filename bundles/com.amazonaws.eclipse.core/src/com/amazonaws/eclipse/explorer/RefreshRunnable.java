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
package com.amazonaws.eclipse.explorer;

import org.eclipse.jface.viewers.TreeViewer;

/**
 * Display-thread runnable to refresh an element in the tree.
 */
public final class RefreshRunnable implements Runnable {

    private final Object parentElement;
    private final TreeViewer viewer;

    public RefreshRunnable(TreeViewer viewer, Object parentElement) {
        this.parentElement = parentElement;
        this.viewer = viewer;
    }

    @Override
    public void run() {
        this.viewer.refresh(parentElement);
    }
}
