/*
* Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.eclipse.lambda.serverless.model.ServerlessTemplate;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ServerlessTemplateMapperTest {

    private static final String SERVERLESS_JSON_TEMPLATE_FILE = "serverless-template.json";
    private static final String SERVERLESS_YAML_TEMPLATE_FILE = "serverless-template.yaml";
    private static final String CODESTAR_YAML_TEMPLATE_FILE = "codestar.template.yml";

    private ServerlessModel jsonModel;
    private ServerlessTemplate jsonTemplate;

    private ServerlessModel yamlModel;
    private ServerlessTemplate yamlTemplate;

    private ServerlessModel codestarModel;
    private ServerlessTemplate codestarTemplate;

    @Before
    public void setUp() throws JsonParseException, JsonMappingException, IOException {
        try (InputStream serverlessTemplateInputStream = ServerlessTemplateMapperTest.class.getResourceAsStream(SERVERLESS_JSON_TEMPLATE_FILE)) {
            jsonModel = Serverless.load(serverlessTemplateInputStream);
            jsonTemplate = Serverless.convert(jsonModel);
        }

        try (InputStream serverlessTemplateInputStream = ServerlessTemplateMapperTest.class.getResourceAsStream(SERVERLESS_YAML_TEMPLATE_FILE)) {
            yamlModel = Serverless.load(serverlessTemplateInputStream);
            yamlTemplate = Serverless.convert(yamlModel);
        }

        try (InputStream serverlessTemplateInputStream = ServerlessTemplateMapperTest.class.getResourceAsStream(CODESTAR_YAML_TEMPLATE_FILE)) {
            codestarModel = Serverless.load(serverlessTemplateInputStream);
            codestarTemplate = Serverless.convert(codestarModel);
        }
    }

    @Test
    public void testCodeStarTransform() throws IOException {
        Assert.assertArrayEquals(new String[]{"AWS::Serverless-2016-10-31", "AWS::CodeStar"},
                codestarTemplate.getTransform().toArray());
    }

    @Test
    public void testModel_additionalProperties() {
        Consumer<ServerlessModel> testModel_additionalProperties = (model) -> {
            Map<String, Object> additionalProperties = model.getAdditionalProperties();
            testValuePath(additionalProperties, "bar", "foo");
            testValuePath(additionalProperties, "bar", "foo1", "foo");
        };

        testModel_additionalProperties.accept(jsonModel);
        testModel_additionalProperties.accept(yamlModel);
    }

    @Test
    public void testModel_ServerlessFunction() {
        Consumer<ServerlessModel> testModel_ServerlessFunction = (model) -> {
            Map<String, ServerlessFunction> functions = jsonModel.getServerlessFunctions();
            ServerlessFunction function = testServerlessFunction(functions, "ServerlessFunction",
                    "fakeCodeUri", "fakeHandler", "java8", 512, 300, Arrays.asList("Policy1", "Policy2"));

            Map<String, Object> additionalTopLevelProperties = function.getAdditionalTopLevelProperties();
            testValuePath(additionalTopLevelProperties, "bar", "foo");

            Map<String, Object> additionalProperties = function.getAdditionalProperties();
            assertTrue(additionalProperties.containsKey("Events"));
            assertS3EventMatches((Map<String, Object>)additionalProperties.get("Events"), "S3Event", "fakeBucket");
        };

        testModel_ServerlessFunction.accept(jsonModel);
        testModel_ServerlessFunction.accept(yamlModel);
    }

    @Test
    public void testModel_ServerlessFunction2() {
        Consumer<ServerlessModel> testModel_ServerlessFunction2 = (model) -> {
            Map<String, ServerlessFunction> functions = jsonModel.getServerlessFunctions();

            ServerlessFunction function = testServerlessFunction(functions, "ServerlessFunction2",
                    "fakeCodeUri", "fakeHandler", "fakeRuntime", 100, 100, Collections.<String>emptyList());

            Map<String, Object> additionalTopLevelProperties = function.getAdditionalTopLevelProperties();
            assertTrue(additionalTopLevelProperties.isEmpty());

            Map<String, Object> additionalProperties = function.getAdditionalProperties();

            testValuePath(additionalProperties, "value1", "Environment", "Variables", "key1");
            testValuePath(additionalProperties, "value2", "Environment", "Variables", "key2");
        };

        testModel_ServerlessFunction2.accept(jsonModel);
        testModel_ServerlessFunction2.accept(yamlModel);
    }

    @Test
    public void testModel_additionalResources() {
        Consumer<ServerlessModel> testModel_additionalResources = (model) -> {
            Map<String, TypeProperties> resources = jsonModel.getAdditionalResources();

            assertTrue(resources.containsKey("IamRole"));
            TypeProperties tp = resources.get("IamRole");
            assertEquals("AWS::IAM::Role", tp.getType());

            Map<String, Object> properties = tp.getProperties();
            assertEquals("fakeValue", properties.get("fakeKey"));

            assertTrue(tp.getAdditionalProperties().containsKey("foo"));
            assertEquals("bar", tp.getAdditionalProperties().get("foo"));
        };

        testModel_additionalResources.accept(jsonModel);
        testModel_additionalResources.accept(yamlModel);
    }

    @Test
    public void testTemplate_Metadata() {
        Consumer<ServerlessModel> testTemplate_Metadata = (model) -> {
            assertEquals("2010-09-09", jsonTemplate.getAWSTemplateFormatVersion());
            assertEquals(null, jsonTemplate.getDescription());
            assertArrayEquals(new String[]{"AWS::Serverless-2016-10-31"}, jsonTemplate.getTransform().toArray());
        };

        testTemplate_Metadata.accept(jsonModel);
        testTemplate_Metadata.accept(yamlModel);
    }

    @Test
    public void testTemplate_AdditionalProperties() {
        Consumer<ServerlessTemplate> testTemplate_AdditionalProperties = (template) -> {
            Map<String, Object> additionalProperties = jsonTemplate.getAdditionalProperties();
            testValuePath(additionalProperties, "bar", "foo");
            testValuePath(additionalProperties, "bar", "foo1", "foo");
        };

        testTemplate_AdditionalProperties.accept(jsonTemplate);
        testTemplate_AdditionalProperties.accept(yamlTemplate);
    }

    @Test
    public void testTemplate_ServerlessFunction() {
        Consumer<ServerlessTemplate> testTemplate_ServerlessFunction = (template) -> {
            Map<String, TypeProperties> resources = jsonTemplate.getResources();
            TypeProperties resource = testTemplateResource(resources, "ServerlessFunction", "AWS::Serverless::Function");

            Map<String, Object> additionalProperties = resource.getAdditionalProperties();
            testValuePath(additionalProperties, "bar", "foo");

            Map<String, Object> properties = resource.getProperties();
            testValuePath(properties, "fakeCodeUri", "CodeUri");
            testValuePath(properties, "fakeHandler", "Handler");
            testValuePath(properties, "S3", "Events", "S3Event", "Type");
            testValuePath(properties, "fakeBucket", "Events", "S3Event", "Properties", "Bucket");
        };

        testTemplate_ServerlessFunction.accept(jsonTemplate);
        testTemplate_ServerlessFunction.accept(yamlTemplate);
    }

    @Test
    public void testTemplate_ServerlessFunction2() {
        Consumer<ServerlessTemplate> testTemplate_ServerlessFunction2 = (template) -> {
            Map<String, TypeProperties> resources = jsonTemplate.getResources();
            TypeProperties resource = testTemplateResource(resources, "ServerlessFunction2", "AWS::Serverless::Function");

            Map<String, Object> additionalProperties = resource.getAdditionalProperties();
            assertTrue(additionalProperties.isEmpty());

            Map<String, Object> properties = resource.getProperties();
            testValuePath(properties, "fakeCodeUri", "CodeUri");
            testValuePath(properties, "fakeHandler", "Handler");
            testValuePath(properties, "fakeRuntime", "Runtime");
            testValuePath(properties, "fakeFunctionName", "FunctionName");
            testValuePath(properties, new Integer(100), "MemorySize");
            testValuePath(properties, new Integer(100), "Timeout");
            testValuePath(properties, "value1", "Environment", "Variables", "key1");
            testValuePath(properties, "value2", "Environment", "Variables", "key2");
        };

        testTemplate_ServerlessFunction2.accept(jsonTemplate);
        testTemplate_ServerlessFunction2.accept(yamlTemplate);
    }

    @Test
    public void testTemplate_IamRole() {
        Consumer<ServerlessTemplate> testTemplate_IamRole = (template) -> {
            Map<String, TypeProperties> resources = jsonTemplate.getResources();
            TypeProperties resource = testTemplateResource(resources, "ServerlessFunction", "AWS::Serverless::Function");

            Map<String, Object> additionalProperties = resource.getAdditionalProperties();
            testValuePath(additionalProperties, "bar", "foo");

            Map<String, Object> properties = resource.getProperties();
            testValuePath(properties, "fakeCodeUri", "CodeUri");
            testValuePath(properties, "fakeHandler", "Handler");
            testValuePath(properties, "S3", "Events", "S3Event", "Type");
            testValuePath(properties, "fakeBucket", "Events", "S3Event", "Properties", "Bucket");
        };

        testTemplate_IamRole.accept(jsonTemplate);
        testTemplate_IamRole.accept(yamlTemplate);
    }

    private TypeProperties testTemplateResource(Map<String, TypeProperties> resources, String resourceName, String resourceType) {
        assertTrue(resources.containsKey(resourceName));

        TypeProperties resource = resources.get(resourceName);
        assertEquals(resourceType, resource.getType());
        return resource;
    }

    private void assertS3EventMatches(Map<String, Object> events, String s3EventName, String bucketName) {
        testValuePath(events, "S3", s3EventName, "Type");
        testValuePath(events, bucketName, s3EventName, "Properties", "Bucket");
    }

    private void testValuePath(Map<String, Object> map, Object value, String... keyPath) {
        assertTrue(keyPath != null && keyPath.length != 0);
        Object currentValue = map;
        for (String key : keyPath) {
            Map<String, Object> currentMap = (Map<String, Object>) currentValue;
            assertTrue(currentMap.containsKey(key));
            currentValue = currentMap.get(key);
        }
        assertEquals(value, currentValue);
    }

    private ServerlessFunction testServerlessFunction(Map<String, ServerlessFunction> functions, String resourceName,
            String codeUri, String handler, String runtime, int memorySize, int timeout, List<String> policies) {
        assertTrue(functions.containsKey(resourceName));

        ServerlessFunction function = functions.get(resourceName);
        assertEquals(codeUri, function.getCodeUri());
        assertEquals(handler, function.getHandler());
        assertEquals(runtime, function.getRuntime());
        assertEquals(memorySize, function.getMemorySize().intValue());
        assertEquals(timeout, function.getTimeout().intValue());

        List<String> actualPolicies = function.getPolicies();
        assertTrue(actualPolicies.equals(policies));

        return function;
    }
}
