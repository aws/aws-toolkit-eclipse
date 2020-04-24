/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.telemetry;

import static com.amazonaws.eclipse.core.telemetry.internal.Constants.JAVA_PREFERENCE_NODE_FOR_AWS_TOOLKIT_FOR_ECLIPSE;
import static com.amazonaws.eclipse.core.telemetry.internal.Constants.MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY;

import java.util.Locale;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import com.amazonaws.annotation.Immutable;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.util.StringUtils;

/**
 * Container for all the data required in the x-amz-client-context header.
 *
 * @see http://docs.aws.amazon.com/mobileanalytics/latest/ug/PutEvents.html#putEvents-request-client-context-header
 */
@Immutable
public class ClientContextConfig {
    private final String envPlatformName;
    private final String envPlatformVersion;
    private final String eclipseVersion;
    private final String envLocale;
    private final String clientId;
    private final String version;

    public static final ClientContextConfig PROD_CONFIG = new ClientContextConfig(
            _getSystemOsName(), _getSystemOsVersion(),
            _getSystemLocaleCountry(), getOrGenerateClientId());

    public static final ClientContextConfig TEST_CONFIG = new ClientContextConfig(
            _getSystemOsName(), _getSystemOsVersion(),
            _getSystemLocaleCountry(), getOrGenerateClientId());

    private ClientContextConfig(String envPlatformName, String envPlatformVersion, String envLocale, String clientId) {
        this.eclipseVersion = eclipseVersion();
        this.version = getPluginVersion();
        this.envPlatformName = envPlatformName;
        this.envPlatformVersion = envPlatformVersion;
        this.envLocale = envLocale;
        this.clientId = clientId;
    }

    private String eclipseVersion() {
        try {
            Bundle bundle = Platform.getBundle("org.eclipse.platform");
            return bundle.getVersion().toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String getPluginVersion() {
        try {
            Bundle bundle = Platform.getBundle("com.amazonaws.eclipse.core");
            return bundle.getVersion().toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getEnvPlatformName() {
        return envPlatformName;
    }

    public String getEnvPlatformVersion() {
        return envPlatformVersion;
    }

    public String getEnvLocale() {
        return envLocale;
    }

    public String getClientId() {
        return clientId;
    }
    
	public String getVersion() {
		return version;
	}
	
	public String getEclipseVersion() {
		return eclipseVersion;
	}

    private static String _getSystemOsName() {
        try {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                return null;
            }

            osName = osName.toLowerCase();

            if (osName.startsWith("windows")) {
                return Constants.CLIENT_CONTEXT_ENV_PLATFORM_WINDOWS;
            }
            if (osName.startsWith("mac")) {
                return Constants.CLIENT_CONTEXT_ENV_PLATFORM_MACOS;
            }
            if (osName.startsWith("linux")) {
                return Constants.CLIENT_CONTEXT_ENV_PLATFORM_LINUX;
            }

            AwsToolkitCore.getDefault().logInfo("Unknown OS name: " + osName);
            return null;

        } catch (Exception e) {
            return null;
        }
    }

    private static String _getSystemOsVersion() {
        try {
            return System.getProperty("os.version");
        } catch (Exception e) {
            return null;
        }
    }

    private static String _getSystemLocaleCountry() {
        try {
            return Locale.getDefault().getDisplayCountry(Locale.US);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getOrGenerateClientId() {
        // This is the Java preferences scope
        Preferences awsToolkitNode = Preferences.userRoot().node(JAVA_PREFERENCE_NODE_FOR_AWS_TOOLKIT_FOR_ECLIPSE);
        String clientId = awsToolkitNode.get(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, null);

        if (!StringUtils.isNullOrEmpty(clientId)) {
            return clientId;
        }

        // This is an installation scope PreferenceStore.
        IEclipsePreferences eclipsePreferences = ConfigurationScope.INSTANCE.getNode(
                AwsToolkitCore.getDefault().getBundle().getSymbolicName());
        clientId = eclipsePreferences.get(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, null);

        if (StringUtils.isNullOrEmpty(clientId)) {
            // This is an instance scope PreferenceStore.
            IPreferenceStore store = AwsToolkitCore.getDefault().getPreferenceStore();
            clientId = store.getString(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY);
        }

        if (StringUtils.isNullOrEmpty(clientId)) {
            // Generate a GUID as the client id and persist it in the preference store
            clientId = UUID.randomUUID().toString();
        }

        awsToolkitNode.put(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, clientId);

        try {
            awsToolkitNode.flush();
        } catch (BackingStoreException e) {
            // Silently fails if exception occurs when flushing the client id.
        }
        return clientId;
    }
}
