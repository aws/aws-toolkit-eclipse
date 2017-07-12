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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.eclipse.wst.server.core.util.WebResource;


public class ElasticBeanstalkLaunchableAdapter extends LaunchableAdapterDelegate {

    private static final Map<String, ElasticBeanstalkHttpLaunchable> launchables = new HashMap<>();

    /**
     * Returns the launchable currently associated with the given server.
     */
    public static ElasticBeanstalkHttpLaunchable getLaunchable(IServer server) {
        return launchables.get(server.getId());
    }

    @Override
    public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) {
        Object serverDelegate = server.loadAdapter(ServerDelegate.class, null);
        if (serverDelegate instanceof Environment == false) {
            return null;
        }

        if (!(moduleArtifact instanceof Servlet) &&
            !(moduleArtifact instanceof WebResource))
            return null;
        if (moduleArtifact.getModule().loadAdapter(IWebModule.class, null) == null)
            return null;

        ElasticBeanstalkHttpLaunchable launchable = new ElasticBeanstalkHttpLaunchable(server, moduleArtifact);
        launchables.put(server.getId(), launchable);
        return launchable;
    }

}
