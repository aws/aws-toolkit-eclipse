/*
 * Copyright 2009-2011 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb.ui;

import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.datatools.sqltools.tablewizard.simpledb"; //$NON-NLS-1$

    // The shared instance
    private static UIPlugin plugin;

    /**
     * The constructor
     */
    public UIPlugin() {
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static UIPlugin getDefault() {
        return plugin;
    }

    /**
     * Helper method for easier error logging.
     * 
     * @param mess
     * @param e
     * @param severity
     */
    public static void logMessage(final String mess, final Exception e, final int severity) {
        final Status status = new Status(severity, PLUGIN_ID, 1, "SimpleDB: " + mess, e); //$NON-NLS-1$
        getDefault().getLog().log(status);
    }
}
