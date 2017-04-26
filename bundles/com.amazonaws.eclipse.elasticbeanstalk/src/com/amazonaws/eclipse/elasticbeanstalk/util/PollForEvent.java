/*
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.util;

import java.util.concurrent.TimeUnit;

/**
 * Class to poll until a given {@link Event} has occurred at a specified interval and up to a max
 * number of iterations
 */
public class PollForEvent {

    /**
     * Interface to represent a certain event and whether it has occurred or not.
     */
    public interface Event {

        /**
         * @return True if event has occurred, false otherwise
         */
        public boolean hasEventOccurred();
    }

    /**
     * Represents an interval of time with units specified
     */
    public static class Interval {

        private final long millis;

        public Interval(long value, TimeUnit unit) {
            millis = unit.toMillis(value);
        }

        private long getMillis() {
            return millis;
        }
    }

    private final Interval interval;
    private final long maxNumberOfIntervalsToPollFor;

    /**
     * @param interval
     *            {@link Interval} of time to wait between checking the event status
     * @param maxNumberOfIntervalsToPollFor
     *            Max intervals to poll for before giving up
     */
    public PollForEvent(Interval interval, int maxNumberOfIntervalsToPollFor) {
        this.interval = interval;
        this.maxNumberOfIntervalsToPollFor = maxNumberOfIntervalsToPollFor;
    }

    public void poll(Event event) {
        int currentPollInterval = 0;
        while (!event.hasEventOccurred() && !hasReachedMaxPollInterval(currentPollInterval)) {
            try {
                Thread.sleep(interval.getMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            currentPollInterval++;
        }
    }

    public boolean hasReachedMaxPollInterval(int currentPollInterval) {
        return currentPollInterval >= maxNumberOfIntervalsToPollFor;
    }

}