/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Abstract plugin that to be extended by AWS plugins. It defines common logics shared
 * by all the AWS plugins such as logging and exception reporting.
 */
public abstract class AbstractAwsPlugin extends AbstractUIPlugin {

    /**
     * Returns the default plugin id defined in the manifest file.
     */
    public String getPluginId() {
        return getBundle().getSymbolicName();
    }

    /**
     * Convenience method for logging a debug message at INFO level.
     */
    public IStatus logInfo(String infoMessage) {
        IStatus status = new Status(IStatus.INFO, getPluginId(), infoMessage, null);
        getLog().log(status);
        return status;
    }

    /**
     * Convenience method for logging a warning message.
     */
    public IStatus logWarning(String warningMessage, Throwable e) {
        IStatus status = new Status(IStatus.WARNING, getPluginId(), warningMessage, e);
        getLog().log(status);
        return status;
    }

    /**
     * Convenience method for logging an error message.
     */
    public IStatus logError(String errorMessage, Throwable e) {
        IStatus status = new Status(IStatus.ERROR, getPluginId(), errorMessage, e);
        getLog().log(status);
        return status;
    }

    /**
     * Convenience method for reporting error to StatusManager.
     */
    public IStatus reportException(String errorMessage, Throwable e) {
        IStatus status = new Status(IStatus.ERROR, getPluginId(), errorMessage, e);
        StatusManager.getManager().handle(
                status, StatusManager.SHOW | StatusManager.LOG);
        return status;
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry imageRegistry = super.createImageRegistry();
        for (Entry<String, String> entry : getImageRegistryMap().entrySet()) {
            imageRegistry.put(entry.getKey(), ImageDescriptor.createFromFile(getClass(), entry.getValue()));
        }
        return imageRegistry;
    }

    // Subclass plugin should override this method if their image registry is not empty.
    protected Map<String, String> getImageRegistryMap() {
        return Collections.emptyMap();
    }

    protected String getBundleVersion() {
        return getBundle().getVersion().toString();
    }
}
