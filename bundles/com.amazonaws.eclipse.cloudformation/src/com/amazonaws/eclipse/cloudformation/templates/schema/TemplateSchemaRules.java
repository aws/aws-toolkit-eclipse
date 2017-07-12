/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.schema;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TemplateSchemaRules {

    // JSON Keys
    private static final String ALLOWED_VALUES = "allowed-values";
    private static final String CHILD_SCHEMAS = "child-schemas";
    private static final String DEFAULT_CHILD_SCHEMA = "default-child-schema";
    private static final String DESCRIPTION = "description";
    private static final String INTRINSIC_FUNCTIONS = "intrinsic-functions";
    private static final String PARAMETER = "parameter";
    private static final String PROPERTIES = "properties";
    private static final String PSEUDO_PARAMETERS = "pseudo-parameters";
    private static final String REQUIRED = "required";
    private static final String RESOURCES = "Resources";
    private static final String ROOT_SCHEMA_OBJECT = "root-schema-object";
    private static final String SCHEMA_LOOKUP_PROPERTY = "schema-lookup-property";
    private static final String TYPE = "type";

    // Template URL
    private static final String TEMPLATE_SCHEMA_LOCATION = "http://vstoolkit.amazonwebservices.com/CloudFormationSchema/CloudFormationV1.schema";

    private JsonNode rootNode;
    private static TemplateSchemaRules instance;

    private void parse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        rootNode = mapper.readValue(new URL(TEMPLATE_SCHEMA_LOCATION), JsonNode.class);
    }

    public Set<String> getResourceTypeNames() {
        return getTopLevelSchema().getProperty(RESOURCES).getChildSchemas().keySet();
    }

    public Schema getTopLevelSchema() {
        Schema schema = parseSchema(this.rootNode.get(ROOT_SCHEMA_OBJECT));

        return schema;
    }

    public List<PseudoParameter> getPseudoParameters() {
        // TODO: Caching

        ArrayList<PseudoParameter> pseudoParameters = new ArrayList<>();

        Iterator<Entry<String, JsonNode>> iterator = rootNode.get(PSEUDO_PARAMETERS).fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();

            pseudoParameters.add(new PseudoParameter(entry.getKey(),
                entry.getValue().get(TYPE).asText(),
                entry.getValue().get(DESCRIPTION).asText()));
        }

        return pseudoParameters;
    }

    public List<IntrinsicFunction> getIntrinsicFuntions() {
        // TODO: Caching

        ArrayList<IntrinsicFunction> intrinsicFunctions = new ArrayList<>();

        Iterator<Entry<String, JsonNode>> iterator = rootNode.get(INTRINSIC_FUNCTIONS).fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> entry = iterator.next();

            intrinsicFunctions.add(new IntrinsicFunction(entry.getKey(),
                entry.getValue().get(PARAMETER).asText(),
                entry.getValue().get(DESCRIPTION).asText()));
        }

        return intrinsicFunctions;
    }


    private Schema parseSchema(JsonNode schemaNode) {
        Schema schema = new Schema();

        if (schemaNode.has(DESCRIPTION)) {
            schema.setDescription(schemaNode.get(DESCRIPTION).textValue());
        }

        if (schemaNode.has(PROPERTIES)) {
            Iterator<Entry<String, JsonNode>> fields = schemaNode.get(PROPERTIES).fields();
            while (fields.hasNext()) {
                Entry<String, JsonNode> entry = fields.next();

                SchemaProperty schemaProperty = new SchemaProperty(entry.getValue().get(TYPE).asText());

                if (entry.getValue().has(DESCRIPTION)) {
                    schemaProperty.setDescription(entry.getValue().get(DESCRIPTION).asText());
                }

                if (entry.getValue().has(REQUIRED)) {
                    schemaProperty.setRequired(entry.getValue().get(REQUIRED).asBoolean());
                }

                if (entry.getValue().has(ALLOWED_VALUES)) {
                    List<String> allowedValues = new ArrayList<>();
                    Iterator<JsonNode> iterator = entry.getValue().get(ALLOWED_VALUES).elements();
                    while (iterator.hasNext()) allowedValues.add(iterator.next().asText());
                    schemaProperty.setAllowedValues(allowedValues);
                }

                schema.addProperty(entry.getKey(), schemaProperty);

                JsonNode node = entry.getValue();
                if (node.has(DEFAULT_CHILD_SCHEMA)) {
                    Schema defaultSchema = parseSchema(node.get(DEFAULT_CHILD_SCHEMA));
                    schemaProperty.setDefaultChildSchema(defaultSchema);
                } else if (node.has(PROPERTIES)) {
                    Schema defaultSchema = parseSchema(node);
                    schemaProperty.setSchema(defaultSchema);
                }

                if (node.has(SCHEMA_LOOKUP_PROPERTY)) {
                    schemaProperty.setSchemaLookupProperty(node.get(SCHEMA_LOOKUP_PROPERTY).asText());
                }

                if (node.has(CHILD_SCHEMAS)) {
                    Iterator<Entry<String, JsonNode>> fields2 = node.get(CHILD_SCHEMAS).fields();
                    while (fields2.hasNext()) {
                        Entry<String, JsonNode> entry2 = fields2.next();

                        String schemaName = entry2.getKey();
                        Schema schema2 = parseSchema(entry2.getValue());

                        schemaProperty.addChildSchema(schemaName, schema2);
                    }
                }
            }
        } else {
            // JSON freeform text?
        }

        return schema;
    }

    public static TemplateSchemaRules getInstance() {
        if (instance == null) {
            instance = new TemplateSchemaRules();
            try {
                instance.parse();
            } catch (Exception e) {
                throw new RuntimeException("", e);
            }
        }

        return instance;
    }
}
