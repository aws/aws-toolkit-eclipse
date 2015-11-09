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
package com.amazonaws.eclipse.core.mobileanalytics.internal;

import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitEvent;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitEvent.ToolkitEventBuilder;

public class NoOpToolkitAnalyticsManager implements ToolkitAnalyticsManager {

    public void startSession(boolean forceFlushEvents) {
    }

    public void endSession(boolean forceFlushEvents) {
    }

    public ToolkitEventBuilder eventBuilder() {
        return new ToolkitEventBuilder(null);
    }

    public void publishEvent(ToolkitEvent event) {
    }

    public void setEnabled(boolean enabled) {
    }

}
