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

import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import com.amazonaws.eclipse.core.AccountInfoChangeListener;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.DefaultRegionChangeRefreshListener;
import com.amazonaws.eclipse.core.ui.IRefreshable;

public class ResourcesView extends CommonNavigator implements IRefreshable {

    private static final ExplorerNodeOpenListener EXPLORER_NODE_OPEN_LISTENER = new ExplorerNodeOpenListener();

    public static final AWSResourcesRootElement rootElement = new AWSResourcesRootElement();

    private CommonViewer viewer;

    private DefaultRegionChangeRefreshListener defaultRegionChangeRefreshListener;

    private AccountChangeListener accountChangeListener;

    @Override
    protected Object getInitialInput() {
        return rootElement;
    }

    @Override
    protected CommonViewer createCommonViewerObject(Composite aParent) {
        viewer = super.createCommonViewerObject(aParent);
        viewer.addOpenListener(EXPLORER_NODE_OPEN_LISTENER);
        viewer.setAutoExpandLevel(1);
        return viewer;
    }

    @Override
    public void dispose() {
        getCommonViewer().removeOpenListener(EXPLORER_NODE_OPEN_LISTENER);
        if (defaultRegionChangeRefreshListener != null) {
            defaultRegionChangeRefreshListener.stopListening();
        }
        if (accountChangeListener != null) {
            AwsToolkitCore.getDefault().removeAccountInfoChangeListener(accountChangeListener);
        }
    }

    @Override
    public void init(IViewSite aSite, IMemento aMemento) throws PartInitException {
        super.init(aSite, aMemento);
        accountChangeListener = new AccountChangeListener();
        AwsToolkitCore.getDefault().addAccountInfoChangeListener(accountChangeListener);
        defaultRegionChangeRefreshListener = new DefaultRegionChangeRefreshListener(this);
    }

    private class AccountChangeListener extends AccountInfoChangeListener {
        @Override
        public void currentAccountChanged() {
            ContentProviderRegistry.clearAllCachedResponses();

            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    viewer.refresh();
                    viewer.collapseAll();
                }
            });
        }
    }

    private static final class ExplorerNodeOpenListener implements IOpenListener {
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection)event.getSelection();

            Iterator<?> i = selection.iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                if (obj instanceof ExplorerNode) {
                    ExplorerNode explorerNode = (ExplorerNode)obj;
                    IAction openAction = explorerNode.getOpenAction();
                    if (openAction != null) openAction.run();
                }
            }
        }

    }

    public void refreshData() {
        ContentProviderRegistry.clearAllCachedResponses();

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                viewer.refresh();
                viewer.collapseAll();
            }
        });

    }
}
