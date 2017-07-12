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
package com.amazonaws.eclipse.core.mobileanalytics.batchclient.internal;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.eclipse.core.mobileanalytics.batchclient.MobileAnalyticsBatchClient;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.mobileanalytics.AmazonMobileAnalyticsAsync;
import com.amazonaws.services.mobileanalytics.AmazonMobileAnalyticsAsyncClient;
import com.amazonaws.services.mobileanalytics.model.Event;
import com.amazonaws.services.mobileanalytics.model.PutEventsRequest;
import com.amazonaws.services.mobileanalytics.model.PutEventsResult;

/**
 * An implementation of MobileAnalyticsBatchClient which uses a bounded queue
 * for caching incoming events and a single-threaded service client for sending
 * out event batches. It also guarantees that the events are sent in the same
 * order as they are accepted by the client.
 */
public class MobileAnalyticsBatchClientImpl implements MobileAnalyticsBatchClient {

    private static final int MIN_EVENT_BATCH_SIZE = 20;
    private static final int MAX_QUEUE_SIZE = 500;

    /**
     * Mobile Analytics async client with a single background thread
     */
    private final AmazonMobileAnalyticsAsync mobileAnalytics;

    /**
     * The x-amz-client-context header string to be included in every PutEvents
     * request
     */
    private final String clientContextString;

    /**
     * For caching incoming events for batching
     */
    private final EventQueue eventQueue = new EventQueue();

    /**
     * To keep track of the on-going putEvents request and make sure only one
     * request can be made at a time.
     */
    private final AtomicBoolean isSendingPutEventsRequest = new AtomicBoolean(false);

    public MobileAnalyticsBatchClientImpl(
            AWSCredentialsProvider credentialsProvider,
            String clientContextString) {
        this.mobileAnalytics = new AmazonMobileAnalyticsAsyncClient(
                credentialsProvider, Executors.newFixedThreadPool(1));
        this.clientContextString = clientContextString;
    }

    @Override
    public void putEvent(Event event) {

        // we don't lock the queue when accepting incoming event, and the
        // queue size is only a rough estimate.
        int queueSize = eventQueue.size();

        // keep the queue bounded
        if (queueSize >= MAX_QUEUE_SIZE) {
            tryDispatchAllEventsAsync();
            return;
        }

        eventQueue.addToTail(event);

        if (queueSize >= MIN_EVENT_BATCH_SIZE) {
            tryDispatchAllEventsAsync();
        }
    }

    @Override
    public void flush() {
        tryDispatchAllEventsAsync();
    }

    /**
     * To make sure the order of the analytic events is preserved, this method
     * call will immediately return if there is an ongoing PutEvents call.
     */
    private void tryDispatchAllEventsAsync() {

        boolean contentionDetected = this.isSendingPutEventsRequest
                .getAndSet(true);

        if (!contentionDetected) {
            dispatchAllEventsAsync();
        }
    }

    /**
     * Only one thread can call this method at a time
     */
    private void dispatchAllEventsAsync() {

        final List<Event> eventsBatch = this.eventQueue.pollAllQueuedEvents();

        mobileAnalytics.putEventsAsync(
                new PutEventsRequest().withClientContext(clientContextString)
                        .withEvents(eventsBatch),
                new AsyncHandler<PutEventsRequest, PutEventsResult>() {

                    @Override
                    public void onSuccess(PutEventsRequest arg0, PutEventsResult arg1) {
                        markRequestDone();
                    }

                    @Override
                    public void onError(Exception arg0) {
                        restoreEventsQueue(eventsBatch);
                        markRequestDone();
                    }

                    private void restoreEventsQueue(List<Event> failedBatch) {
                        MobileAnalyticsBatchClientImpl.this.eventQueue
                                .addToHead(failedBatch);
                    }

                    private void markRequestDone() {
                        MobileAnalyticsBatchClientImpl.this.isSendingPutEventsRequest
                                .set(false);
                    }

                });
    }

}
