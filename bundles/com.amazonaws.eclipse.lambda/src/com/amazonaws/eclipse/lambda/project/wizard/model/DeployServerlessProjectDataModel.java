/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import com.amazonaws.eclipse.cloudformation.model.ParametersDataModel;
import com.amazonaws.eclipse.core.model.MultipleSelectionListDataModel;
import com.amazonaws.eclipse.core.model.RegionDataModel;
import com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel;
import com.amazonaws.eclipse.lambda.model.SelectOrInputStackDataModel;
import com.amazonaws.eclipse.lambda.project.metadata.ServerlessProjectMetadata;
import com.amazonaws.services.cloudformation.model.Capability;

public class DeployServerlessProjectDataModel {

    private final RegionDataModel regionDataModel = new RegionDataModel();
    private final SelectOrCreateBucketDataModel bucketDataModel = new SelectOrCreateBucketDataModel();
    private final SelectOrInputStackDataModel stackDataModel = new SelectOrInputStackDataModel();
    private final ParametersDataModel parametersDataModel = new ParametersDataModel();
    private final MultipleSelectionListDataModel<Capability> capabilitiesDataModel = new MultipleSelectionListDataModel<>();

    private final IProject project;
    private final String projectName;
    private final Set<String> handlerClasses;

    private ServerlessProjectMetadata metadata = new ServerlessProjectMetadata();

    private String lambdaFunctionJarFileKeyName;
    private File updatedServerlessTemplate;

    public DeployServerlessProjectDataModel(IProject project, Set<String> handlerClasses) {
        this.project = project;
        this.projectName = project.getName();
        this.stackDataModel.setDefaultStackNamePrefix(projectName);
        this.handlerClasses = handlerClasses;
    }

    // Add listeners for property changing.
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        bucketDataModel.addPropertyChangeListener(listener);
        stackDataModel.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        bucketDataModel.removePropertyChangeListener(listener);
        bucketDataModel.removePropertyChangeListener(listener);
    }

    public RegionDataModel getRegionDataModel() {
        return this.regionDataModel;
    }

    public SelectOrCreateBucketDataModel getBucketDataModel() {
        return bucketDataModel;
    }

    public SelectOrInputStackDataModel getStackDataModel() {
        return stackDataModel;
    }

    public MultipleSelectionListDataModel<Capability> getCapabilitiesDataModel() {
        return capabilitiesDataModel;
    }

    public ParametersDataModel getParametersDataModel() {
        return parametersDataModel;
    }

    public IProject getProject() {
        return project;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getLambdaFunctionJarFileKeyName() {
        return lambdaFunctionJarFileKeyName;
    }

    public void setLambdaFunctionJarFileKeyName(String lambdaFunctionJarFileKeyName) {
        this.lambdaFunctionJarFileKeyName = lambdaFunctionJarFileKeyName;
    }

    public File getUpdatedServerlessTemplate() {
        return updatedServerlessTemplate;
    }

    public void setUpdatedServerlessTemplate(File updatedServerlessTemplate) {
        this.updatedServerlessTemplate = updatedServerlessTemplate;
    }

    public Set<String> getHandlerClasses() {
        return handlerClasses;
    }

    public void setMetadata(ServerlessProjectMetadata metadata) {
        this.metadata = metadata == null ? new ServerlessProjectMetadata() : metadata;
    }

    /**
     * The returned metadata is nonnull
     */
    public ServerlessProjectMetadata getMetadata() {
        if (metadata == null) {
            metadata = new ServerlessProjectMetadata();
        }
        return metadata;
    }
}
