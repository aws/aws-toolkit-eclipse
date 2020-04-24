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
package com.amazonaws.eclipse.core.telemetry;

public final class ToolkitAnalyticsUtils {

    public static void publishBooleansEvent(AwsToolkitMetricType metricType,
            String name, Boolean value) {
        MetricsDataModel dataModel = new MetricsDataModel(metricType);
        dataModel.addBooleanMetric(name, value);
        dataModel.publishEvent();
    }

    public static void trackSpeedMetrics(
            final ToolkitAnalyticsManager analytics, final String eventType,
            final String metricNameTime, final long time,
            final String metricNameSize, final long size,
            final String metricNameSpeed) {

        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(eventType)
                .addMetric(metricNameTime, time)
                .addMetric(metricNameSize, size)
                .addMetric(metricNameSpeed, (double)size / (double)time)
                .build());
    }
}
