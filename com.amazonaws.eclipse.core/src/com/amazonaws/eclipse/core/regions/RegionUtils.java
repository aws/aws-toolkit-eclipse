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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Utilities for working with regions.
 */
public class RegionUtils {

    private static List<Region> regions;

    /**
     * Returns true if the specified service is available in the current/active
     * region, otherwise returns false.
     * 
     * @param serviceAbbreviation
     *            The abbreviation of the service to check.
     * 
     * @return True if the specified service is available in the current/active
     *         region, otherwise returns false.
     * 
     * @see ServiceAbbreviations
     */
    public static boolean isServiceSupportedInCurrentRegion(String serviceAbbreviation) {
        Region currentRegion = getCurrentRegion();
        return currentRegion.getServiceEndpoints().get(serviceAbbreviation) != null;    
    }
    
    /**
     * Returns a list of the available AWS regions.
     */
    public static List<Region> getRegions() {
        if (regions == null) {
            ClassLoader classLoader = RegionUtils.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("/etc/regions.xml");
            RegionMetadataParser parser = new RegionMetadataParser();
            regions = parser.parseRegionMetadata(inputStream);
        }
        
        return regions;
    }
    
    /**
     * Returns the default/active region that the user previously selected.
     */
    public static Region getCurrentRegion() {
        IPreferenceStore preferenceStore = AwsToolkitCore.getDefault().getPreferenceStore();
        String defaultRegion = preferenceStore.getString(PreferenceConstants.P_DEFAULT_REGION);
        
        for (Region region : getRegions()) {
            if (region.getId().equals(defaultRegion)) return region; 
        }

        throw new RuntimeException("Unable to determine default region");
    }

    /**
     * Searches through all known regions to find one with any service at the
     * specified endpoint. If no region is found with a service at that
     * endpoint, an exception is thrown.
     * 
     * @param endpoint
     *            The endpoint for any service residing in the desired region.
     * 
     * @return The region containing any service running at the specified
     *         endpoint, otherwise an exception is thrown if no region is found
     *         with a service at the specified endpoint.
     */
    public static Region getRegionByEndpoint(String endpoint) {
        URL targetEndpointUrl = null;
        try {
            targetEndpointUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to parse service endpoint: " + e.getMessage());
        }
        
        String targetHost = targetEndpointUrl.getHost();
        for (Region region : getRegions()) {
            for (String serviceEndpoint : region.getServiceEndpoints().values()) {
                try {
                    URL serviceEndpointUrl = new URL(serviceEndpoint);
                    if (serviceEndpointUrl.getHost().equals(targetHost)) return region;
                } catch (MalformedURLException e) {
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, "Unable to parse service endpoint: " + serviceEndpoint, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }
        
        throw new RuntimeException("No region found with any service for endpoint " + endpoint);
    }
    
}
