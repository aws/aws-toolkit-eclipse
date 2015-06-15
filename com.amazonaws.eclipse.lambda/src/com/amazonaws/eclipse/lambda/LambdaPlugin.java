/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.lambda;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class LambdaPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.amazonaws.eclipse.lambda";

    public static final String DEFAULT_REGION = "us-east-1";

    /*
     * Preference store keys
     */
    public static final String PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT = "showReadmeAfterCreateNewProject";

    /** The shared instance */
    private static LambdaPlugin plugin;

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);

        initializePreferenceStoreDefaults();

        plugin = this;
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    private void initializePreferenceStoreDefaults() {
        getPreferenceStore().setDefault(PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT, true);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static LambdaPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * Convenience method for reporting the exception to StatusManager
     */
    public void reportException(String errorMessage, Throwable e) {
        StatusManager.getManager().handle(
                new Status(IStatus.ERROR, PLUGIN_ID,
                        errorMessage, e),
                        StatusManager.SHOW | StatusManager.LOG);
    }

    /**
     * Convenience method for logging a debug message at INFO level.
     */
    public void logInfo(String debugMessage) {
        getLog().log(new Status(Status.INFO, PLUGIN_ID, debugMessage, null));
    }

    public void warn(String message, Throwable e) {
        getLog().log(new Status(Status.WARNING, PLUGIN_ID, message, e));
    }
}
