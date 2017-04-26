/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.popup.actions;

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.icons.ImageDescription;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.navigator.CommonViewer;

import com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui.Messages;

public class ForwardEngineerAction extends Action {
    private static final String TEXT = Messages.GENERATE_DDL_MENU_TEXT;
    private static final ImageDescriptor descriptor = ImageDescription.getGenerateCodeDescriptor();

    protected SelectionChangedEvent event;
    protected CommonViewer viewer;

    public ForwardEngineerAction() {
        this.setImageDescriptor(descriptor);
        this.setDisabledImageDescriptor(descriptor);
        this.setText(TEXT);
        this.setToolTipText(TEXT);
    }

    public void setCommonViewer(final CommonViewer viewer) {
        this.viewer = viewer;
    }

    public void selectionChanged(final SelectionChangedEvent event) {
        this.event = event;
        setEnabled(false);
    }

    @Override
    public void run() {
    }
}
