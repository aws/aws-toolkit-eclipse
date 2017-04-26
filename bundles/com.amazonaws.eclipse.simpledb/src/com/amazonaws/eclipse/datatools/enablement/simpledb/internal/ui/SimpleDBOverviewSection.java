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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui;

import org.eclipse.datatools.connectivity.ui.actions.AddProfileViewAction;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import com.amazonaws.eclipse.core.AwsUrls;
import com.amazonaws.eclipse.core.ui.overview.OverviewSection;

/**
 * Amazon SimpleDB OverviewSection implementation that provides overview content
 * for the SimpleDB Management functionality in the AWS Toolkit for Eclipse
 * (links to docs, shortcuts for creating new connections to SimpleDB, etc.)
 */
public class SimpleDBOverviewSection extends OverviewSection implements OverviewSection.V2 {

    private static final String SIMPLEDB_ECLIPSE_GETTING_STARTED_GUIDE_URL =
        "http://aws.amazon.com/eclipse/simpledb/gsg" + "?" + AwsUrls.TRACKING_PARAMS;

    private static final String SIMPLEDB_ECLIPSE_SCREENCAST_URL =
        "http://media.amazonwebservices.com/videos/eclipse-sdb-management-video.html" + "?" + AwsUrls.TRACKING_PARAMS;

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.core.ui.overview.OverviewSection#createControls(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControls(final Composite parent) {
        Composite tasksSection = this.toolkit.newSubSection(parent, "Tasks");
        this.toolkit.newListItem(tasksSection,
                "Connect to Amazon SimpleDB", null, new CreateNewSimpleDBConnectionAction());

        Composite resourcesSection = this.toolkit.newSubSection(parent, "Additional Resources");
        this.toolkit.newListItem(resourcesSection,
                "Video: Overview of Amazon SimpleDB Management in Eclipse",
                SIMPLEDB_ECLIPSE_SCREENCAST_URL, null);
        this.toolkit.newListItem(resourcesSection,
                "Getting Started with Amazon SimpleDB Management in Eclipse",
                SIMPLEDB_ECLIPSE_GETTING_STARTED_GUIDE_URL, null);
    }

    /**
     * Action class that opens the DTP Database Development perspective, and
     * brings up the new connection profile wizard.
     */
    private static class CreateNewSimpleDBConnectionAction extends Action {
        /** The ID of the Database Development perspective provided by DTP */
        private static final String DATABASE_DEVELOPMENT_PERSPECTIVE_ID =
            "org.eclipse.datatools.sqltools.sqleditor.perspectives.EditorPerspective";

        @Override
        public void run() {
            try {
                PlatformUI.getWorkbench().showPerspective(DATABASE_DEVELOPMENT_PERSPECTIVE_ID,
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow());
            } catch (WorkbenchException e) {
                e.printStackTrace();
            }

            new AddProfileViewAction().run();
        }
    }

}
