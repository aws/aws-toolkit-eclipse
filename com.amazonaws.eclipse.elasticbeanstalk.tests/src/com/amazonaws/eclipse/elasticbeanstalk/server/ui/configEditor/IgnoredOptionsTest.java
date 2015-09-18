/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel.IgnoredOptions;

public class IgnoredOptionsTest {

    private IgnoredOptions ignoredOptions;

    @Before
    public void setup() {
        ignoredOptions = new IgnoredOptions(new HashMap<String, List<String>>());
    }

    @Test
    public void ignoreNamespace_ignoresAllOptionsInThatNamespace() {
        String ignoredNamespace = "some-namespace";
        ignoredOptions.ignoreNamespace(ignoredNamespace);
        assertTrue(ignoredOptions.isNamespaceIgnored(ignoredNamespace));
        assertTrue(ignoredOptions.isOptionIgnored(ignoredNamespace, "some-option"));
        assertFalse(ignoredOptions.isNamespaceIgnored("some-other-namespace"));
    }

    @Test
    public void ignoreOptions_IgnoreMultipleOptions() {
        String namespace = "some-namespace";
        String optionName = "some-option";
        String otherOption = "some-other-option";
        ignoredOptions.ignoreOption(namespace, optionName);
        ignoredOptions.ignoreOption(namespace, otherOption);
        assertTrue(ignoredOptions.isOptionIgnored(namespace, optionName));
        assertTrue(ignoredOptions.isOptionIgnored(namespace, otherOption));
    }

    @Test
    public void ignoreOption_OnlyIgnoresThatOptionNotTheNamespace() {
        String namespace = "some-namespace";
        String optionName = "some-option";
        ignoredOptions.ignoreOption(namespace, optionName);
        assertFalse(ignoredOptions.isNamespaceIgnored(namespace));
        assertTrue(ignoredOptions.isOptionIgnored(namespace, optionName));
        assertFalse(ignoredOptions.isOptionIgnored(namespace, "some-other-option"));
    }
}
