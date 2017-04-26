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
package com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor;

import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.core.ui.MultiValueEditorDialog;

/**
 * Simple table dialog to allow use user to enter multiple values for an
 * attribute.
 */
class MultiValueAttributeEditorDialog extends MultiValueEditorDialog {

    private final SimpleDBItem item;
    private final String attributeName;

    public MultiValueAttributeEditorDialog(final Shell parentShell, final SimpleDBItem item, final String attributeName) {
        super(parentShell);
        this.item = item;
        this.attributeName = attributeName;
        this.values.addAll(this.item.attributes.get(this.attributeName));
    }
}
