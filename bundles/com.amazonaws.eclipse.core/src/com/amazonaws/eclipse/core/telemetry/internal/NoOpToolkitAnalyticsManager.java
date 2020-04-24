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
package com.amazonaws.eclipse.core.telemetry.internal;

import com.amazonaws.eclipse.core.accounts.AwsPluginAccountManager;
import com.amazonaws.eclipse.core.telemetry.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.telemetry.ToolkitEvent;
import com.amazonaws.eclipse.core.telemetry.ToolkitEvent.ToolkitEventBuilder;

public class NoOpToolkitAnalyticsManager implements ToolkitAnalyticsManager {

    @Override
	public void startSession(AwsPluginAccountManager accountManager, boolean forceFlushEvents) {
    }

    @Override
    public void endSession(boolean forceFlushEvents) {
    }

    @Override
    public ToolkitEventBuilder eventBuilder() {
        return new ToolkitEventBuilder(null);
    }

    @Override
    public void publishEvent(ToolkitEvent event) {
    }

    @Override
    public void setEnabled(boolean enabled) {
    }
}
