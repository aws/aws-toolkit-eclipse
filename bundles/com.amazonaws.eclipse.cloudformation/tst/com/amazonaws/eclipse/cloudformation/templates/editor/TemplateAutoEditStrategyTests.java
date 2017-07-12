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
package com.amazonaws.eclipse.cloudformation.templates.editor;

import static com.amazonaws.eclipse.cloudformation.templates.editor.TemplateAutoEditStrategy.parseOneLine;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateAutoEditStrategy.IndentContext;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateAutoEditStrategy.IndentType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TemplateAutoEditStrategyTests {

    @Test
    public void testAutoEditStrategy() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, AutoEditStrategyTestCase> testCases = mapper.readValue(
                TemplateAutoEditStrategyTests.class.getResourceAsStream("template-auto-edit-test-cases.json"),
                new TypeReference<Map<String, AutoEditStrategyTestCase>>() {});
        for (Entry<String, AutoEditStrategyTestCase> entry : testCases.entrySet()) {
            System.out.println("Testing test case: " + entry.getKey());
            AutoEditStrategyTestCase testCase = entry.getValue();
            IndentContext expectedIndentContext = new IndentContext(IndentType.valueOf(testCase.indentType), testCase.indentValue);
            testParseOneLine(testCase.line, testCase.offset, expectedIndentContext);
        }
    }

    private void testParseOneLine(String line, int offset, IndentContext expectedIndentContext) {
        IndentContext actualContext = parseOneLine(line, offset);
        assertIndentContextEquals(actualContext, expectedIndentContext);
    }

    private void assertIndentContextEquals(IndentContext actualContext, IndentContext expectedContext) {
        assertTrue(actualContext.indentType == expectedContext.indentType);
        assertTrue(actualContext.indentValue == expectedContext.indentValue);
    }

    private static class AutoEditStrategyTestCase {
        String line;
        int offset;
        String indentType;
        int indentValue;

        @JsonCreator
        public AutoEditStrategyTestCase(
                @JsonProperty("line") String line,
                @JsonProperty("offset") int offset,
                @JsonProperty("indentType") String indentType,
                @JsonProperty("indentValue") int indentValue) {
            this.line = line;
            this.offset = offset;
            this.indentType = indentType;
            this.indentValue = indentValue;
        }
    }
}
