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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;

/**
 * Utilities for initializing default values for new server instances.
 */
public class ServerDefaultsUtils {

    public static void setDefaultServerName(IServerWorkingCopy serverWorkingCopy, String environmentName) {
        Set<String> existingServerNames = new HashSet<>();
        for (IServer server : ServerCore.getServers()) {
            existingServerNames.add(server.getName());
        }

        String host = serverWorkingCopy.getHost();
        String newServerName = environmentName + " at " + host;

        int count = 1;
        while (existingServerNames.contains(newServerName)) {
            newServerName = environmentName + " (" + count++ + ") " +
            "at " + serverWorkingCopy.getHost();
        }

        serverWorkingCopy.setName(newServerName);
    }

    public static void setDefaultHostName(IServerWorkingCopy serverWorkingCopy, String regionEndpoint) {
        Region region = RegionUtils.getRegionByEndpoint(regionEndpoint);
        String regionName = region.getName();

        serverWorkingCopy.setHost("AWS Elastic Beanstalk - " + regionName);
    }
}
