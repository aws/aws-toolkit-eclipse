/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.dynamodb.preferences.TestToolPreferencePage;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class DynamoDBPlugin extends AbstractAwsPlugin {

    public static final String IMAGE_ONE = "1";
    public static final String IMAGE_A = "a";
    public static final String IMAGE_TABLE = "table";
    public static final String IMAGE_NEXT_RESULTS = "next_results";
    public static final String IMAGE_ADD = "add";
    public static final String IMAGE_REMOVE = "remove";
    public static final String IMAGE_DYNAMODB_SERVICE = "dynamodb-service";

    // The plug-in ID
    public static final String PLUGIN_ID = "com.amazonaws.eclipse.dynamodb"; //$NON-NLS-1$

    // The shared instance
    private static DynamoDBPlugin plugin;

    private IPropertyChangeListener listener;

    /**
     * The constructor
     */
    public DynamoDBPlugin() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        setDefaultDynamoDBLocalPort();

        listener = new DefaultTestToolPortListener();
        getPreferenceStore().addPropertyChangeListener(listener);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;

        if (listener != null) {
            getPreferenceStore().removePropertyChangeListener(listener);
            listener = null;
        }

        super.stop(context);
    }

    /**
     * Register a local DynamoDB service on the configured default port for
     * DynamoDB Local. This will cause the DynamoDB node to show up on the
     * Local region of the explorer, so you can connect to it if you've
     * started DynamoDB Local from outside of Eclipse.
     */
    public void setDefaultDynamoDBLocalPort() {
        int defaultPort = getPreferenceStore()
            .getInt(TestToolPreferencePage.DEFAULT_PORT_PREFERENCE_NAME);

        RegionUtils.addLocalService(ServiceAbbreviations.DYNAMODB,
                                    "dynamodb",
                                    defaultPort);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static DynamoDBPlugin getDefault() {
        return plugin;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#createImageRegistry()
     */
    @Override
    protected ImageRegistry createImageRegistry() {
        String[] images = new String[] {
                IMAGE_ONE,              "/icons/1.png",
                IMAGE_A,                "/icons/a.png",
                IMAGE_TABLE,            "/icons/table.png",
                IMAGE_NEXT_RESULTS,     "/icons/next_results.png",
                IMAGE_ADD,              "/icons/add.png",
                IMAGE_REMOVE,           "/icons/remove.gif",
                IMAGE_DYNAMODB_SERVICE, "/icons/dynamodb-service.png",
        };

        ImageRegistry imageRegistry = super.createImageRegistry();
        Iterator<String> i = Arrays.asList(images).iterator();
        while (i.hasNext()) {
            String id = i.next();
            String imagePath = i.next();
            imageRegistry.put(id, ImageDescriptor.createFromFile(getClass(),
                                                                 imagePath));
        }

        return imageRegistry;
    }

    /**
     * Property change listener that updates the endpoint registered with the
     * "Local" psuedo-region.
     */
    private static class DefaultTestToolPortListener
            implements IPropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if ((event.getProperty()
                        == TestToolPreferencePage.DEFAULT_PORT_PREFERENCE_NAME)
                    && !TestToolManager.INSTANCE.isRunning()) {

                RegionUtils.addLocalService(ServiceAbbreviations.DYNAMODB,
                                            "dynamodb",
                                            (Integer) event.getNewValue());
            }
        }
    }

}
