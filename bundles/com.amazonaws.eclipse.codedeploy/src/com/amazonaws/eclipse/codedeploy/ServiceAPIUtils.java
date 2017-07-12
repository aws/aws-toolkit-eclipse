package com.amazonaws.eclipse.codedeploy;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.model.ApplicationInfo;
import com.amazonaws.services.codedeploy.model.BatchGetApplicationsRequest;
import com.amazonaws.services.codedeploy.model.BatchGetApplicationsResult;
import com.amazonaws.services.codedeploy.model.BatchGetDeploymentsRequest;
import com.amazonaws.services.codedeploy.model.BatchGetDeploymentsResult;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentInstanceRequest;
import com.amazonaws.services.codedeploy.model.InstanceSummary;
import com.amazonaws.services.codedeploy.model.LifecycleEvent;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentsResult;

public class ServiceAPIUtils {

    public static List<String> getAllApplicationNames(AmazonCodeDeploy client) {

        List<String> allAppNames = new LinkedList<>();
        String nextToken = null;

        do {
           ListApplicationsResult result = client.listApplications(
                    new ListApplicationsRequest()
                        .withNextToken(nextToken));

           List<String> appNames = result.getApplications();
           if (appNames != null) {
               allAppNames.addAll(appNames);
           }

           nextToken = result.getNextToken();

        } while (nextToken != null);

        return allAppNames;
    }

    public static List<ApplicationInfo> getAllApplicationInfos(AmazonCodeDeploy client) {

        List<ApplicationInfo> allAppInfos = new LinkedList<>();

        List<String> allAppNames = getAllApplicationNames(client);
        if (allAppNames != null && !allAppNames.isEmpty()) {
            BatchGetApplicationsResult result = client.batchGetApplications(
                    new BatchGetApplicationsRequest()
                        .withApplicationNames(allAppNames));

            if (result.getApplicationsInfo() != null) {
                allAppInfos.addAll(result.getApplicationsInfo());
            }
        }

        return allAppInfos;
    }

    public static List<String> getAllDeploymentGroupNames(
            AmazonCodeDeploy client, String applicationName) {

        List<String> allDeployGroupNames = new LinkedList<>();
        String nextToken = null;

        do {
            ListDeploymentGroupsResult result = client.listDeploymentGroups(
                    new ListDeploymentGroupsRequest()
                        .withApplicationName(applicationName)
                        .withNextToken(nextToken));

           List<String> deployGroupNames = result.getDeploymentGroups();
           if (deployGroupNames != null) {
               allDeployGroupNames.addAll(deployGroupNames);
           }

           nextToken = result.getNextToken();

        } while (nextToken != null);

        return allDeployGroupNames;
    }

    public static List<DeploymentGroupInfo> getAllDeploymentGroupInfos(
            AmazonCodeDeploy client, String applicationName) {

        List<DeploymentGroupInfo> allDeployGroupInfos = new LinkedList<>();

        List<String> allDeployGroupNames = getAllDeploymentGroupNames(client, applicationName);
        for (String deployGroupName : allDeployGroupNames) {
            DeploymentGroupInfo group = client.getDeploymentGroup(
                    new GetDeploymentGroupRequest()
                            .withApplicationName(applicationName)
                            .withDeploymentGroupName(deployGroupName))
                    .getDeploymentGroupInfo();
            allDeployGroupInfos.add(group);
        }

        return allDeployGroupInfos;
    }

    public static List<String> getAllDeploymentConfigNames(AmazonCodeDeploy client) {

        List<String> allConfigNames = new LinkedList<>();
        String nextToken = null;

        do {
            ListDeploymentConfigsResult result = client.listDeploymentConfigs(
                    new ListDeploymentConfigsRequest()
                        .withNextToken(nextToken));

           List<String> configNames = result.getDeploymentConfigsList();
           if (configNames != null) {
               allConfigNames.addAll(configNames);
           }

           nextToken = result.getNextToken();

        } while (nextToken != null);

        return allConfigNames;
    }

    public static List<DeploymentInfo> getAllDeployments(
            AmazonCodeDeploy client, String applicationName,
            String deploymentGroupName) {

        List<DeploymentInfo> allDeploymentInfos = new LinkedList<>();
        String nextToken = null;

        do {
            ListDeploymentsResult result = client.listDeployments(
                    new ListDeploymentsRequest()
                            .withApplicationName(applicationName)
                            .withDeploymentGroupName(deploymentGroupName)
                            .withNextToken(nextToken)
                            );

            List<String> deploymentIds = result.getDeployments();

            if (deploymentIds != null) {
                BatchGetDeploymentsResult batchGetResult = client.batchGetDeployments(
                        new BatchGetDeploymentsRequest()
                                .withDeploymentIds(deploymentIds));

                if (batchGetResult.getDeploymentsInfo() != null) {
                    allDeploymentInfos.addAll(batchGetResult.getDeploymentsInfo());
                }
            }

            nextToken = result.getNextToken();

        } while (nextToken != null);

        return allDeploymentInfos;
    }

    public static DeploymentInfo getMostRecentDeployment(
            AmazonCodeDeploy client, String applicationName,
            String deploymentGroupName) {

        List<String> deploymentIds = client.listDeployments(
                new ListDeploymentsRequest()
                        .withApplicationName(applicationName)
                        .withDeploymentGroupName(deploymentGroupName)
                        ).getDeployments();

        if (deploymentIds == null || deploymentIds.isEmpty()) {
            return null;
        }

        List<DeploymentInfo> latestDeploymentInfosPage = client.batchGetDeployments(
                new BatchGetDeploymentsRequest()
                        .withDeploymentIds(deploymentIds)
                        ).getDeploymentsInfo();

        if (latestDeploymentInfosPage == null || latestDeploymentInfosPage.isEmpty()) {
            return null;
        }

        // Sort by creation data
        Collections.sort(latestDeploymentInfosPage, new Comparator<DeploymentInfo>() {
            @Override
            public int compare(DeploymentInfo a, DeploymentInfo b) {
                int a_to_b = a.getCreateTime().compareTo(b.getCreateTime());
                // In descending order
                return - a_to_b;
            }
        });

        return latestDeploymentInfosPage.get(0);

    }

    public static List<InstanceSummary> getAllDeploymentInstances(
            AmazonCodeDeploy client, String deploymentId) {

        List<InstanceSummary> allDeploymentInstances = new LinkedList<>();
        String nextToken = null;

        do {
            ListDeploymentInstancesResult result = client.listDeploymentInstances(
                    new ListDeploymentInstancesRequest()
                            .withDeploymentId(deploymentId)
                            .withNextToken(nextToken)
                            );

            List<String> instanceIds = result.getInstancesList();

            if (instanceIds != null) {
                for (String instanceId : instanceIds) {
                    allDeploymentInstances.add(client.getDeploymentInstance(
                            new GetDeploymentInstanceRequest()
                                    .withDeploymentId(deploymentId)
                                    .withInstanceId(instanceId)
                                    )
                            .getInstanceSummary());
                }
            }

            nextToken = result.getNextToken();

        } while (nextToken != null);

        return allDeploymentInstances;
    }

    public static LifecycleEvent findLifecycleEventByEventName(InstanceSummary instanceSummary, String eventName) {
        for (LifecycleEvent event : instanceSummary.getLifecycleEvents()) {
            if (event.getLifecycleEventName().equals(eventName)) {
                return event;
            }
        }

        throw new RuntimeException(eventName + " event not found in the InstanceSummary.");
    }
}
