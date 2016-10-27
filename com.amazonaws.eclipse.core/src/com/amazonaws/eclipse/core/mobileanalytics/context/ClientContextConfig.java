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
package com.amazonaws.eclipse.core.mobileanalytics.context;

import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.MOBILE_ANALYTICS_APP_ID_PROD;
import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.MOBILE_ANALYTICS_APP_ID_TEST;
import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.MOBILE_ANALYTICS_APP_TITLE_PROD;
import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.MOBILE_ANALYTICS_APP_TITLE_TEST;
import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY;

import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

import com.amazonaws.annotation.Immutable;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.mobileanalytics.internal.Constants;
import com.amazonaws.util.StringUtils;

/**
 * Container for all the data required in the x-amz-client-context header.
 *
 * @see http://docs.aws.amazon.com/mobileanalytics/latest/ug/PutEvents.html#putEvents-request-client-context-header
 */
@Immutable
public class ClientContextConfig {

    private final String appTitle;
    private final String appId;
    private final String envPlatformName;
    private final String envPlatformVersion;
    private final String envLocale;
    // unique per installation; persisted in preference store
    private final String clientId;

    public static final ClientContextConfig PROD_CONFIG = new ClientContextConfig(
            MOBILE_ANALYTICS_APP_TITLE_PROD, MOBILE_ANALYTICS_APP_ID_PROD,
            _getSystemOsName(), _getSystemOsVersion(),
            _getSystemLocaleCountry(), _getOrGenerateClientId());

    public static final ClientContextConfig TEST_CONFIG = new ClientContextConfig(
            MOBILE_ANALYTICS_APP_TITLE_TEST, MOBILE_ANALYTICS_APP_ID_TEST,
            _getSystemOsName(), _getSystemOsVersion(),
            _getSystemLocaleCountry(), _getOrGenerateClientId());

    private ClientContextConfig(String appTitle, String appId,
            String envPlatformName, String envPlatformVersion, String envLocale, String clientId) {
        this.appTitle = appTitle;
        this.appId = appId;
        this.envPlatformName = envPlatformName;
        this.envPlatformVersion = envPlatformVersion;
        this.envLocale = envLocale;
        this.clientId = clientId;
    }

    public String getAppTitle() {
        return appTitle;
    }

    public String getAppId() {
        return appId;
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

    private static String _getOrGenerateClientId() {
        // This is an instance scope PreferenceStore which should be replaced with an installation scope store.
        IPreferenceStore store = AwsToolkitCore.getDefault().getPreferenceStore();
        String clientId = store.getString(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY);

        IEclipsePreferences eclipsePreferences = new ConfigurationScope().getNode(
                AwsToolkitCore.getDefault().getBundle().getSymbolicName());
        String installationScopeClientId = eclipsePreferences.get(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, null);

        if (!StringUtils.isNullOrEmpty(installationScopeClientId)) {
            if (!installationScopeClientId.equals(clientId)) {
                store.setValue(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, installationScopeClientId);
            }
            return installationScopeClientId;
        }

        if (StringUtils.isNullOrEmpty(clientId)) {
            // Generate a GUID as the client id and persist it in the preference store
            clientId = UUID.randomUUID().toString();
            // For backward compatibility, we still store the new client id to the instance scope preference store.
            store.setValue(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, clientId);
        }
        eclipsePreferences.put(MOBILE_ANALYTICS_CLIENT_ID_PREF_STORE_KEY, clientId);
        try {
            eclipsePreferences.flush();
        } catch (BackingStoreException e) {
            // Silently fails if exception occurs when flushing the client id.
        }
        return clientId;
    }

}
