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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsToolkitHttpClient;
import com.amazonaws.eclipse.core.HttpClientFactory;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.regions.Regions;

/**
 * Utilities for loading and working with regions. The AWS regions loading priorities are:
 *
 * {@link #P_REGIONS_FILE_OVERRIDE}              // Use the regions file from the provided system property - used for accessing private regions
 * > {@link #P_USE_LOCAL_REGION_FILE}            // Use the embedded regions file /etc/regions.xml explicitly - used for testing purpose
 * > {@link #LOCAL_REGION_FILE_OVERRIDE}         // Use the embedded regions file /etc/override.xml if exists - used for accessing private partitions
 * > {@link #CLOUDFRONT_DISTRO}                  // Use the remote shared ServiceEndPoints.xml file - used in most cases for accessing public regions
 * > /etc/regions.xml                            // Use the local embedded file if failed to download the remote file - fall back to the embedded version which could be outdated
 */
public class RegionUtils {

    public static final String S3_US_EAST_1_REGIONAL_ENDPOINT = "https://s3-external-1.amazonaws.com";

    private static final String CLOUDFRONT_DISTRO = "http://vstoolkit.amazonwebservices.com/";
    private static final String REGIONS_METADATA_S3_OBJECT = "ServiceEndPoints.xml";

    // System property name whose value is the path of the overriding file.
    private static final String P_REGIONS_FILE_OVERRIDE = RegionUtils.class.getName() + ".fileOverride";
    // System property name whose value is a boolean whether to use the embedded region file.
    private static final String P_USE_LOCAL_REGION_FILE = RegionUtils.class.getName() + ".useLocalRegionFile";
    // This file overrides the remote ServiceEndPoints.xml file if exists.
    private static final String LOCAL_REGION_FILE_OVERRIDE = "/etc/regions-override.xml";
    private static final String LOCAL_REGION_FILE = "/etc/regions.xml";

    private static List<Region> regions;

    /**
     * Returns true if the specified service is available in the current/active
     * region, otherwise returns false.
     *
     * @param serviceAbbreviation
     *            The abbreviation of the service to check.
     * @return True if the specified service is available in the current/active
     *         region, otherwise returns false.
     * @see ServiceAbbreviations
     */
    public static boolean isServiceSupportedInCurrentRegion(String serviceAbbreviation) {
        return getCurrentRegion().isServiceSupported(serviceAbbreviation);
    }

    /**
     * Returns a list of the available AWS regions.
     */
    public synchronized static List<Region> getRegions() {
        if (regions == null) {
            init();
        }

        return regions;
    }

    /**
     * Add a service endpoint to the special "local" region, causing the
     * service to show up in the AWS Explorer when the region is set to local
     * and setting the port that the local service is expected to listen on.
     */
    public synchronized static void addLocalService(
            final String serviceName,
            final String serviceId,
            final int port) {

        Region local = getRegion("local");
        if (local == null) {
            throw new IllegalStateException("No local region found!");
        }

        Service service = new Service(serviceName,
                                      serviceId,
                                      "http://localhost:" + port,
                                      null);

        local.getServicesByName().put(serviceName, service);
        local.getServiceEndpoints().put(serviceName, service.getEndpoint());
    }

    /**
     * Returns a list of the regions that support the service given.
     *
     * @see ServiceAbbreviations
     */
    public synchronized static List<Region> getRegionsForService(String serviceAbbreviation) {
        List<Region> regions = new LinkedList<>();
        for (Region r : getRegions()) {
            if (r.isServiceSupported(serviceAbbreviation)) {
                regions.add(r);
            }
        }
        return regions;
    }

    /**
     * Returns the region with the id given, if it exists. Otherwise, returns null.
     */
    public static Region getRegion(String regionId) {
        for (Region r : getRegions()) {
            if (r.getId().equals(regionId)) {
                return r;
            }
        }

        return null;
    }

    /**
     * Returns the default/active region that the user previously selected.
     */
    public static Region getCurrentRegion() {
        IPreferenceStore preferenceStore = AwsToolkitCore.getDefault().getPreferenceStore();
        String defaultRegion = preferenceStore.getString(PreferenceConstants.P_DEFAULT_REGION);

        Region rval = getRegion(defaultRegion);

        if (rval == null) {
            throw new RuntimeException("Unable to determine default region");
        }

        return rval;
    }

    /**
     * Searches through the defined services in all regions looking for a
     * service running on the specified endpoint.
     *
     * @param endpoint
     *            The endpoint of the desired service.
     * @return The service running on the specified endpoint.
     *
     * @throws IllegalArgumentException
     *             if no service is found with the specified endpoint.
     */
    public static Service getServiceByEndpoint(String endpoint) {
        for (Region region : regions) {
            for (Service service : region.getServicesByName().values()) {
                if (service.getEndpoint().equals(endpoint)) {
                    return service;
                }
            }
        }

        throw new IllegalArgumentException("Unknown service endpoint: " + endpoint);
    }

    /**
     * Searches through all known regions to find one with any service at the
     * specified endpoint. If no region is found with a service at that
     * endpoint, an exception is thrown.
     *
     * @param endpoint
     *            The endpoint for any service residing in the desired region.
     * @return The region containing any service running at the specified
     *         endpoint, otherwise an exception is thrown if no region is found
     *         with a service at the specified endpoint.
     */
    public static Region getRegionByEndpoint(String endpoint) {
        // The S3_US_EAST_1_REGIONAL_ENDPOINT region is not configured in the regions.xml file.
        if (S3_US_EAST_1_REGIONAL_ENDPOINT.equals(endpoint)) {
            return RegionUtils.getRegion(Regions.US_EAST_1.getName());
        }
        URL targetEndpointUrl = null;
        try {
            targetEndpointUrl = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Unable to parse service endpoint: " + e.getMessage());
        }

        String targetHost = targetEndpointUrl.getHost();
        for (Region region : getRegions()) {
            for (String serviceEndpoint
                        : region.getServiceEndpoints().values()) {
                try {
                    URL serviceEndpointUrl = new URL(serviceEndpoint);
                    if (serviceEndpointUrl.getHost().equals(targetHost)) {
                        return region;
                    }
                } catch (MalformedURLException e) {
                    AwsToolkitCore.getDefault().reportException("Unable to parse service endpoint: " + serviceEndpoint, e);
                }
            }
        }

        throw new RuntimeException(
                "No region found with any service for endpoint " + endpoint);
    }


    /**
     * Fetches the most recent version of the regions file from the remote
     * source and caches it to the workspace metadata directory, then
     * initializes the static list of regions with it.
     */
    public static synchronized void init() {
        // Use overriding file for testing unlaunched services.
        if (System.getProperty(P_REGIONS_FILE_OVERRIDE) != null) {
            loadRegionsFromOverrideFile();
        // Use the local region override file
        } else if (localRegionOverrideFileExists()) {
            initBundledRegionsOverride();
        // Use the remote ServiceEndpoints.xml file
        } else if (!Boolean.valueOf(System.getProperty(P_USE_LOCAL_REGION_FILE))) {
            initRegionsFromS3();
        }
        // Fall back onto the version we ship with the toolkit
        if (regions == null) {
            initBundledRegions();
        }

        // If the preference store references an unknown starting region,
        // go ahead and set the starting region to any existing region
        IPreferenceStore preferenceStore = AwsToolkitCore.getDefault().getPreferenceStore();
        Region defaultRegion = getRegion(preferenceStore.getString(PreferenceConstants.P_DEFAULT_REGION));
        if (defaultRegion == null) {
            preferenceStore.setValue(PreferenceConstants.P_DEFAULT_REGION, regions.get(0).getId());
        }
    }

    private static void loadRegionsFromOverrideFile() {
        try {
            System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
            File regionsFile =
                new File(System.getProperty(P_REGIONS_FILE_OVERRIDE));
            try (InputStream override = new FileInputStream(regionsFile)) {
                regions = parseRegionMetadata(override);
            }

            try {
                cacheFlags(regionsFile.getParentFile());
            } catch (Exception e) {
                AwsToolkitCore.getDefault().logError(
                        "Couldn't cache flag icons", e);
            }
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logError(
                    "Couldn't load regions override", e);
        }
    }

    private static void initRegionsFromS3() {
        IPath stateLocation = Platform.getStateLocation(AwsToolkitCore
                .getDefault().getBundle());
        File regionsDir = new File(stateLocation.toFile(), "regions");
        File regionsFile = new File(regionsDir, "regions.xml");

        cacheRegionsFile(regionsFile);
        initCachedRegions(regionsFile);
    }

    /**
     * Caches the regions file stored in cloudfront to the destination file
     * given. Tries S3 if cloudfront is unavailable.
     *
     * If the file in s3 is older than the one on disk, does nothing.
     */
    private static void cacheRegionsFile(File regionsFile) {
        Date regionsFileLastModified = new Date(0);
        if (!regionsFile.exists()) {
            regionsFile.getParentFile().mkdirs();
        } else {
            regionsFileLastModified = new Date(regionsFile.lastModified());
        }

        try {
            String url = CLOUDFRONT_DISTRO + REGIONS_METADATA_S3_OBJECT;
            AwsToolkitHttpClient client = HttpClientFactory.create(AwsToolkitCore.getDefault(), url);
            Date remoteFileLastModified = client.getLastModifiedDate(url);
            if (remoteFileLastModified == null || remoteFileLastModified.after(regionsFileLastModified)) {
                fetchFile(url, regionsFile);
            }
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logError(
                    "Failed to cache regions file", e);
        }
    }

    /**
     * Tries to initialize the regions list from the file given. If the file
     * doesn't exist or cannot, it is deleted so that it can be fetched cleanly
     * on the next startup.
     */
    private static void initCachedRegions(File regionsFile) {
        try (InputStream inputStream = new FileInputStream(regionsFile)) {
            regions = parseRegionMetadata(inputStream);
            try {
                cacheFlags(regionsFile.getParentFile());
            } catch (Exception e) {
                AwsToolkitCore.getDefault().logError(
                        "Couldn't cache flag icons", e);
            }
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logError(
                    "Couldn't read regions file", e);
            // Clear out the regions file so that it will get cached again at
            // next startup
            regionsFile.delete();
        }
    }

    /**
     * Failsafe method to initialize the regions list from the list bundled with
     * the plugin, in case it cannot be fetched from the remote source.
     */
    private static void initBundledRegions() {
        try {
            regions = loadRegionsFromLocalRegionFile();
            registerRegionFlagsFromBundle(regions);
        } catch (IOException e) {
            // Do nothing, the regions remains null in this case.
        }
    }

    private static void initBundledRegionsOverride() {
        try {
            regions = loadRegionsFromLocalRegionOverrideFile();
            registerRegionFlagsFromBundle(regions);
        } catch (IOException e) {
            // Do nothing, the regions remains null in this case.
        }
    }

    private static final void registerRegionFlagsFromBundle(List<Region> regions) {
        if (regions == null) {
            return;
        }
        for (Region r : regions) {
            if (r == LocalRegion.INSTANCE) {
                // No flag to load for the local region.
                continue;
            }

            AwsToolkitCore
                .getDefault()
                .getImageRegistry()
                .put(AwsToolkitCore.IMAGE_FLAG_PREFIX + r.getId(),
                    ImageDescriptor.createFromFile(RegionUtils.class,
                                                   "/icons/" + r.getFlagIconPath()));
        }
    }

    private static final RegionMetadataParser PARSER =
        new RegionMetadataParser();

    /**
     * Parses a list of regions from the given input stream. Adds in the
     * special "local" region.
     */
    private static List<Region> parseRegionMetadata(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        List<Region> list = PARSER.parseRegionMetadata(inputStream);
        list.add(LocalRegion.INSTANCE);
        replaceS3GlobalEndpointWithRegional(list);
        return list;
    }

    /*
     * A hacky way to replace the evil S3 global endpoint with the regional one.
     */
    private static void replaceS3GlobalEndpointWithRegional(List<Region> regions) {
        for (Region region : regions) {
            if (Regions.US_EAST_1.getName().equals(region.getId())) {
                region.getServiceEndpoints().put(ServiceAbbreviations.S3, S3_US_EAST_1_REGIONAL_ENDPOINT);
            }
        }
    }

    /**
     * Caches flag icons as necessary, also registering images for them
     */
    private static void cacheFlags(File regionsDir)
            throws ClientProtocolException, IOException {
        if (!regionsDir.exists()) {
            return;
        }

        for (Region r : regions) {
            if (r == LocalRegion.INSTANCE) {
                // Local region has no flag to initialize.
                continue;
            }

            File icon = new File(regionsDir, r.getFlagIconPath());
            if (icon.exists() == false) {
                icon.getParentFile().mkdirs();
                String iconUrl = CLOUDFRONT_DISTRO + r.getFlagIconPath();
                fetchFile(iconUrl, icon);
            }

            AwsToolkitCore
                .getDefault()
                .getImageRegistry()
                .put(AwsToolkitCore.IMAGE_FLAG_PREFIX + r.getId(),
                    ImageDescriptor.createFromURL(
                        icon.getAbsoluteFile().toURI().toURL()));
        }
    }

    /**
     * Fetches a file from the URL given and writes it to the destination given.
     */
    private static void fetchFile(String url, File destinationFile)
            throws IOException, ClientProtocolException, FileNotFoundException {

        AwsToolkitHttpClient httpClient = HttpClientFactory.create(
                AwsToolkitCore.getDefault(), url);
        try (FileOutputStream output = new FileOutputStream(destinationFile)) {
            httpClient.outputEntityContent(url, output);
        }
    }

    /**
     * Load regions from remote CloudFront URL.
     */
    public static List<Region> loadRegionsFromCloudFront() throws IOException {
        String url = CLOUDFRONT_DISTRO + REGIONS_METADATA_S3_OBJECT;
        AwsToolkitHttpClient httpClient = HttpClientFactory.create(AwsToolkitCore.getDefault(), url);
        try (InputStream inputStream =
                httpClient.getEntityContent(url)) {
            return parseRegionMetadata(inputStream);
        }
    }

    /**
     * Load regions from local file.
     */
    private static List<Region> loadRegionsFromLocalFile(String localFileName) throws IOException {
        ClassLoader classLoader = RegionUtils.class.getClassLoader();
        try (InputStream inputStream =
            classLoader.getResourceAsStream(localFileName)) {
            return parseRegionMetadata(inputStream);
        }
    }

    private static boolean localRegionOverrideFileExists() {
        ClassLoader classLoader = RegionUtils.class.getClassLoader();
        return classLoader.getResource(LOCAL_REGION_FILE_OVERRIDE) != null;
    }

    private static List<Region> loadRegionsFromLocalRegionOverrideFile() throws IOException {
        return loadRegionsFromLocalFile(LOCAL_REGION_FILE_OVERRIDE);
    }

    public static List<Region> loadRegionsFromLocalRegionFile() throws IOException {
        return loadRegionsFromLocalFile(LOCAL_REGION_FILE);
    }
}
