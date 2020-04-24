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

import com.amazonaws.eclipse.core.accounts.AwsPluginAccountManager;
import com.amazonaws.eclipse.core.telemetry.ToolkitEvent.ToolkitEventBuilder;

/**
 * The entry point for managing Toolkit analytics sessions and events.
 */
public interface ToolkitAnalyticsManager {

    /**
     * Start a new session by sending out a session.start event. After this point,
     * all the events published by this manager will be bound to this new session.
     *
     * @param accountManager   The account manager needed to start the credentials
     *                         changed listener
     * @param forceFlushEvents true if the session.start event should be sent
     *                         immediately after the method call.
     */
    public void startSession(AwsPluginAccountManager accountManager, boolean forceFlushEvents);

    /**
     * Terminate the current session (if any) by sending out a session.stop event.
     * After this point, any call of {@link #publishEvent(ToolkitEvent)} won't have
     * any effect, until the next {@link #startSession(boolean)} call is made.
     *
     * @param forceFlushEvents true if all the cached events should be forcefully
     *                         sent out to the Analytics service after this method
     *                         call.
     */
    public void endSession(boolean forceFlushEvents);

    /**
     * @return a builder for {@link ToolkitEvent}s. The generated event will by
     *         default be bound to the current session of the manager.
     */
    public ToolkitEventBuilder eventBuilder();

    /**
     * Publish a new {@link ToolkitEvent}. This method call won't take any affect if
     * the manager is not currently tracking an on-going session.
     *
     * @param event the toolkit event to be published.
     */
    public void publishEvent(ToolkitEvent event);

    /**
     * Enable or disable the analytics collection. When disabled, none of the
     * methods of the manager takes any effect.
     */
    public void setEnabled(boolean enabled);
}
