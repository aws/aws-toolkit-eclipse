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
package com.amazonaws.eclipse.codestar.model;

import com.amazonaws.eclipse.core.model.GitCredentialsDataModel;

public class CodeStarProjectCheckoutWizardDataModel {

    private String accountId;
    private String projectName;
    private String projectId;
    private String projectRegionId;
    private String repoName;
    private String repoHttpUrl;

    private final GitCredentialsDataModel gitCredentialsDataModel = new GitCredentialsDataModel();

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectRegionId() {
        return projectRegionId;
    }

    public void setProjectRegionId(String projectRegionId) {
        this.projectRegionId = projectRegionId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String RepoName) {
        this.repoName = RepoName;
    }

    public String getRepoHttpUrl() {
        return repoHttpUrl;
    }

    public void setRepoHttpUrl(String repoHttpUrl) {
        this.repoHttpUrl = repoHttpUrl;
    }

    public GitCredentialsDataModel getGitCredentialsDataModel() {
        return gitCredentialsDataModel;
    }
}
