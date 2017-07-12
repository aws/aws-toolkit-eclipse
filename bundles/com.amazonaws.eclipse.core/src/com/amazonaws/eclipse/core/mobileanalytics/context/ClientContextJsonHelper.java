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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.annotation.Immutable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.amazonaws.eclipse.core.mobileanalytics.internal.Constants.*;

/**
 * Responsible for converting {@link ClientContextConfig} into the expected JSON
 * format.
 *
 * @see http://docs.aws.amazon.com/mobileanalytics/latest/ug/PutEvents.html#putEvents-request-client-context-header
 */
@Immutable
public class ClientContextJsonHelper {

    private final Map<String, String> client = new HashMap<>();
    private final Map<String, String> env = new HashMap<>();
    private final Map<String, Map<String, String>> services = new HashMap<>();

    private static final ObjectMapper JACKSON_MAPPER = new ObjectMapper();

    public static String toJsonString(ClientContextConfig contextConfig)
            throws JsonProcessingException {
        ClientContextJsonHelper jsonHelper = new ClientContextJsonHelper(
                contextConfig);
        return JACKSON_MAPPER.writeValueAsString(jsonHelper);
    }

    private ClientContextJsonHelper(ClientContextConfig contextConfig) {

        // client map
        if (contextConfig.getClientId() != null) {
            client.put(CLIENT_CONTEXT_MAP_KEY_CLIENT_ID, contextConfig.getClientId());
        }
        if (contextConfig.getAppTitle() != null) {
            client.put(CLIENT_CONTEXT_MAP_KEY_APP_TITLE, contextConfig.getAppTitle());
        }

        // env map
        if (contextConfig.getEnvPlatformName() != null) {
            env.put(CLIENT_CONTEXT_MAP_KEY_PLATFORM_NAME, contextConfig.getEnvPlatformName());
        }
        if (contextConfig.getEnvPlatformVersion() != null) {
            env.put(CLIENT_CONTEXT_MAP_KEY_PLATFORM_VERSION, contextConfig.getEnvPlatformVersion());
        }
        if (contextConfig.getEnvLocale() != null) {
            env.put(CLIENT_CONTEXT_MAP_KEY_LOCALE, contextConfig.getEnvLocale());
        }

        // services map
        if (contextConfig.getAppId() != null) {
            services.put(CLIENT_CONTEXT_MAP_KEY_SERVICE_NAME, Collections
                    .singletonMap(CLIENT_CONTEXT_MAP_KEY_APP_ID,
                            contextConfig.getAppId()));
        }
    }

    public Map<String, String> getClient() {
        return client;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public Map<String, Map<String, String>> getServices() {
        return services;
    }

}
