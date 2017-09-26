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
package com.amazonaws.eclipse.lambda.upload.wizard.model;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;

import com.amazonaws.eclipse.core.model.RegionDataModel;
import com.amazonaws.eclipse.core.model.SelectOrCreateBucketDataModel;
import com.amazonaws.eclipse.core.model.SelectOrCreateKmsKeyDataModel;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.eclipse.lambda.model.SelectOrCreateBasicLambdaRoleDataModel;
import com.amazonaws.eclipse.lambda.model.SelectOrInputFunctionAliasDataModel;
import com.amazonaws.eclipse.lambda.model.SelectOrInputFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;

public class UploadFunctionWizardDataModel {
    public static final String P_HANDLER = "handler";

    private final IProject project;
    private final IJavaElement selectedJavaElement;
    private final List<String> requestHandlerImplementerClasses;
    private final LambdaFunctionProjectMetadata projectMetadataBeforeUpload;

    /* Page 1 */
    private String handler;
    private final RegionDataModel regionDataModel = new RegionDataModel();
    private final SelectOrInputFunctionDataModel functionDataModel = new SelectOrInputFunctionDataModel();

    /* Page 2 */
    private final SelectOrCreateBasicLambdaRoleDataModel lambdaRoleDataModel = new SelectOrCreateBasicLambdaRoleDataModel();
    private final SelectOrInputFunctionAliasDataModel functionAliasDataModel = new SelectOrInputFunctionAliasDataModel();
    private final SelectOrCreateBucketDataModel s3BucketDataModel = new SelectOrCreateBucketDataModel();
    private final SelectOrCreateKmsKeyDataModel kmsKeyDataModel = new SelectOrCreateKmsKeyDataModel();
    private final FunctionConfigPageDataModel functionConfigPageDataModel = new FunctionConfigPageDataModel();;

    public CreateFunctionRequest toCreateFunctionRequest() {

        return new CreateFunctionRequest()
                .withFunctionName(functionDataModel.getFunctionName())
                .withRuntime(ServiceApiUtils.JAVA_8)
                .withDescription(functionConfigPageDataModel.getDescription())
                .withHandler(getHandler())
                .withRole(getLambdaRoleDataModel().getExistingResource().getArn())
                .withMemorySize(functionConfigPageDataModel.getMemory().intValue())
                .withTimeout(functionConfigPageDataModel.getTimeout().intValue())
                .withPublish(functionConfigPageDataModel.isPublishNewVersion());
    }

    public UpdateFunctionConfigurationRequest toUpdateFunctionConfigRequest() {
        return new UpdateFunctionConfigurationRequest()
                .withFunctionName(functionDataModel.getFunctionName())
                .withDescription(functionConfigPageDataModel.getDescription())
                .withHandler(getHandler())
                .withRole(getLambdaRoleDataModel().getExistingResource().getArn())
                .withMemorySize(functionConfigPageDataModel.getMemory().intValue())
                .withTimeout(functionConfigPageDataModel.getTimeout().intValue());
    }

    /**
     * @param project
     *            the project being uploaded
     * @param requestHandlerImplementerClasses
     *            a non-empty list of FQCNs of the classes within the project
     *            that implement the RequestHandler interface.
     * @param projectMetadataBeforeUpload
     *            the existing persistent metadata for this project
     */
    public UploadFunctionWizardDataModel(IProject project,
            IJavaElement selectedJavaElement,
            List<String> requestHandlerImplementerClasses,
            LambdaFunctionProjectMetadata projectMetadataBeforeUpload) {

        this.project = project;
        this.selectedJavaElement = selectedJavaElement;
        this.projectMetadataBeforeUpload = projectMetadataBeforeUpload == null ?
                new LambdaFunctionProjectMetadata() : projectMetadataBeforeUpload;

        if (requestHandlerImplementerClasses.isEmpty()) {
            throw new IllegalArgumentException(
                    "requestHandlerImplementerClasses must not be empty.");
        }
        this.requestHandlerImplementerClasses = Collections
                .unmodifiableList(requestHandlerImplementerClasses);
    }

    public IProject getProject() {
        return project;
    }

    public List<String> getRequestHandlerImplementerClasses() {
        return requestHandlerImplementerClasses;
    }

    public LambdaFunctionProjectMetadata getProjectMetadataBeforeUpload() {
        return projectMetadataBeforeUpload;
    }

    public FunctionConfigPageDataModel getFunctionConfigPageDataModel() {
        return functionConfigPageDataModel;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public SelectOrCreateBasicLambdaRoleDataModel getLambdaRoleDataModel() {
        return lambdaRoleDataModel;
    }

    public SelectOrInputFunctionAliasDataModel getFunctionAliasDataModel() {
        return functionAliasDataModel;
    }

    public RegionDataModel getRegionDataModel() {
        return regionDataModel;
    }

    public SelectOrInputFunctionDataModel getFunctionDataModel() {
        return functionDataModel;
    }

    public SelectOrCreateBucketDataModel getS3BucketDataModel() {
        return s3BucketDataModel;
    }

    public SelectOrCreateKmsKeyDataModel getKmsKeyDataModel() {
        return kmsKeyDataModel;
    }
}
