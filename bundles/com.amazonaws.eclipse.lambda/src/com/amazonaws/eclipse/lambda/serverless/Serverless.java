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
package com.amazonaws.eclipse.lambda.serverless;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.eclipse.lambda.serverless.model.EventSourceType;
import com.amazonaws.eclipse.lambda.serverless.model.ResourceType;
import com.amazonaws.eclipse.lambda.serverless.model.ServerlessTemplate;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A class for processing serverless template file, including loading Serverless template file into a model,
 * writing a model back to a Serverless template file, etc.
 */
public class Serverless {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static ServerlessModel load(String templatePath) throws JsonParseException, JsonMappingException, IOException {
        File serverlessFile = new File(templatePath);
        return load(serverlessFile);
    }

    public static ServerlessModel load(File templateFile) throws JsonParseException, JsonMappingException, IOException {
        ServerlessTemplate serverlessTemplate = MAPPER.readValue(templateFile, ServerlessTemplate.class);
        return convert(serverlessTemplate);
    }

    public static File write(ServerlessModel model, String path) throws JsonGenerationException, JsonMappingException, IOException {
        File file = new File(path);
        ServerlessTemplate template = convert(model);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(file), template);
        return file;
    }

    /**
     * Make this raw Serverless model to one that could be recognized by CloudFormation.
     * 
     * @param model - The raw Serverless model.
     * @param packagePrefix - The package prefix holding all the Lambda Functions.
     * @param filePath - The zipped jar file path. It should be a s3 path since CloudFormation only support this.
     * @return A CloudFormation recognized Serverless Model.
     */
    public static ServerlessModel cookServerlessModel(ServerlessModel model, String packagePrefix, String filePath) {
        for (Entry<String, ServerlessFunction> entry : model.getServerlessFunctions().entrySet()) {
            ServerlessFunction function = entry.getValue();
            function.setHandler(NameUtils.toHandlerClassFqcn(packagePrefix, function.getHandler()));
            function.setCodeUri(filePath);
        }
        return model;
    }

    private static ServerlessTemplate convert(ServerlessModel model) {
        ServerlessTemplate template = new ServerlessTemplate();
        template.setAWSTemplateFormatVersion(model.getAWSTemplateFormatVersion());
        template.setDescription(model.getDescription());
        template.setTransform(model.getTransform());
        template.setAdditionalProperties(model.getAdditionalProperties());
        Map<String, TypeProperties> resources = new HashMap<String, TypeProperties>();
        resources.putAll(model.getAdditionalResources());

        // Put Serverless resources to the map.
        for (Entry<String, ServerlessFunction> entry : model.getServerlessFunctions().entrySet()) {
            String resourceLogicalId = entry.getKey();
            ServerlessFunction serverlessFunction = entry.getValue();
            resources.put(resourceLogicalId, serverlessFunction.toTypeProperties());
        }
        
        template.setResources(resources);
        return template;
    }

    private static ServerlessModel convert(ServerlessTemplate template) {
        ServerlessModel model = new ServerlessModel();
        model.setAWSTemplateFormatVersion(template.getAWSTemplateFormatVersion());
        model.setDescription(template.getDescription());
        model.setTransform(template.getTransform());
        model.setAdditionalProperties(template.getAdditionalProperties());

        if (template.getResources() == null) return model;

        for (Entry<String, TypeProperties> entry : template.getResources().entrySet()) {
            String key = entry.getKey();
            TypeProperties resource = entry.getValue();
            convertResource(model, key, resource);
        }

        return model;
    }

    /**
     * Convert a general representation of a resource to a modeled resource and put it to ServerlessModel.
     * 
     * @param model The Serverless model in which the converted resource will be put.
     * @param resourceLogicalId The logical id for the resource.
     * @param resource The general representation for the resource.
     */
    private static void convertResource(ServerlessModel model, String resourceLogicalId, TypeProperties resource) {
        ResourceType type = ResourceType.fromValue(resource.getType());

        switch (type) {
        case AWS_SERVERLESS_FUNCTION:
            ServerlessFunction function = convertServerlessFunction(resource.getProperties());
            model.getServerlessFunctions().put(resourceLogicalId, function);
            break;
        // Unrecognized resources put to additionalResources
        default:
            model.getAdditionalResources().put(resourceLogicalId, resource);
            break;
        }
    }

    private static ServerlessFunction convertServerlessFunction(Map<String, Object> resource) {
        ServerlessFunction function = MAPPER.convertValue(resource, ServerlessFunction.class);
        Object eventsObject = resource.get("Events");
        if (eventsObject != null) {
            Map<String, TypeProperties> events = convert((Map<String, Object>) eventsObject);
            Map<String, TypeProperties> additionalEvents = new HashMap<String, TypeProperties>();
            for (Entry<String, TypeProperties> entry : events.entrySet()) {
                String eventLogicalId = entry.getKey();
                TypeProperties event = entry.getValue();
                EventSourceType eventSourceType = EventSourceType
                        .fromValue(event.getType());
                switch (eventSourceType) {
                // TODO: Treating all the events as additional event for
                // now. Need to add Api Event handler to better handle this
                // event.
                default:
                    additionalEvents.put(eventLogicalId, event);
                    break;
                }
            }
            function.setAdditionalEvents(additionalEvents);
        }
        return function;
    }

    private static Map<String, TypeProperties> convert(Map<String, Object> map) {
        Map<String, TypeProperties> typeProperties = new HashMap<String, TypeProperties>();
        for (Entry<String, Object> entry : map.entrySet()) {
            typeProperties.put(entry.getKey(), MAPPER.convertValue(entry.getValue(), TypeProperties.class));
        }
        return typeProperties;
    }

}
