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
package com.amazonaws.eclipse.core.telemetry.batchclient.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;

/**
 * A very simplistic implementation of a double-ended queue with non-blocking
 * write access.
 */
class EventQueue {

    private final ConcurrentLinkedQueue<MetricDatum> headQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MetricDatum> tailQueue = new ConcurrentLinkedQueue<>();

    /**
     * Not thread safe.
     *
     * @throws IllegalStateException
     *             if this queue still contains event added via any previous
     *             addToHead calls
     */
    public void addToHead(List<MetricDatum> events) {
        if (!headQueue.isEmpty()) {
            throw new IllegalStateException();
        }
        headQueue.addAll(events);
    }

    public void addToTail(MetricDatum event) {
        tailQueue.add(event);
    }

    /**
     * Not thread safe.
     */
    public int size() {
        return headQueue.size() + tailQueue.size();
    }

    /**
     * Not thread safe.
     */
    public List<MetricDatum> pollAllQueuedEvents() {
        List<MetricDatum> events = new LinkedList<>();
        events.addAll(pollAll(headQueue));
        events.addAll(pollAll(tailQueue));
        return events;
    }

    private List<MetricDatum> pollAll(Queue<MetricDatum> queue) {
        List<MetricDatum> events = new LinkedList<>();
        while (true) {
            MetricDatum polled = queue.poll();
            if (polled != null) {
                events.add(polled);
            } else {
                break;
            }
        }
        return events;
    }

}
