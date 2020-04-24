/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codestar;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.telemetry.ToolkitEvent.ToolkitEventBuilder;

public class CodeStarAnalytics {
    private static final ToolkitAnalyticsManager ANALYTICS = AwsToolkitCore.getDefault().getAnalyticsManager();

    // Import AWS CodeStar Project
    private static final String EVENT_IMPORT_PROJECT = "codestar_importProject";

    // Attribute
    private static final String ATTR_NAME_END_RESULT = "result";

    public static void trackImportProject(EventResult result) {
        publishEventWithAttributes(EVENT_IMPORT_PROJECT, ATTR_NAME_END_RESULT, result.getResultText());
    }

    private static void publishEventWithAttributes(String eventType, String... attributes) {
        ToolkitEventBuilder builder = ANALYTICS.eventBuilder().setEventType(eventType);
        for (int i = 0; i < attributes.length; i += 2) {
            builder.addAttribute(attributes[i], attributes[i + 1]);
        }
        ANALYTICS.publishEvent(builder.build());
    }

    public static enum EventResult {

        SUCCEEDED("Succeeded"),
        FAILED("Failed"),
        CANCELED("Canceled")
        ;

        private final String resultText;

        private EventResult(String resultText) {
            this.resultText = resultText;
        }

        public String getResultText() {
            return resultText;
        }
    }
}
