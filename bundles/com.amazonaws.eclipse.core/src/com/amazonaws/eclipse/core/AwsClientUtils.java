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

package com.amazonaws.eclipse.core;

import java.util.Dictionary;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;

/**
 * Utilities for working with AWS clients, such as creating consistent user
 * agent strings for different plugins.
 */
public final class AwsClientUtils {

    /**
     * Forms a user-agent string for clients to send when making service calls,
     * indicating the name and version of this client.
     *
     * @param pluginName
     *            The name of the plugin to use in the user agent string.
     * @param plugin
     *            The plugin from which to pull version information.
     *
     * @return A user-agent string indicating what client and version are
     *         accessing AWS.
     */
    public static String formatUserAgentString(
            String pluginName,
            Plugin plugin) {

        /*
         * If we aren't running in an OSGi container (ex: during tests), then we
         * won't have access to pull out the version, but if we are, we can pull
         * it out of the bundle-version property.
         */
        String version = "???";
        if ( plugin != null ) {
            Dictionary headers = plugin.getBundle().getHeaders();
            version = (String) headers.get("Bundle-Version");
        }

        String userAgentValue = pluginName + "/" + version;

        Bundle runtimeCore = Platform.getBundle("org.eclipse.core.runtime");
        if ( runtimeCore != null ) {
            Dictionary headers = runtimeCore.getHeaders();
            version = (String) headers.get("Bundle-Version");
            userAgentValue += ", Eclipse/" + version;
        }

        return userAgentValue;
    }

    @Deprecated
    public String formUserAgentString(String pluginName, Plugin plugin) {
        return formatUserAgentString(pluginName, plugin);
    }

    @Deprecated
    public AwsClientUtils() {
    }
}
