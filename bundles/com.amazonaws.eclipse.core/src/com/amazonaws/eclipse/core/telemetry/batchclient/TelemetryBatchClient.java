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
package com.amazonaws.eclipse.core.telemetry.batchclient;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;

public interface TelemetryBatchClient {

    /**
     * To improve performance, the client may cache the incoming event and wait
     * till it has collected a big enough batch of events and then send out the
     * batch with one single API call. The implementation of this API should
     * never block.
     */
    void putEvent(MetricDatum event);

    /**
     * Flush the local cache by sending out all the cached events to the
     * service. This is usually called at the end of an application life-cycle,
     * to make sure all the events are sent before the end of the session.
     */
    void flush();

}
