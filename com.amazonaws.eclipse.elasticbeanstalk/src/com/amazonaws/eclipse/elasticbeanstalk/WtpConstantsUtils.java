/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerEvent;

public class WtpConstantsUtils {
    public static String lookupPublishKind(int publishKind) {
        switch (publishKind) {
            case IServer.PUBLISH_AUTO:        return "PUBLISH_AUTO";
            case IServer.PUBLISH_CLEAN:       return "PUBLISH_CLEAN";
            case IServer.PUBLISH_FULL:        return "PUBLISH_FULL";
            case IServer.PUBLISH_INCREMENTAL: return "PUBLISH_INCREMENTAL";
            default: return "???";
        }
    }

    public static String lookupDeltaKind(int deltaKind) {
        switch (deltaKind) {
            case EnvironmentBehavior.ADDED:     return "ADDED";
            case EnvironmentBehavior.CHANGED:   return "CHANGED";
            case EnvironmentBehavior.NO_CHANGE: return "NO_CHANGE";
            case EnvironmentBehavior.REMOVED:   return "REMOVED";
            default: return "???";
        }
    }

    public static String lookupServerEventKind(int kind) {
        if ((kind & ServerEvent.MODULE_CHANGE) > 0) return "MODULE_CHANGE";
        if ((kind & ServerEvent.SERVER_CHANGE) > 0) return "SERVER_CHANGE";

        switch (kind) {
            case ServerEvent.MODULE_CHANGE:        return "MODULE_CHANGE";
            case ServerEvent.PUBLISH_STATE_CHANGE: return "PUBLISH_STATE_CHANGE";
            case ServerEvent.RESTART_STATE_CHANGE: return "RESTART_STATE_CHANGE";
            case ServerEvent.SERVER_CHANGE:        return "SERVER_CHANGE";
            case ServerEvent.STATE_CHANGE:         return "STATE_CHANGE";
            default: return "???";
        }
    }

    public static String lookupState(int state) {
        switch (state) {
            case IServer.STATE_STARTED:  return "STATE_STARTED";
            case IServer.STATE_STARTING: return "STATE_STARTING";
            case IServer.STATE_STOPPED:  return "STATE_STOPPED";
            case IServer.STATE_STOPPING: return "STATE_STOPPING";
            case IServer.STATE_UNKNOWN:  return "STATE_UNKNOWN";
            default: return "???";
        }
    }

    public static String lookupPublishState(int state) {
        switch (state) {
            case IServer.PUBLISH_STATE_FULL:        return "PUBLISH_STATE_FULL";
            case IServer.PUBLISH_STATE_INCREMENTAL: return "PUBLISH_STATE_INCREMENTAL";
            case IServer.PUBLISH_STATE_NONE:        return "PUBLISH_STATE_NONE";
            case IServer.PUBLISH_STATE_UNKNOWN:     return "PUBLISH_STATE_UNKNOWN";
            default: return "???";
        }
    }

}
