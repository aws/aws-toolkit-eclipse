/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.sdk.ui.model;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.model.ProjectNameDataModel;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.telemetry.MetricsDataModel;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.sdk.ui.SdkSample;

public class NewAwsJavaProjectWizardDataModel {
    private String actionSource;
    private final ProjectNameDataModel projectNameDataModel = new ProjectNameDataModel();
    private final MavenConfigurationDataModel mavenConfigurationDataModel = new MavenConfigurationDataModel();
    private AccountInfo accountInfo;
    private final List<SdkSample> sdkSamples = new ArrayList<>();
    private String endResult;
    private Long actionExecutionTimeMillis;

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }
    public void setAccountInfo(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;
    }
    public List<SdkSample> getSdkSamples() {
        return sdkSamples;
    }

    public MavenConfigurationDataModel getMavenConfigurationDataModel() {
        return mavenConfigurationDataModel;
    }

    public ProjectNameDataModel getProjectNameDataModel() {
        return projectNameDataModel;
    }

    public String getActionSource() {
        return actionSource;
    }

    public void setActionSource(String actionSource) {
        this.actionSource = actionSource;
    }

    public void actionFailed() {
        endResult = AwsAction.FAILED;
    }

    public void actionSucceeded() {
        endResult = AwsAction.SUCCEEDED;
    }

    public void actionCanceled() {
        endResult = AwsAction.CANCELED;
    }

    public void publishMetrics() {
        MetricsDataModel metricsDataModel = new MetricsDataModel(AwsToolkitMetricType.AWS_NEW_JAVA_PROJECT_WIZARD);
        metricsDataModel.addAttribute("ActionSource", actionSource);
        for (SdkSample sample: sdkSamples) {
            metricsDataModel.addBooleanMetric(sample.getName(), true);
        }
        metricsDataModel.addAttribute(AwsAction.END_RESULT, endResult);
        if (actionExecutionTimeMillis != null) {
            metricsDataModel.addMetric("ExecutionTimeMillis", (double) actionExecutionTimeMillis);
        }
        metricsDataModel.publishEvent();
    }

    public void setActionExecutionTimeMillis(Long actionExecutionTimeMillis) {
        this.actionExecutionTimeMillis = actionExecutionTimeMillis;
    }
}