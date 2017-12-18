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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.lambda.project.listener.LambdaProjectChangeTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class LambdaPlugin extends AbstractAwsPlugin {

    public static final String PLUGIN_ID = "com.amazonaws.eclipse.lambda";

    public static final String DEFAULT_REGION = "us-east-1";
    public static final String IMAGE_LAMBDA = "lambda-service";
    public static final String IMAGE_FUNCTION = "function";
    public static final String IMAGE_SAM_LOCAL = "sam-local";

    private static final Map<String, String> IMAGE_REGISTRY_MAP = new HashMap<>();

    static {
        IMAGE_REGISTRY_MAP.put(IMAGE_LAMBDA, "/icons/lambda-service.png");
        IMAGE_REGISTRY_MAP.put(IMAGE_FUNCTION, "/icons/function.png");
        IMAGE_REGISTRY_MAP.put(IMAGE_SAM_LOCAL, "/icons/sam-local.png");
    }

    /*
     * Preference store keys
     */
    public static final String PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT = "showReadmeAfterCreateNewProject";

    /** The shared instance */
    private static LambdaPlugin plugin;

    private final LambdaProjectChangeTracker projectChangeTracker = new LambdaProjectChangeTracker();

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        initializePreferenceStoreDefaults();
        projectChangeTracker.clearDirtyFlags();
        projectChangeTracker.start();
    }

    /**
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        projectChangeTracker.clearDirtyFlags();
        projectChangeTracker.stop();
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

    public LambdaProjectChangeTracker getProjectChangeTracker() {
        return projectChangeTracker;
    }

    /**
     * Print the message in system.out if Eclipse is running in debugging mode.
     */
    public void trace(String message) {
        if (Platform.inDebugMode()) {
            System.out.println(message);
        }
    }

    @Override
    protected Map<String, String> getImageRegistryMap() {
        return IMAGE_REGISTRY_MAP;
    }
}
