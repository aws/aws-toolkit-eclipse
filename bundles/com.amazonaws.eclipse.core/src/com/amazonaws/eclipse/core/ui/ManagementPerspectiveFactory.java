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
package com.amazonaws.eclipse.core.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class ManagementPerspectiveFactory implements IPerspectiveFactory {

    /** Folder ID where the Amazon EC2 views will be opened */
    private static final String BOTTOM_FOLDER_ID = "bottom";

    /** Folder ID where the AWS Toolkit Overview will be opened */
    private static final String LEFT_FOLDER_ID = "left";

    /** Project Explorer view ID (not available through IPageLayout in 3.4) */
    private static final String ORG_ECLIPSE_UI_NAVIGATOR_PROJECT_EXPLORER
        = "org.eclipse.ui.navigator.ProjectExplorer";

    /**
     * The Package Explorer view from JDT. We create a placeholder for this
     * view, so that if a user has this view open in another perspective,
     * they'll see it here, too, otherwise they won't see it.
     */
    private static final String JDT_PACKAGE_EXPLORER_VIEW_ID = "org.eclipse.jdt.ui.PackageExplorer";


    @Override
    public void createInitialLayout(IPageLayout layout) {
        IFolderLayout left = layout.createFolder(LEFT_FOLDER_ID, IPageLayout.LEFT, 0.30f, layout.getEditorArea());
        left.addView("com.amazonaws.eclipse.explorer.view");
        left.addView(ORG_ECLIPSE_UI_NAVIGATOR_PROJECT_EXPLORER);
        left.addPlaceholder(JDT_PACKAGE_EXPLORER_VIEW_ID);

        IFolderLayout bot = layout.createFolder(BOTTOM_FOLDER_ID, IPageLayout.BOTTOM, 0.50f, layout.getEditorArea());
        bot.addView(IPageLayout.ID_PROGRESS_VIEW);
    }
}
