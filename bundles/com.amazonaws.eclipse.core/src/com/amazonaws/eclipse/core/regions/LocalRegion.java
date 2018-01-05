/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * The special local "region", comprised of test services running on the
 * loopback adapter.
 */
class LocalRegion implements Region {

    public static final LocalRegion INSTANCE = new LocalRegion();

    private final Map<String, String> serviceEndpoints =
        new ConcurrentHashMap<>();

    private final Map<String, Service> servicesByName =
        new ConcurrentHashMap<>();

    private LocalRegion() {
    }

    /**
     * The display name for this region. This is the value that should be shown
     * in UIs when this region is used.
     *
     * @return The display name for this region.
     */
    @Override
    public String getName() {
        return "Local (localhost)";
    }

    /**
     * The unique system ID for this region; ex: us-east-1.
     *
     * @return The unique system ID for this region.
     */
    @Override
    public String getId() {
        return "local";
    }

    /**
     * Returns a map of the available services in this region and their
     * endpoints. The keys of the map are service abbreviations, as defined in
     * {@link ServiceAbbreviations}, and the values are the endpoint URLs.
     *
     * @return A map of the available services in this region.
     */
    @Override
    public Map<String, String> getServiceEndpoints() {
        return serviceEndpoints;
    }

    /**
     * Returns a map of the available services in this region. THe keys of the
     * map are service abbreviations, as defined in {@link ServiceAbbreviations},
     * and the values are {@link Service} objects that provide information on
     * connecting to the service.
     *
     * @return A map of the available services in this region.
     */
    @Override
    public Map<String, Service> getServicesByName() {
        return servicesByName;
    }

    /**
     * Returns the endpoint for the service given.
     *
     * @see ServiceAbbreviations
     */
    @Override
    public String getServiceEndpoint(String serviceName) {
        return serviceEndpoints.get(serviceName);
    }

    /**
     * Returns whether the given service is supported in this region.
     *
     * @see ServiceAbbreviations
     */
    @Override
    public boolean isServiceSupported(String serviceName) {
        return serviceEndpoints.containsKey(serviceName);
    }

    /**
     * Returns the relative path to a small flag icon representing this region.
     */
    @Override
    public String getFlagIconPath() {
        return null;
    }

    /**
     * Returns the image for this region's flag.
     */
    @Override
    public Image getFlagImage() {
        return AwsToolkitCore.getDefault().getImageRegistry()
            .get(AwsToolkitCore.IMAGE_AWS_ICON);
    }

    /**
     * Returns the flag's image descriptor.
     */
    @Override
    public ImageDescriptor getFlagImageDescriptor() {
        return  AwsToolkitCore.getDefault().getImageRegistry()
            .getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON);
    }

    @Override
    public String toString() {
        return "Region: Local (localhost) (local)";
    }

    @Override
    public String getRegionRestriction() {
        return "IsLocalAccount";
    }
}
