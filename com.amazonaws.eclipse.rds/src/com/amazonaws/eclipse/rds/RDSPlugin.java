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
package com.amazonaws.eclipse.rds;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.ui.dse.views.DataSourceExplorerView;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class RDSPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.amazonaws.eclipse.rds";

	// The shared instance
	private static RDSPlugin plugin;
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

    /**
     * Connects the specified connection profile and selects and reveals it in
     * the Data Source Explorer view.
     * 
     * @param profile
     *            The connection profile to connect and reveal.
     */
	public static void connectAndReveal(final IConnectionProfile profile) {
        IStatus connectStatus = profile.connect();
        if (connectStatus.isOK() == false) {
            Status status = new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, "Unable to connect to the database.  Make sure your password is correct and make sure you can access your database through your network and any firewalls you may be connecting through.");
            StatusManager.getManager().handle(status, StatusManager.BLOCK | StatusManager.LOG);
            return;
        }
        
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                try {
                    IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("org.eclipse.datatools.connectivity.DataSourceExplorerNavigator");
                    if (view instanceof DataSourceExplorerView) {
                        DataSourceExplorerView dse = (DataSourceExplorerView)view;
                        StructuredSelection selection = new StructuredSelection(profile);
                        dse.getCommonViewer().setSelection(selection, true);
                    }
                } catch (Exception e) {
                    Status status = new Status(IStatus.ERROR, RDSPlugin.PLUGIN_ID, "Unable to reveal connection profile: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static RDSPlugin getDefault() {
		return plugin;
	}
}
