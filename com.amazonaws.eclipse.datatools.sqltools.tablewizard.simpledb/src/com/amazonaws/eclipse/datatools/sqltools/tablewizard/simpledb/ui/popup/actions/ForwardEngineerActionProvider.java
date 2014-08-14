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

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

public class ForwardEngineerActionProvider  extends CommonActionProvider {
    private static final ForwardEngineerAction action = new ForwardEngineerAction();
    protected ISelectionProvider selectionProvider;
    protected CommonViewer viewer;
    protected ActionContributionItem ITEM;

    @Override
    public void init(final ICommonActionExtensionSite aSite) {
        super.init(aSite);
        this.selectionProvider = aSite.getViewSite().getSelectionProvider();
        this.viewer = (CommonViewer) aSite.getStructuredViewer();
        initActionContributionItem();
    }

    @Override
    public void fillContextMenu(final IMenuManager menu) {
        action.setCommonViewer(this.viewer);
        action.selectionChanged(new SelectionChangedEvent(this.selectionProvider, this.getContext().getSelection()));

        menu.insertAfter("slot3", action); //$NON-NLS-1$
    }

    protected void initActionContributionItem() {
        this.ITEM = new ActionContributionItem(action);
    }

}
