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

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.popup.AbstractAction;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.popup.providers.AbstractSubMenuActionProvider;
import org.eclipse.datatools.sqltools.tabledataeditor.actions.providers.DataGroupProvider;
import org.eclipse.jface.action.ActionContributionItem;

public class LoadDataActionProvider extends AbstractSubMenuActionProvider
{
    private static final AbstractAction action = new LoadDataAction();

    @Override
    protected String getSubMenuId()
    {
        return DataGroupProvider.DATA_MENU_ID;
    }

    @Override
    protected AbstractAction getAction()
    {
        return action;
    }

    @Override
    protected ActionContributionItem getActionContributionItem()
    {
        return this.ITEM;
    }
}
