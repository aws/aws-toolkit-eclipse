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
import java.net.MalformedURLException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Test CloudFormation Template schema file to be valid, and up to date.
 */
public class TemplateSchemaTests {

    private TemplateSchema schema;

    @Before
    public void setup() throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
        schema = TemplateSchemaParser.getDefaultSchema();
    }

    @Test
    public void testIntrinsicFunctions() {
        Map<String, IntrinsicFunction> intrinsicFunctions = schema.getIntrinsicFunctions();
        Assert.assertNotNull(intrinsicFunctions);
        IntrinsicFunction function = intrinsicFunctions.get("Fn::ImportValue");
        Assert.assertTrue(function != null);
        Assert.assertEquals("String", function.getReturnType());
    }

    @Test
    public void testPseudoParameters() {
        Map<String, PseudoParameter> pseudoParameters = schema.getPseudoParameters();
        Assert.assertNotNull(pseudoParameters);
        PseudoParameter parameter = pseudoParameters.get("AWS::NotificationARNs");
        Assert.assertTrue(parameter != null);
        Assert.assertEquals("String", parameter.getArrayType());
    }

    //TODO more tests for verifying the structure of the schema

}
