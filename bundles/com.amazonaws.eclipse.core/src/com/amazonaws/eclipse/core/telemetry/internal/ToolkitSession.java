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

import java.util.Date;
import java.util.UUID;

import com.amazonaws.annotation.Immutable;
import com.amazonaws.services.mobileanalytics.model.Session;
import com.amazonaws.util.DateUtils;

@Immutable
public class ToolkitSession {

    private final String id;
    private final Date startTimestamp;

    ToolkitSession(String id, Date startTimestamp) {
        this.id = id;
        this.startTimestamp = startTimestamp;
    }

    public static ToolkitSession newSession() {
        String id = UUID.randomUUID().toString();
        Date now = new Date();
        return new ToolkitSession(id, now);
    }


    public String getId() {
        return id;
    }

    public Date getStartTimestamp() {
        return startTimestamp;
    }

    public Session toMobileAnalyticsSession() {
        Session session = new Session();
        session.setId(this.id);
        session.setStartTimestamp(DateUtils.formatISO8601Date(this.startTimestamp));
        return session;
    }

}
