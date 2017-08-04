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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.eclipse.lambda.serverless.model.ServerlessTemplate;
import com.amazonaws.eclipse.lambda.serverless.model.TypeProperties;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ServerlessTemplateMapperTest {

    private static final String SERVERLESS_TEMPLATE_FILE = "serverless-template.json";
    private ServerlessModel model;
    private ServerlessTemplate template;

    @Before
    public void setUp() throws JsonParseException, JsonMappingException, IOException {
        InputStream serverlessTemplateInputStream = ServerlessTemplateMapperTest.class.getResourceAsStream(SERVERLESS_TEMPLATE_FILE);
        model = Serverless.load(serverlessTemplateInputStream);
        template = Serverless.convert(model);
    }

    @Test
    public void testModel_additionalProperties() {
        Map<String, Object> additionalProperties = model.getAdditionalProperties();
        testValuePath(additionalProperties, "bar", "foo");
        testValuePath(additionalProperties, "bar", "foo1", "foo");
    }

    @Test
    public void testModel_ServerlessFunction() {
        Map<String, ServerlessFunction> functions = model.getServerlessFunctions();

        ServerlessFunction function = testServerlessFunction(functions, "ServerlessFunction",
                "fakeCodeUri", "fakeHandler", "java8", 512, 300, Arrays.asList("Policy1", "Policy2"));

        Map<String, Object> additionalTopLevelProperties = function.getAdditionalTopLevelProperties();
        testValuePath(additionalTopLevelProperties, "bar", "foo");

        Map<String, Object> additionalProperties = function.getAdditionalProperties();
        assertTrue(additionalProperties.containsKey("Events"));
        assertS3EventMatches((Map<String, Object>)additionalProperties.get("Events"), "S3Event", "fakeBucket");
    }

    @Test
    public void testModel_ServerlessFunction2() {
        Map<String, ServerlessFunction> functions = model.getServerlessFunctions();

        ServerlessFunction function = testServerlessFunction(functions, "ServerlessFunction2",
                "fakeCodeUri", "fakeHandler", "fakeRuntime", 100, 100, Collections.<String>emptyList());

        Map<String, Object> additionalTopLevelProperties = function.getAdditionalTopLevelProperties();
        assertTrue(additionalTopLevelProperties.isEmpty());

        Map<String, Object> additionalProperties = function.getAdditionalProperties();

        testValuePath(additionalProperties, "value1", "Environment", "Variables", "key1");
        testValuePath(additionalProperties, "value2", "Environment", "Variables", "key2");
    }

    @Test
    public void testModel_additionalResources() {
        Map<String, TypeProperties> resources = model.getAdditionalResources();

        assertTrue(resources.containsKey("IamRole"));
        TypeProperties tp = resources.get("IamRole");
        assertEquals("AWS::IAM::Role", tp.getType());

        Map<String, Object> properties = tp.getProperties();
        assertEquals("fakeValue", properties.get("fakeKey"));

        assertTrue(tp.getAdditionalProperties().containsKey("foo"));
        assertEquals("bar", tp.getAdditionalProperties().get("foo"));
    }

    @Test
    public void testTemplate_Metadata() {
        assertEquals("2010-09-09", template.getAWSTemplateFormatVersion());
        assertEquals(null, template.getDescription());
        assertEquals("AWS::Serverless-2016-10-31", template.getTransform());
    }

    @Test
    public void testTemplate_AdditionalProperties() {
        Map<String, Object> additionalProperties = template.getAdditionalProperties();
        testValuePath(additionalProperties, "bar", "foo");
        testValuePath(additionalProperties, "bar", "foo1", "foo");
    }

    @Test
    public void testTemplate_ServerlessFunction() {
        Map<String, TypeProperties> resources = template.getResources();
        TypeProperties resource = testTemplateResource(resources, "ServerlessFunction", "AWS::Serverless::Function");

        Map<String, Object> additionalProperties = resource.getAdditionalProperties();
        testValuePath(additionalProperties, "bar", "foo");

        Map<String, Object> properties = resource.getProperties();
        testValuePath(properties, "fakeCodeUri", "CodeUri");
        testValuePath(properties, "fakeHandler", "Handler");
        testValuePath(properties, "S3", "Events", "S3Event", "Type");
        testValuePath(properties, "fakeBucket", "Events", "S3Event", "Properties", "Bucket");
    }

    @Test
    public void testTemplate_ServerlessFunction2() {
        Map<String, TypeProperties> resources = template.getResources();
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
    }

    @Test
    public void testTemplate_IamRole() {
        Map<String, TypeProperties> resources = template.getResources();
        TypeProperties resource = testTemplateResource(resources, "ServerlessFunction", "AWS::Serverless::Function");

        Map<String, Object> additionalProperties = resource.getAdditionalProperties();
        testValuePath(additionalProperties, "bar", "foo");

        Map<String, Object> properties = resource.getProperties();
        testValuePath(properties, "fakeCodeUri", "CodeUri");
        testValuePath(properties, "fakeHandler", "Handler");
        testValuePath(properties, "S3", "Events", "S3Event", "Type");
        testValuePath(properties, "fakeBucket", "Events", "S3Event", "Properties", "Bucket");
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
