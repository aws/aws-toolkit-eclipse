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

import static com.amazonaws.eclipse.lambda.serverless.model.ResourceType.AWS_SERVERLESS_FUNCTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
        return load(new File(templatePath));
    }

    public static ServerlessModel load(File templateFile) throws JsonParseException, JsonMappingException, IOException {
        return load(new FileInputStream(templateFile));
    }

    public static ServerlessModel load(InputStream templateInput) throws JsonParseException, JsonMappingException, IOException {
        ServerlessTemplate serverlessTemplate = MAPPER.readValue(templateInput, ServerlessTemplate.class);
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

    public static ServerlessTemplate convert(ServerlessModel model) {
        ServerlessTemplate template = new ServerlessTemplate();
        template.setAWSTemplateFormatVersion(model.getAWSTemplateFormatVersion());
        template.setDescription(model.getDescription());
        template.setTransform(model.getTransform());
        for (Entry<String, Object> entry : model.getAdditionalProperties().entrySet()) {
            template.addAdditionalProperty(entry.getKey(), entry.getValue());
        }
        for (Entry<String, TypeProperties> entry : model.getAdditionalResources().entrySet()) {
            template.addResource(entry.getKey(), entry.getValue());
        }

        // Put Serverless resources to the map.
        for (Entry<String, ServerlessFunction> entry : model.getServerlessFunctions().entrySet()) {
            String resourceLogicalId = entry.getKey();
            ServerlessFunction serverlessFunction = entry.getValue();
            template.addResource(resourceLogicalId, serverlessFunction.toTypeProperties());
        }

        return template;
    }

    private static ServerlessModel convert(ServerlessTemplate template) {
        ServerlessModel model = new ServerlessModel();
        model.setAWSTemplateFormatVersion(template.getAWSTemplateFormatVersion());
        model.setDescription(template.getDescription());
        model.setTransform(template.getTransform());
        for (Entry<String, Object> entry : template.getAdditionalProperties().entrySet()) {
            model.addAdditionalProperty(entry.getKey(), entry.getValue());
        }

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

        String type = resource.getType();
        if (AWS_SERVERLESS_FUNCTION.getType().equals(type)) {
            ServerlessFunction function = convertServerlessFunction(resource);
            model.addServerlessFunction(resourceLogicalId, function);
        } else {
            // Unrecognized resources put to additionalResources
            model.addAdditionalResource(resourceLogicalId, resource);
        }
    }

    private static ServerlessFunction convertServerlessFunction(TypeProperties tp) {
        Map<String, Object> resource = tp.getProperties();
        ServerlessFunction function = MAPPER.convertValue(resource, ServerlessFunction.class);
        for (Entry<String, Object> entry : tp.getAdditionalProperties().entrySet()) {
            function.addAdditionalTopLevelProperty(entry.getKey(), entry.getValue());
        }
        return function;
    }

    private static Map<String, TypeProperties> convert(Map<String, Object> map) {
        Map<String, TypeProperties> typeProperties = new HashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            typeProperties.put(entry.getKey(), MAPPER.convertValue(entry.getValue(), TypeProperties.class));
        }
        return typeProperties;
    }

}
