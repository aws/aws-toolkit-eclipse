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
package com.amazonaws.eclipse.lambda.launching;

import java.util.regex.Matcher;

import static com.amazonaws.eclipse.lambda.launching.SamLocalConsoleLineTracker.URL_PATTERN;

import org.junit.Assert;
import org.junit.Test;

public class SamLocalConsoleLineTrackerTest {
    private TestCase[] testCases = {
            new TestCase("foo: http://234.12.21.12:2000", true, "http://234.12.21.12:2000"),
            new TestCase("foo: http://234.12.21.12:2000bar", true, "http://234.12.21.12:2000"),
            new TestCase("foo: https://234.12.21.12:3000", true, "https://234.12.21.12:3000"),
            new TestCase("foo: 234.12.255.12:2012", true, "234.12.255.12:2012"),
            new TestCase("foo: localhost:3000", true, "localhost:3000"),
            new TestCase("217.0.0.1:3000", true, "217.0.0.1:3000"),
            new TestCase("217.0.0.1", false, null),
            new TestCase("This is foo", false, null)
        };

    private static class TestCase {
        String text;
        boolean valid;
        String ipAddress;

        private TestCase(String text, boolean valid, String ipAddress) {
            this.text = text;
            this.valid = valid;
            this.ipAddress = ipAddress;
        }
    }

    @Test
    public void testPatterns() {
        for (TestCase testCase : testCases) {
            doTestPattern(testCase.text, testCase.valid, testCase.ipAddress);
        }
    }

    private void doTestPattern(String line, boolean match, String matchString) {
        Matcher matcher = URL_PATTERN.matcher(line);
        Assert.assertEquals(match, matcher.find());
        if (match) {
            String actualMatchString = matcher.group();
            Assert.assertEquals(matchString, actualMatchString);
        }
    }
}
