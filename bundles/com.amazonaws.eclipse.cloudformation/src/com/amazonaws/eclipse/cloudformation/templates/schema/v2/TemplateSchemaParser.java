/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.schema.v2;

import java.io.IOException;
import java.net.URL;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TemplateSchemaParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // The shared Template URL
    private static final String TEMPLATE_SCHEMA_LOCATION = "http://vstoolkit.amazonwebservices.com/CloudFormationSchema/CloudFormationV1.schema";

    private static TemplateSchema defaultTemplateSchema;

    static {
        MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        try {
            defaultTemplateSchema = MAPPER.readValue(new URL(TEMPLATE_SCHEMA_LOCATION), TemplateSchema.class);
        } catch (IOException e) {
            CloudFormationPlugin.getDefault().logError("Failed to load and parse the underlying CloudFormation schema file.", e);
        }
    }

    public static TemplateSchema getDefaultSchema() {
        return defaultTemplateSchema;
    }
}
