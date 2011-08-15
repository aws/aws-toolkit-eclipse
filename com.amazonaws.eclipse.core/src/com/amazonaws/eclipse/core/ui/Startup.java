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
package com.amazonaws.eclipse.core.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Runs at startup to determine if any new AWS Toolkit components have been
 * installed, and if so, displays the AWS Toolkit Overview view.
 */
public class Startup implements IStartup {

    private static IEditorInput input = null;

    /* (non-Javadoc)
     * @see org.eclipse.ui.IStartup#earlyStartup()
     */
    public void earlyStartup() {
        recordOverviewContributors();
    }


    /*
     * Protected Interface
     */

    /**
     * Returns true if the AWS Toolkit Overview view should be displayed.
     *
     * @return True if the AWS Toolkit Overview view should be displayed.
     */
    protected boolean shouldDisplayOverview() {
        Map<String, String> overviewContributors = findOverviewContributors();
        Map<String, String> registeredOverviewContributors = getRegisteredOverviewContributors();

        // If we found more overview contributors than we have registered,
        // we know something must be new so we can return early.
        if (overviewContributors.size() > registeredOverviewContributors.size()) {
            return true;
        }

        for (String key : overviewContributors.keySet()) {
            /*
             * If we identified a contributing plugin that hasn't been
             * registered yet, we want to display the overview view.
             */
            if (!registeredOverviewContributors.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Opens the AWS Toolkit Overview editor.
     */
    protected static void displayAwsToolkitOverviewEditor() {
        if ( input == null) {
            input = new IEditorInput() {
            public Object getAdapter(Class adapter) {
                return null;
            }

            public String getToolTipText() {
                return "AWS Toolkit for Eclipse Overview";
            }

            public IPersistableElement getPersistable() {
                return null;
            }

            public String getName() {
                return "AWS Toolkit for Eclipse Overview";
            }

            public ImageDescriptor getImageDescriptor() {
                return null;
            }

            public boolean exists() {
                return true;
            }
        };
        }

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, AwsToolkitCore.OVERVIEW_EDITOR_ID);
                } catch (PartInitException e) {
                    String errorMessage = "Unable to open the AWS Toolkit Overview view: " + e.getMessage();
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        });
    }


    /*
     * Private Interface
     */

    /**
     * Returns the file which records which components of the AWS Toolkit for
     * Eclipse are installed.
     *
     * @return The file which records which components of the AWS Toolkit for
     *         Eclipse are installed.
     */
    private File getPropertiesFile() {
        return AwsToolkitCore.getDefault().getBundle().getBundleContext().getDataFile("awsToolkitInstalledPlugins.properties");
    }

    /**
     * Returns a map of plugin versions, keyed by their plugin ID, representing
     * plugins that have previously been detected and recorded as contributing
     * to the AWS Toolkit Overview view.
     *
     * @return A map of plugin versions, keyed by their plugin ID, representing
     *         plugins that have been recorded as contributing to the AWS
     *         Toolkit Overview view.
     */
    private Map<String, String> getRegisteredOverviewContributors() {
        Map<String, String> registeredPlugins = new HashMap<String, String>();

        File dataFile = getPropertiesFile();
        if (dataFile == null || dataFile.exists() == false) {
            return registeredPlugins;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(dataFile);
            Properties properties = new Properties();
            properties.load(inputStream);

            for (Object key : properties.keySet()) {
                String value = properties.getProperty(key.toString());

                registeredPlugins.put(key.toString(), value);
            }
        } catch (IOException e) {
            String errorMessage = "Unable to read currently registered AWS Toolkit components: " + e.getMessage();
            Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        } finally {
            try {inputStream.close();} catch (IOException ioe) {}
        }

        return registeredPlugins;
    }

    /**
     * Returns a map of plugin versions, keyed by their plugin ID, representing
     * installed plugins which contribute to the AWS Toolkit Overview view
     * through the core plugin's extension point.
     *
     * @return A map of plugin versions, keyed by their plugin ID, representing
     *         installed plugins which contribute to the AWS Toolkit Overview
     *         view.
     */
    private Map<String, String> findOverviewContributors() {
        Map<String, String> plugins = new HashMap<String, String>();

        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(AwsToolkitCore.OVERVIEW_EXTENSION_ID);
        IExtension[] extensions = extensionPoint.getExtensions();
        for (IExtension extension : extensions) {
            String pluginName = extension.getContributor().getName();

            Dictionary headers = Platform.getBundle(pluginName).getHeaders();
            String pluginVersion = (String)headers.get("Bundle-Version");
            if (pluginVersion == null) pluginVersion = "";

            plugins.put(pluginName, pluginVersion);
        }

        return plugins;
    }

    /**
     * Records a list of the detected plugins (and their versions) which
     * contribute to the AWS Toolkit Overview view through the core plugin's
     * overview extension point.
     */
    private void recordOverviewContributors() {
        File dataFile = getPropertiesFile();
        Properties properties = new Properties();

        Map<String, String> contributions = findOverviewContributors();
        for (String pluginName : contributions.keySet()) {
            String pluginVersion = contributions.get(pluginName);
            properties.put(pluginName, pluginVersion);
        }

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(dataFile);
            properties.store(outputStream, null);
        } catch (IOException e) {
            String errorMessage = "Unable to record registered components: " + e.getMessage();
            Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        } finally {
            try {outputStream.close();} catch (IOException ioe) {}
        }
    }

}
