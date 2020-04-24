package com.amazonaws.eclipse.core.regions;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class RegionUtilsTest {

    private static final Pattern[] PATTERNS = {
        // global region endpoint, ex. s3.amazonaws.com
        Pattern.compile("^(http|https)://\\w+(\\.us-gov)?.amazonaws.com(/)?$"),
        // s3 irregular regional endpoints
        Pattern.compile("^(http|https)://s3-(us|eu|ap|sa)-(gov-)?(east|west|south|north|central|northeast|southeast)-(1|2).amazonaws.com(/)?$"),
        // regular region endpoints
        Pattern.compile("^(http|https)://\\w+\\.(\\w+\\.)?(ca|us|eu|ap|sa|me|af)-(gov-)?(east|west|south|north|central|northeast|southeast)-(1|2|3)\\.amazonaws\\.com(/)?$"),
        // China region endpoints, currently we only have cn-north-1 region
        Pattern.compile("^(http|https)://\\w+\\.(\\w+\\.)?cn-(north|northwest)-1.amazonaws.com.cn(/)?$"),
        // us-gov region endpoints
        Pattern.compile("^(http|https)://\\w+\\.us-gov(-west-1)?.amazonaws.com(/)?$")
    };

    @Test
    public void testRemoteRegionFile() throws IOException {
        List<Region> regions = RegionUtils.loadRegionsFromCloudFront();
        assertRegionEndpointsValid(regions);
    }

    @Test
    public void testLocalRegionFile() throws IOException {
        List<Region> regions = RegionUtils.loadRegionsFromLocalRegionFile();
        assertRegionEndpointsValid(regions);
    }

    private void assertRegionEndpointsValid(List<Region> regions) {
        for (Region region : regions) {
            if ("local".equals(region.getId())) {
                continue;
            }
            for (String endpoint : region.getServiceEndpoints().values()) {
                if (RegionUtils.S3_US_EAST_1_REGIONAL_ENDPOINT.equals(endpoint)) {
                    continue;
                }
                assertEndpointValid(endpoint);
            }
        }
    }

    private void assertEndpointValid(String endpoint) {
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(endpoint).matches()) {
                return;
            }
        }
        Assert.fail("Endpoint: " + endpoint + " doesn't follow any endpoint patterns.");
    }

}
