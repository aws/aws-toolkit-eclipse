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
package com.amazonaws.eclipse.core.mobileanalytics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.diagnostic.utils.PlatformEnvironmentDataCollector;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitEvent.ToolkitEventBuilder;
import com.amazonaws.services.errorreport.model.PlatformDataModel;

public class MetricsDataModel {

    private static final String JAVA_VERSION = "JavaVersion";
    private static final String TOOLKIT_VERSION = "ToolkitVersion";

    private static final Map<String, String> METRICS_METADATA = new HashMap<>();

    static {
        PlatformDataModel platformDataModel = PlatformEnvironmentDataCollector.getData();
        METRICS_METADATA.put(JAVA_VERSION, platformDataModel.getLanguageVersion());
    }

    private final ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault().getAnalyticsManager();
    private final AwsToolkitMetricType metricType;
    private ToolkitEventBuilder eventBuilder;

    public MetricsDataModel(AwsToolkitMetricType metricType) {
        this.metricType = metricType;
        this.eventBuilder = analytics.eventBuilder();
        this.eventBuilder.setEventType(metricType.getName());
    }

    public AwsToolkitMetricType getMetricType() {
        return metricType;
    }

    public final MetricsDataModel addAttribute(String name, String value) {
        eventBuilder.addAttribute(name, value);
        return this;
    }

    public final MetricsDataModel addBooleanMetric(String name, Boolean value) {
        eventBuilder.addBooleanMetric(name, value);
        return this;
    }

    public final MetricsDataModel addMetric(String name, Double value) {
        eventBuilder.addMetric(name, value);
        return this;
    }

    public final void publishEvent() {
        for (Entry<String, String> entry : METRICS_METADATA.entrySet()) {
            addAttribute(entry.getKey(), entry.getValue());
        }
        analytics.publishEvent(eventBuilder.build());
        eventBuilder = analytics.eventBuilder().setEventType(metricType.getName());
    }
}
