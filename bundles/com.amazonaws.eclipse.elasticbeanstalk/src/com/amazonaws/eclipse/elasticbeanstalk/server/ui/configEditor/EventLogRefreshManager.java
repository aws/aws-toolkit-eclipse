/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of event logs for environments with active deployments in
 * progress and periodically refreshes them.
 */
public class EventLogRefreshManager {

    private final static EventLogRefreshManager instance = new EventLogRefreshManager();

    private final ScheduledThreadPoolExecutor executor;

    private Set<EventLogEditorSection> activeEventLogs = new CopyOnWriteArraySet<>();

    private EventLogRefreshManager() {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(new EventLogRefresher(), 0, 30, TimeUnit.SECONDS);
    }

    private class EventLogRefresher implements Runnable {
        @Override
        public void run() {
            for (EventLogEditorSection eventLog : activeEventLogs) {
                eventLog.refresh();
            }
        }
    }

    public static EventLogRefreshManager getInstance() {
        return instance;
    }

    public void startAutoRefresh(EventLogEditorSection eventLog) {
        activeEventLogs.add(eventLog);
    }

    public void stopAutoRefresh(EventLogEditorSection eventLog) {
        // Refresh one more time before we stop auto-refreshing,
        // to ensure that we don't miss the last events
        eventLog.refresh();
        activeEventLogs.remove(eventLog);
    }
}