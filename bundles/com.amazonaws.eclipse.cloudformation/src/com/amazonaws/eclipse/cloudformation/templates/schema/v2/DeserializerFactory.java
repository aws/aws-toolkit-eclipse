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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Customized Deserializers.
 */
public class DeserializerFactory {

    /**
     * {@link AllowedValue} POJO in the Json document could be either from a string literal
     * or a Json map.
     */
    public static class AllowedValuesDeserializer extends StdDeserializer<AllowedValue> {

        private static final long serialVersionUID = 1L;

        public AllowedValuesDeserializer() {
            this(null);
        }

        protected AllowedValuesDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public AllowedValue deserialize(JsonParser jp, DeserializationContext context)
                throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            if (node.isTextual() || node.isNumber()) {
                String text = node.asText();
                return new AllowedValue(text, text);
            } else if (node.isObject()) {
                String displayLabel = node.get(AllowedValue.P_DISPLAY_LABEL).asText();
                String value = node.get(AllowedValue.P_VALUE).asText();
                return new AllowedValue(displayLabel, value);
            }

            throw new RuntimeException("Unrecognized AllowedValue pattern");
        }
    }

}
