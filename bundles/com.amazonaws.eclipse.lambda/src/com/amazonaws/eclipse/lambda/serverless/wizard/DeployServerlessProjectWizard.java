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
package com.amazonaws.eclipse.lambda.serverless.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.plugin.AbstractAwsJobWizard;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.cloudformation.OpenStackEditorAction;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;
import com.amazonaws.eclipse.lambda.project.metadata.ServerlessProjectMetadata;
import com.amazonaws.eclipse.lambda.project.wizard.model.DeployServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.serverless.ui.DeployServerlessProjectPage;
import com.amazonaws.eclipse.lambda.serverless.ui.DeployServerlessProjectPageTwo;
import com.amazonaws.eclipse.lambda.upload.wizard.util.FunctionJarExportHelper;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.ChangeSetStatus;
import com.amazonaws.services.cloudformation.model.ChangeSetType;
import com.amazonaws.services.cloudformation.model.CreateChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DeleteChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeChangeSetResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ExecuteChangeSetRequest;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.StringUtils;

public class DeployServerlessProjectWizard extends AbstractAwsJobWizard {

    // AWS CloudFormation stack statuses for updating the ChangeSet.
    private static final Set<String> STATUSES_FOR_UPDATE = new HashSet<>(Arrays.asList(
            StackStatus.CREATE_COMPLETE.toString(),
            StackStatus.UPDATE_COMPLETE.toString(),
            StackStatus.UPDATE_ROLLBACK_COMPLETE.toString()));

    // AWS CloudFormation stack statuses for creating a new ChangeSet.
    private static final Set<String> STATUSES_FOR_CREATE = new HashSet<>(Arrays.asList(
            StackStatus.REVIEW_IN_PROGRESS.toString(),
            StackStatus.DELETE_COMPLETE.toString()));

    // AWS CloudFormation stack statuses for waiting and deleting the stack first, then creating a new ChangeSet.
    private static final Set<String> STATUSES_FOR_DELETE = new HashSet<>(Arrays.asList(
            StackStatus.ROLLBACK_IN_PROGRESS.toString(),
            StackStatus.ROLLBACK_COMPLETE.toString(),
            StackStatus.DELETE_IN_PROGRESS.toString()));

    private DeployServerlessProjectDataModel dataModel;

    public DeployServerlessProjectWizard(IProject project, Set<String> handlerClasses) {
        super("Deploy Serverless application to AWS");
        this.dataModel = new DeployServerlessProjectDataModel(project, handlerClasses);
        initDataModel();
    }

    @Override
    public void addPages() {
        addPage(new DeployServerlessProjectPage(dataModel));
        addPage(new DeployServerlessProjectPageTwo(dataModel));
    }

    @Override
    public IStatus doFinish(IProgressMonitor monitor) {
        monitor.beginTask("Deploying Serverless template to AWS CloudFormation.", 100);
        try {
            if (deployServerlessTemplate(monitor, 100)) {
                new OpenStackEditorAction(
                    dataModel.getStackDataModel().getStackName(), dataModel.getRegionDataModel().getRegion(), true).run();
            } else {
                return Status.CANCEL_STATUS;
            }
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Failed to deploy serverless project to AWS CloudFormation.", e);
            LambdaAnalytics.trackDeployServerlessProjectFailed();
            return new Status(Status.ERROR, LambdaPlugin.PLUGIN_ID,
                    "Failed to deploy serverless project to AWS CloudFormation.", e);
        }
        monitor.done();
        LambdaAnalytics.trackDeployServerlessProjectSucceeded();
        return Status.OK_STATUS;
    }

    @Override
    protected String getJobTitle() {
        return "Deploying Serverless template to AWS CloudFormation.";
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackServerlessProjectCreationCanceled();
        return true;
    }

    private boolean deployServerlessTemplate(IProgressMonitor monitor, int totalUnitOfWork) throws IOException, InterruptedException {
        String stackName = dataModel.getStackDataModel().getStackName();
        monitor.subTask("Exporting Lambda functions...");
        File jarFile = FunctionJarExportHelper.exportProjectToJarFile(dataModel.getProject(), true);
        monitor.worked((int)(totalUnitOfWork * 0.1));
        if (monitor.isCanceled()) {
            return false;
        }

        Region region = dataModel.getRegionDataModel().getRegion();
        String bucketName = dataModel.getBucketDataModel().getBucketName();
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientByRegion(region.getId());
        TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .build();

        monitor.subTask("Uploading Lambda function to S3...");
        LambdaAnalytics.trackExportedJarSize(jarFile.length());
        long startTime = System.currentTimeMillis();
        Upload upload = tm.upload(new PutObjectRequest(bucketName, dataModel.getLambdaFunctionJarFileKeyName(), jarFile));
        while (!upload.isDone() && !monitor.isCanceled()) {
            Thread.sleep(500L); // Sleep for half a second
        }
        if (upload.isDone()) {
            long uploadTime = System.currentTimeMillis() - startTime;
            LambdaAnalytics.trackUploadS3BucketTime(uploadTime);
            LambdaAnalytics.trackUploadS3BucketSpeed((double) jarFile.length()
                    / (double) uploadTime);
            monitor.worked((int) (totalUnitOfWork * 0.4));
        } else if (monitor.isCanceled()) {
            upload.abort(); // Abort the uploading and return
            return false;
        }

        monitor.subTask("Uploading Generated Serverless template to S3...");
        String generatedServerlessTemplateKeyName = stackName + "-" + System.currentTimeMillis() + ".template";
        s3.putObject(bucketName, generatedServerlessTemplateKeyName, dataModel.getUpdatedServerlessTemplate());
        monitor.worked((int)(totalUnitOfWork * 0.1));
        if (monitor.isCanceled()) {
            return false;
        }

        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClientByRegion(region.getId());
        monitor.subTask("Creating ChangeSet...");
        String changeSetName = stackName + "-changeset-" + System.currentTimeMillis();
        ChangeSetType changeSetType = ChangeSetType.CREATE;

        StackSummary stackSummary = getCloudFormationStackSummary(cloudFormation, stackName);
        Stack stack = stackSummary == null ? null : getCloudFormationStackById(cloudFormation, stackSummary.getStackId());

        if (stack == null || STATUSES_FOR_CREATE.contains(stack.getStackStatus())) {
            changeSetType = ChangeSetType.CREATE;
        } else if (STATUSES_FOR_DELETE.contains(stack.getStackStatus())) {
            String stackId = stack.getStackId();
            if (stack.getStackStatus().equals(StackStatus.ROLLBACK_IN_PROGRESS.toString())) {
                stack = waitStackForRollbackComplete(cloudFormation, stackId);
            }
            if (stack != null && stack.getStackStatus().equals(StackStatus.ROLLBACK_COMPLETE.toString())) {
                cloudFormation.deleteStack(new DeleteStackRequest().withStackName(stackName));
                waitStackForDeleteComplete(cloudFormation, stackId);
            }
            if (stack != null && stack.getStackStatus().equals(StackStatus.DELETE_IN_PROGRESS.toString())) {
                waitStackForDeleteComplete(cloudFormation, stackId);
            }
            changeSetType = ChangeSetType.CREATE;
        } else if (STATUSES_FOR_UPDATE.contains(stack.getStackStatus())) {
            changeSetType = ChangeSetType.UPDATE;
        } else {
            String errorMessage = String.format("The stack's current state of %s is invalid for updating", stack.getStackStatus());
            LambdaPlugin.getDefault().logError(errorMessage, null);
            throw new RuntimeException(errorMessage);
        }

        cloudFormation.createChangeSet(new CreateChangeSetRequest()
                .withTemplateURL(s3.getUrl(bucketName, generatedServerlessTemplateKeyName).toString())
                .withChangeSetName(changeSetName).withStackName(stackName)
                .withChangeSetType(changeSetType)
                .withCapabilities(dataModel.getCapabilitiesDataModel().getSelectedList().toArray(new Capability[0]))
                .withParameters(dataModel.getParametersDataModel().getParameters())
                .withTags(new Tag().withKey("ApiGateway").withValue("true")));

        waitChangeSetCreateComplete(cloudFormation, stackName, changeSetName);
        if (monitor.isCanceled()) {
            cloudFormation.deleteChangeSet(new DeleteChangeSetRequest().withChangeSetName(changeSetName).withStackName(stackName));
            return false;
        } else {
            monitor.worked((int)(totalUnitOfWork * 0.2));
        }

        monitor.subTask("Executing ChangeSet...");
        cloudFormation.executeChangeSet(new ExecuteChangeSetRequest()
                .withChangeSetName(changeSetName).withStackName(stackName));
        monitor.worked((int)(totalUnitOfWork * 0.2));
        return true;
    }

    /**
     * We use {@link AmazonCloudFormation#listStacks(ListStacksRequest)} instead of {@link AmazonCloudFormation#describeStacks(DescribeStacksRequest)}}
     * because describeStacks API doesn't return deleted Stacks.
     */
    private StackSummary getCloudFormationStackSummary(AmazonCloudFormation client, String stackName) {

        String nextToken = null;
        do {
            ListStacksResult result = client.listStacks(new ListStacksRequest().withNextToken(nextToken));
            nextToken = result.getNextToken();
            for (StackSummary summary : result.getStackSummaries()) {
                if (summary.getStackName().equals(stackName)) {
                    return summary;
                }
            }
        } while (nextToken != null);

        return null;
    }

    /**
     * Get stack by ID. Note: the parameter must be stack id other than stack name to return the deleted stacks.
     */
    private Stack getCloudFormationStackById(AmazonCloudFormation client, String stackId) {
        try {
            List<Stack> stacks = client.describeStacks(new DescribeStacksRequest().withStackName(stackId)).getStacks();
            return stacks.isEmpty() ? null : stacks.get(0);
        } catch (AmazonCloudFormationException e) {
            // AmazonCloudFormation throws exception if the specified stack doesn't exist.
            return null;
        }
    }

    private Stack waitStackForNoLongerInProgress(AmazonCloudFormation client, String stackId) {
        try {
            Stack currentStack;
            do {
                Thread.sleep(3000L);    // check every 3 seconds
                currentStack = getCloudFormationStackById(client, stackId);
            } while (currentStack != null && currentStack.getStackStatus().endsWith("IN_PROGRESS"));
            return currentStack;
        } catch (Exception e) {
            throw new RuntimeException("Failed for waiting stack to valid status: " + e.getMessage(), e);
        }
    }

    private Stack waitStackForRollbackComplete(AmazonCloudFormation client, String stackId) {
        Stack stack = waitStackForNoLongerInProgress(client, stackId);
        // If failed to rollback the stack, throw Runtime Exception.
        if (stack != null && !stack.getStackStatus().equals(StackStatus.ROLLBACK_COMPLETE.toString())) {
            String errorMessage = String.format("Failed to rollback the stack: ", stack.getStackStatusReason());
            LambdaPlugin.getDefault().logError(errorMessage, null);
            throw new RuntimeException(errorMessage);
        }
        return stack;
    }

    private Stack waitStackForDeleteComplete(AmazonCloudFormation client, String stackId) {
        Stack stack = waitStackForNoLongerInProgress(client, stackId);
        // If failed to rollback the stack, throw Runtime Exception.
        if (stack != null && !stack.getStackStatus().equals(StackStatus.DELETE_COMPLETE.toString())) {
            String errorMessage = String.format("Failed to delete the stack: ", stack.getStackStatusReason());
            LambdaPlugin.getDefault().logError(errorMessage, null);
            throw new RuntimeException(errorMessage);
        }
        return stack;
    }

    private void waitChangeSetCreateComplete(AmazonCloudFormation client, String stackName, String changeSetName) {
        try {

            DescribeChangeSetResult result;
            do {
                Thread.sleep(1000L);
                result = client.describeChangeSet(new DescribeChangeSetRequest()
                        .withChangeSetName(changeSetName)
                        .withStackName(stackName));
            } while (result.getStatus().equals(ChangeSetStatus.CREATE_IN_PROGRESS.toString())
                    || result.getStatus().equals(ChangeSetStatus.CREATE_PENDING.toString()));

            if (result.getStatus().equals(ChangeSetStatus.FAILED.toString())) {
                String errorMessage = "Failed to create CloudFormation change set: " + result.getStatusReason();
                LambdaPlugin.getDefault().logError(errorMessage, null);
                throw new RuntimeException(errorMessage, null);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected void initDataModel() {
        ServerlessProjectMetadata metadata = null;
        try {
            metadata = ProjectMetadataManager.loadServerlessProjectMetadata(dataModel.getProject());
        } catch (IOException e) {
            LambdaPlugin.getDefault().logError(e.getMessage(), e);
        }
        dataModel.setMetadata(metadata);

        if (metadata != null) {
            dataModel.getRegionDataModel().setRegion(RegionUtils.getRegion(metadata.getLastDeploymentRegionId()));
        }

        if (metadata == null || metadata.getLastDeploymentRegionId() == null) {
            dataModel.getRegionDataModel().setRegion(
                    RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.CLOUD_FORMATION)
                            ? RegionUtils.getCurrentRegion()
                            : RegionUtils.getRegion(LambdaPlugin.DEFAULT_REGION));
        }

        if (StringUtils.isNullOrEmpty(dataModel.getMetadata().getPackagePrefix())) {
            dataModel.getMetadata().setPackagePrefix(getPackagePrefix(dataModel.getHandlerClasses()));
        }
    }

    //A hacky way to get the package prefix when it is not cached in the metadata.
    private static String getPackagePrefix(Set<String> handlerClasses) {
        if (handlerClasses.isEmpty()) {
            return null;
        }
        String sampleClass = handlerClasses.iterator().next();
        int index = sampleClass.lastIndexOf(".function.");
        return index == -1 ? null : sampleClass.substring(0, index);
    }

    @Override
    protected void beforeExecution() {
        saveMetadata();

        List<Parameter> params = dataModel.getParametersDataModel().getParameters();
        for ( TemplateParameter parameter : dataModel.getParametersDataModel().getTemplateParameters() ) {
            String value = (String) dataModel.getParametersDataModel().getParameterValues().get(parameter.getParameterKey());
            params.add(new Parameter().withParameterKey(parameter.getParameterKey()).withParameterValue(value == null ? "" : value));
        }
    }

    private void saveMetadata() {
        ServerlessProjectMetadata metadata = dataModel.getMetadata();
        metadata.setLastDeploymentRegionId(dataModel.getRegionDataModel().getRegion().getId());
        metadata.setLastDeploymentBucket(dataModel.getBucketDataModel().getBucketName());
        metadata.setLastDeploymentStack(dataModel.getStackDataModel().getStackName());
        try {
            ProjectMetadataManager.saveServerlessProjectMetadata(dataModel.getProject(), metadata);
        } catch (IOException e) {
            LambdaPlugin.getDefault().logError(e.getMessage(), e);
        }
    }
}
