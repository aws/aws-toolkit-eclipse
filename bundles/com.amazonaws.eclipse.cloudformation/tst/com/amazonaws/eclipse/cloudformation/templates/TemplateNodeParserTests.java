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
package com.amazonaws.eclipse.cloudformation.templates;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TemplateNodeParserTests {

    @Test
    public void testTemplateNodeParser() throws JsonParseException, JsonMappingException, IOException {
        TemplateNodeParser parser = new TemplateNodeParser();
        Map<String, TemplateNodeParserTestCase> testCases = new ObjectMapper().readValue(
                TemplateNodeParserTests.class.getResourceAsStream("template-node-parser-test-cases.json"),
                new TypeReference<Map<String, TemplateNodeParserTestCase>>() {});
        for (Entry<String, TemplateNodeParserTestCase> entry : testCases.entrySet()) {
            System.out.println("Testing test case " + entry.getKey());
            testParse(parser, entry.getValue());
        }
    }

    private void testParse(TemplateNodeParser parser, TemplateNodeParserTestCase testCase) {
        try {
            parser.parse(testCase.intput);
        } catch (Exception e) {
            if (!testCase.throwException) {
                Assert.fail("Assert failure: unexpected exception was thrown: " + e.getMessage());
            }
        } finally {
            Assert.assertEquals(testCase.path, parser.getPath());
        }
    }

    private static class TemplateNodeParserTestCase {
        String intput;
        Boolean throwException;
        String path;

        @JsonCreator
        public TemplateNodeParserTestCase(
                @JsonProperty("input") String intput,
                @JsonProperty("throwException") Boolean throwException,
                @JsonProperty("path") String path) {
            this.intput = intput;
            this.throwException = throwException;
            this.path = path;
        }
    }
}
