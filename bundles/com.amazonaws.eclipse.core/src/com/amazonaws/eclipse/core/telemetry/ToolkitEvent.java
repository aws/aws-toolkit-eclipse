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
package com.amazonaws.eclipse.core.telemetry;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.time.Instant;

import com.amazonaws.annotation.Immutable;
import com.amazonaws.eclipse.core.telemetry.internal.Constants;
import com.amazonaws.eclipse.core.telemetry.internal.ToolkitSession;

import software.amazon.awssdk.services.toolkittelemetry.model.MetadataEntry;
import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;
import software.amazon.awssdk.services.toolkittelemetry.model.Unit;

@Immutable
public class ToolkitEvent {

    private ToolkitSession session;

    private String eventType;
    private Date timestamp;

    private final Map<String, String> attributes = new HashMap<>();
    private final Map<String, Double> metrics = new HashMap<>();

    public MetricDatum toMetricDatum() {
        // we don't differentiate attributes/metrics anymore so add both
        Collection<MetadataEntry> metadata = this.metrics.entrySet().stream().map((it) -> new MetadataEntry().key(it.getKey()).value(it.getValue().toString()))
                .filter(it -> it.getValue() != null && !it.getValue().isEmpty()).collect(Collectors.toList());
        metadata.addAll(this.attributes.entrySet().stream().map(it -> new MetadataEntry().key(it.getKey()).value(it.getValue()))
                .filter(it -> it.getValue() != null && !it.getValue().isEmpty()).collect(Collectors.toList()));

        final MetricDatum datum = new MetricDatum().metricName(this.eventType).value(1.0).unit(Unit.None).epochTimestamp(Instant.now().toEpochMilli())
                .metadata(metadata);
        return datum;
    }

    /**
     * http://docs.aws.amazon.com/mobileanalytics/latest/ug/limits.html
     */
    public boolean isValid() {
        if (session == null) {
            return false;
        }
        if (session.getId() == null) {
            return false;
        }
        if (session.getStartTimestamp() == null) {
            return false;
        }
        if (eventType == null || eventType.isEmpty()) {
            return false;
        }
        if (timestamp == null) {
            return false;
        }
        if (attributes.size() + metrics.size() > Constants.MAX_ATTRIBUTES_AND_METRICS_PER_EVENT) {
            return false;
        }
        for (Entry<String, String> attribute : attributes.entrySet()) {
            if (attribute.getKey().length() > Constants.MAX_ATTRIBUTE_OR_METRIC_NAME_LENGTH) {
                return false;
            }
            if (attribute.getValue() == null) {
                return false;
            }
            if (attribute.getValue().length() > Constants.MAX_ATTRIBUTE_VALUE_LENGTH) {
                return false;
            }
            if (attribute.getValue().isEmpty()) {
                return false;
            }
        }
        for (Entry<String, Double> metric : metrics.entrySet()) {
            if (metric.getKey().length() > Constants.MAX_ATTRIBUTE_OR_METRIC_NAME_LENGTH) {
                return false;
            }
            if (metric.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * The constructor is intentionally marked as private; caller should use
     * ToolkitEventBuilder to create event instance
     */
    private ToolkitEvent() {
    }

    public static class ToolkitEventBuilder {

        private final ToolkitEvent event = new ToolkitEvent();

        public ToolkitEventBuilder(ToolkitSession session) {
            this.event.session = session;
        }

        public ToolkitEventBuilder setEventType(String eventType) {
            this.event.eventType = eventType;
            return this;
        }

        /**
         * If not specified, the timestamp is by default set to the current time.
         */
        public ToolkitEventBuilder setTimestamp(Date timestamp) {
            this.event.timestamp = timestamp;
            return this;
        }

        public ToolkitEventBuilder addAttribute(String name, String value) {
            this.event.attributes.put(name, value);
            return this;
        }

        public ToolkitEventBuilder addMetric(String name, double value) {
            this.event.metrics.put(name, value);
            return this;
        }

        public ToolkitEventBuilder addBooleanMetric(String name, boolean value) {
            this.event.attributes.put(name, value ? "true" : "false");
            return this;
        }

        public ToolkitEvent build() {
            if (this.event.timestamp == null) {
                this.event.timestamp = new Date();
            }
            return this.event;
        }
    }
}
