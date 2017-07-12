/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.datatools.connectivity.ui.actions.AddProfileViewAction;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import com.amazonaws.eclipse.core.AwsToolkitCore;

public class NewSimpleDBConnectionHandler extends AbstractHandler {

    /** The ID of the Database Development perspective provided by DTP */
    private static final String DATABASE_DEVELOPMENT_PERSPECTIVE_ID =
        "org.eclipse.datatools.sqltools.sqleditor.perspectives.EditorPerspective";

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        try {
            PlatformUI.getWorkbench().showPerspective(DATABASE_DEVELOPMENT_PERSPECTIVE_ID,
                PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            new AddProfileViewAction().run();
        } catch (WorkbenchException e) {
            AwsToolkitCore.getDefault().reportException("Unable to open connection wizard", e);
        }

        return null;
    }
}
