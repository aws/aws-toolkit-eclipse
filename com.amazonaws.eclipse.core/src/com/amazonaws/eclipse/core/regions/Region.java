/*
 * Copyright 2011 Amazon Technologies, Inc. 
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
package com.amazonaws.eclipse.core.regions;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata for an AWS region, including it's name, unique ID and what services
 * are available.
 */
public final class Region {
    private String name;
    private String id;
    private Map<String, String> serviceEndpoints = new HashMap<String, String>();

    public Region(String name, String id) {
        this.name = name;
        this.id = id;
    }

    /**
     * The display name for this region. This is the value that should be shown
     * in UIs when this region is used.
     * 
     * @return The display name for this region.
     */
    public String getName() {
        return name;
    }

    /**
     * The unique system ID for this region; ex: us-east-1.
     * 
     * @return The unique system ID for this region.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a map of the available services in this region and their
     * endpoints. The keys of the map are service abbreviations, as defined in
     * {@link ServiceAbbreviations}, and the values are the endpoint URLs.
     * 
     * @return A map of the available services in this region.
     */
    public Map<String, String> getServiceEndpoints() {
        return serviceEndpoints;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Region == false) return false;

        Region region = (Region)obj;
        return this.getId().equals(region.getId());
    }

    @Override
    public String toString() {
        return "Region: " + name + " (" + id + ")";
    }
}