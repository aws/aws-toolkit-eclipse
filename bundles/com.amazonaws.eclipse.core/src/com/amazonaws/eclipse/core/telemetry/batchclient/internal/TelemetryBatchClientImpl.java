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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.ClientContextConfig;
import com.amazonaws.eclipse.core.telemetry.TelemetryClientV2;
import com.amazonaws.eclipse.core.telemetry.batchclient.TelemetryBatchClient;

import software.amazon.awssdk.services.toolkittelemetry.model.MetricDatum;

/**
 * An implementation of MobileAnalyticsBatchClient which uses a bounded queue
 * for caching incoming events and a single-threaded service client for sending
 * out event batches. It also guarantees that the events are sent in the same
 * order as they are accepted by the client.
 */
public class TelemetryBatchClientImpl implements TelemetryBatchClient {

	private static final int MIN_EVENT_BATCH_SIZE = 20;
	private static final int MAX_QUEUE_SIZE = 500;

	private final TelemetryClientV2 telemetryClient;

	/**
	 * For caching incoming events for batching
	 */
	private final EventQueue eventQueue = new EventQueue();

	/**
	 * To keep track of the on-going putEvents request and make sure only one
	 * request can be made at a time.
	 */
	private final AtomicBoolean isSendingPutEventsRequest = new AtomicBoolean(false);

	public TelemetryBatchClientImpl(AWSCredentialsProvider credentialsProvider, ClientContextConfig clientContextConfig) {
		this.telemetryClient = new TelemetryClientV2(credentialsProvider, clientContextConfig);
	}

	@Override
	public void putEvent(MetricDatum event) {

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
	 * To make sure the order of the analytic events is preserved, this method call
	 * will immediately return if there is an ongoing PutEvents call.
	 */
	private void tryDispatchAllEventsAsync() {

		boolean contentionDetected = this.isSendingPutEventsRequest.getAndSet(true);
		AwsToolkitCore.getDefault().logInfo("Trying to dispatch events, contention detected: " + contentionDetected);

		if (!contentionDetected) {
			dispatchAllEventsAsync();
		}
	}

	/**
	 * Only one thread can call this method at a time
	 */
	private void dispatchAllEventsAsync() {
		final List<MetricDatum> eventsBatch = this.eventQueue.pollAllQueuedEvents();

		Job j = Job.create("Posting telemetry", new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					telemetryClient.publish(eventsBatch);
					AwsToolkitCore.getDefault().logInfo("Posting telemetry succeeded");
				} catch (Exception e) {
					eventQueue.addToHead(eventsBatch);
					AwsToolkitCore.getDefault().logError("Unable to post telemetry", e);
				} finally {
					isSendingPutEventsRequest.set(false);
				}
			}
		});
		j.setSystem(true);
		j.schedule();
	}
}
