package com.amazonaws.eclipse.lambda.project.metadata;

import java.util.Properties;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;

public class LambdaFunctionProjectMetadata {

    private static final String P_LAST_DEPLOYMENT_ENDPOINT = "lastDeploymentEndpoint";
    private static final String P_LAST_DEPLOYMENT_FUNCTION_NAME = "lastDeploymentFunctionName";
    private static final String P_LAST_DEPLOYMENT_BUCKET_NAME = "lastDeploymentBucketName";
    private static final String P_LAST_INVOKE_INPUT = "lastInvokeInput";

    private String lastDeploymentEndpoint;
    private String lastDeploymentFunctionName;
    private String lastDeploymentBucketName;
    private String lastInvokeInput;

    public String getLastDeploymentEndpoint() {
        return lastDeploymentEndpoint;
    }

    /**
     * @return null if no lambda service endpoint matches the endpoint persisted
     *         in this metadata
     */
    public Region getLastDeploymentRegion() {
        for (Region region : RegionUtils
                .getRegionsForService(ServiceAbbreviations.LAMBDA)) {
            if (region.getServiceEndpoint(ServiceAbbreviations.LAMBDA).equals(
                    lastDeploymentEndpoint)) {
                return region;
            }
        }
        return null;
    }

    public void setLastDeploymentEndpoint(String lastDeploymentEndpoint) {
        this.lastDeploymentEndpoint = lastDeploymentEndpoint;
    }

    public String getLastDeploymentFunctionName() {
        return lastDeploymentFunctionName;
    }

    public void setLastDeploymentFunctionName(String lastDeploymentFunctionName) {
        this.lastDeploymentFunctionName = lastDeploymentFunctionName;
    }

    public String getLastDeploymentBucketName() {
        return lastDeploymentBucketName;
    }

    public void setLastDeploymentBucketName(String lastDeploymentBucketName) {
        this.lastDeploymentBucketName = lastDeploymentBucketName;
    }

    public String getLastInvokeInput() {
        return lastInvokeInput;
    }

    public void setLastInvokeInput(String lastInvokeInput) {
        this.lastInvokeInput = lastInvokeInput;
    }

    public boolean isValid() {
        return isNotEmpty(lastDeploymentEndpoint)
                && isNotEmpty(lastDeploymentFunctionName)
                && isNotEmpty(lastDeploymentBucketName);
    }

    public Properties toProperties() {

        Properties props = new Properties();

        if (lastDeploymentEndpoint != null) {
            props.setProperty(P_LAST_DEPLOYMENT_ENDPOINT, lastDeploymentEndpoint);
        }
        if (lastDeploymentFunctionName != null) {
            props.setProperty(P_LAST_DEPLOYMENT_FUNCTION_NAME, lastDeploymentFunctionName);
        }
        if (lastDeploymentBucketName != null) {
            props.setProperty(P_LAST_DEPLOYMENT_BUCKET_NAME, lastDeploymentBucketName);
        }
        if (lastInvokeInput != null) {
            props.setProperty(P_LAST_INVOKE_INPUT, lastInvokeInput);
        }

        return props;
    }

    public static LambdaFunctionProjectMetadata fromProperties(Properties props) {

        LambdaFunctionProjectMetadata md = new LambdaFunctionProjectMetadata();

        md.setLastDeploymentEndpoint(props.getProperty(P_LAST_DEPLOYMENT_ENDPOINT));
        md.setLastDeploymentFunctionName(props.getProperty(P_LAST_DEPLOYMENT_FUNCTION_NAME));
        md.setLastDeploymentBucketName(props.getProperty(P_LAST_DEPLOYMENT_BUCKET_NAME));
        md.setLastInvokeInput(props.getProperty(P_LAST_INVOKE_INPUT));

        return md;
    }

    private static boolean isNotEmpty(String arg) {
        return arg != null && !arg.isEmpty();
    }

}
