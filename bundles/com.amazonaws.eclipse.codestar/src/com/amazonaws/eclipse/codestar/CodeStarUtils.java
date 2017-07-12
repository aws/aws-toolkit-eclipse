/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codestar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.codestar.arn.ARN;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.model.GetRepositoryRequest;
import com.amazonaws.services.codecommit.model.RepositoryMetadata;
import com.amazonaws.services.codestar.AWSCodeStar;
import com.amazonaws.services.codestar.model.DescribeProjectRequest;
import com.amazonaws.services.codestar.model.DescribeProjectResult;
import com.amazonaws.services.codestar.model.ListProjectsRequest;
import com.amazonaws.services.codestar.model.ListResourcesRequest;
import com.amazonaws.services.codestar.model.ProjectSummary;
import com.amazonaws.services.codestar.model.Resource;

public class CodeStarUtils {

    public static Map<String, DescribeProjectResult> getCodeStarProjects(String accountId, String regionId) {
        Map<String, DescribeProjectResult> projectMap = new HashMap<>();
        AWSCodeStar client = getCodeStarClient(accountId, regionId);
        List<ProjectSummary> projectList = client.listProjects(new ListProjectsRequest()).getProjects();
        for (ProjectSummary project : projectList) {
            projectMap.put(project.getProjectId(),
                    client.describeProject(new DescribeProjectRequest().withId(project.getProjectId())));
        }
        return projectMap;
    }

    /*
     * Get the AWS CodeCommit repositories associated with the given AWS CodeStar project.
     */
    public static List<RepositoryMetadata> getCodeCommitRepositories(String accountId, String regionId, String codestarProjectId) {
        AWSCodeStar codeStarClient = getCodeStarClient(accountId, regionId);
        AWSCodeCommit codeCommitClient = getCodeCommitClient(accountId, regionId);
        List<Resource> resources = codeStarClient.listResources(new ListResourcesRequest().withProjectId(codestarProjectId)).getResources();
        List<String> codeCommitRepoNames = getCodeCommitRepoNames(resources);
        List<RepositoryMetadata> repositoryMetadatas = new ArrayList<>();
        for (String repoName : codeCommitRepoNames) {
            repositoryMetadatas.add(codeCommitClient.getRepository(new GetRepositoryRequest()
                .withRepositoryName(repoName)).getRepositoryMetadata());
        }
        return repositoryMetadatas;
    }

    /*
     * Return a list of AWS CodeCommit repository names from a given list of AWS CodeStar resources
     * that is associated with one AWS CodeStar project. Return an empty list if no AWS CodeCommit
     * repository is found.
     */
    private static List<String> getCodeCommitRepoNames(List<Resource> resources) {
        List<String> repoNames = new ArrayList<>();
        for (Resource resource : resources) {
            ARN resourceArn = ARN.fromSafeString(resource.getId());
            if ("codecommit".equals(resourceArn.getVendor())) {
                repoNames.add(resourceArn.getRelativeId());
            }
        }
        return repoNames;
    }

    public static AWSCodeStar getCodeStarClient(String accountId, String regionId) {
        return AwsToolkitCore.getDefault().getClientFactory(accountId).getCodeStarClientByRegion(regionId);
    }

    public static AWSCodeCommit getCodeCommitClient(String accountId, String regionId) {
        String endpoint = RegionUtils.getRegion(regionId).getServiceEndpoint(ServiceAbbreviations.CODECOMMIT);
        return AwsToolkitCore.getDefault().getClientFactory(accountId).getCodeCommitClientByEndpoint(endpoint);
    }
}
