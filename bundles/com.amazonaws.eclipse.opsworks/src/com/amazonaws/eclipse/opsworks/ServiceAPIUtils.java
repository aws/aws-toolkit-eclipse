package com.amazonaws.eclipse.opsworks;

import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.DescribeAppsRequest;
import com.amazonaws.services.opsworks.model.DescribeInstancesRequest;
import com.amazonaws.services.opsworks.model.DescribeLayersRequest;
import com.amazonaws.services.opsworks.model.DescribeStacksRequest;
import com.amazonaws.services.opsworks.model.Instance;
import com.amazonaws.services.opsworks.model.Layer;
import com.amazonaws.services.opsworks.model.Stack;

public class ServiceAPIUtils {

    public static List<Stack> getAllStacks(AWSOpsWorks client) {
        return client.describeStacks(new DescribeStacksRequest())
                .getStacks();
    }

    public static List<Layer> getAllLayersInStack(AWSOpsWorks client, String stackId) {
        return client.describeLayers(
                    new DescribeLayersRequest().withStackId(stackId))
                .getLayers();
    }

    public static List<Instance> getAllInstancesInStack(AWSOpsWorks client, String stackId) {
        return client.describeInstances(
                    new DescribeInstancesRequest().withStackId(stackId))
                .getInstances();
    }

    public static List<Instance> getAllInstancesInLayer(AWSOpsWorks client, String layerId) {
        return client.describeInstances(
                    new DescribeInstancesRequest().withLayerId(layerId))
                .getInstances();
    }

    public static List<App> getAllAppsInStack(AWSOpsWorks client, String stackId) {
        return client.describeApps(
                    new DescribeAppsRequest().withStackId(stackId))
                .getApps();
    }

    public static List<App> getAllJavaAppsInStack(AWSOpsWorks client, String stackId) {
        List<App> allJavaApps = new LinkedList<>();
        for (App app : getAllAppsInStack(client, stackId)) {
            if ("java".equalsIgnoreCase(app.getType())) {
                allJavaApps.add(app);
            }
        }
        return allJavaApps;
    }

}
