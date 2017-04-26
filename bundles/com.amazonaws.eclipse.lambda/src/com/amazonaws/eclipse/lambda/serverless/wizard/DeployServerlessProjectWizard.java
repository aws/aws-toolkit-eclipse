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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.cloudformation.OpenStackEditorAction;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.wizard.model.DeployServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.serverless.ui.DeployServerlessProjectPage;
import com.amazonaws.eclipse.lambda.upload.wizard.page.S3BucketUtil;
import com.amazonaws.eclipse.lambda.upload.wizard.util.FunctionJarExportHelper;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.ChangeSetType;
import com.amazonaws.services.cloudformation.model.CreateChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DeleteChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeChangeSetRequest;
import com.amazonaws.services.cloudformation.model.DescribeChangeSetResult;
import com.amazonaws.services.cloudformation.model.ExecuteChangeSetRequest;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

public class DeployServerlessProjectWizard extends Wizard {
    private final IProject project;
    private final String packagePrefix;
    private final DeployServerlessProjectDataModel dataModel;

    public DeployServerlessProjectWizard(IProject project, String packagePrefix) {
        this.project = project;
        this.packagePrefix = packagePrefix;
        dataModel = new DeployServerlessProjectDataModel(project.getName());
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new DeployServerlessProjectPage(dataModel));
    }

    @Override
    public boolean performFinish() {

        try {
            Job deployJob = new Job("Deploying Serverless template to AWS CloudFormation.") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    monitor.beginTask("Deploying Serverless template to AWS CloudFormation.", 100);
                    try {
                        if (deployServerlessTemplate(monitor, 100)) {
                            new OpenStackEditorAction(
                                dataModel.getStackName(), dataModel.getRegion(), true).run();
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
            };

            deployJob.setUser(true);
            deployJob.schedule();

        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException(
                    "Unexpected error during deployment", e.getCause());
        }

        return true;
    }

    @Override
    public boolean performCancel() {
        LambdaAnalytics.trackServerlessProjectCreationCanceled();
        return true;
    }

    private boolean deployServerlessTemplate(IProgressMonitor monitor, int totalUnitOfWork) throws IOException, InterruptedException {
        String stackName = dataModel.getStackName();

        monitor.subTask("Exporting Lambda functions...");
        File jarFile = FunctionJarExportHelper.exportProjectToJarFile(
                project, true);
        monitor.worked((int)(totalUnitOfWork * 0.1));
        if (monitor.isCanceled()) {
            return false;
        }

        Region region = dataModel.getRegion();
        String bucketName = dataModel.getBucketName();
        String lambdaFunctionJarFileKeyName = stackName + "-" + System.currentTimeMillis() + ".zip";
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientByEndpoint(
                dataModel.getRegion().getServiceEndpoint(ServiceAbbreviations.S3));
        TransferManager tm = new TransferManager(s3);

        monitor.subTask("Uploading Lambda function to S3...");
        LambdaAnalytics.trackExportedJarSize(jarFile.length());
        long startTime = System.currentTimeMillis();
        Upload upload = tm.upload(new PutObjectRequest(bucketName, lambdaFunctionJarFileKeyName, jarFile));
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
        File serverlessTemplateFile = FunctionProjectUtil
                .getServerlessTemplateFile(project);
        ServerlessModel model = Serverless.load(serverlessTemplateFile);
        model = Serverless.cookServerlessModel(model, packagePrefix,
                S3BucketUtil.createS3Path(bucketName, lambdaFunctionJarFileKeyName));

        String generatedServerlessFilePath = File.createTempFile(
                "serverless-template", ".json").getAbsolutePath();
        File serverlessGeneratedTemplateFile = Serverless.write(model, generatedServerlessFilePath);
        String generatedServerlessTemplateKeyName = stackName + "-" + System.currentTimeMillis() + ".template";
        s3.putObject(bucketName, generatedServerlessTemplateKeyName, serverlessGeneratedTemplateFile);
        monitor.worked((int)(totalUnitOfWork * 0.1));
        if (monitor.isCanceled()) {
            return false;
        }

        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory()
                .getCloudFormationClientByEndpoint(region.getServiceEndpoint(ServiceAbbreviations.CLOUD_FORMATION));
        monitor.subTask("Creating ChangeSet...");
        String changeSetName = stackName + "-changeset-" + System.currentTimeMillis();
        cloudFormation.createChangeSet(new CreateChangeSetRequest()
                .withTemplateURL(s3.getUrl(bucketName, generatedServerlessTemplateKeyName).toString())
                .withChangeSetName(changeSetName).withStackName(stackName)
                .withChangeSetType(ChangeSetType.CREATE)
                .withCapabilities(Capability.CAPABILITY_IAM)
                .withTags(new Tag().withKey("ApiGateway").withValue("true")));

        //TODO: add a waiter waitChangeSetCreateComplete and use that instead.
        DescribeChangeSetResult result = cloudFormation
                .describeChangeSet(new DescribeChangeSetRequest()
                        .withChangeSetName(changeSetName).withStackName(
                                stackName));
        while (!result.getStatus().equals("CREATE_COMPLETE")) {
            Thread.sleep(1000L);    // check every 1 second for the creation status
            result = cloudFormation
                    .describeChangeSet(new DescribeChangeSetRequest()
                            .withChangeSetName(changeSetName).withStackName(
                                    stackName));
            if (result.getStatus().equals("FAILED")) {
                throw new RuntimeException("Changeset creation failed! " + result.getDescription() + " Please go to AWS Console to see the detailed error message.");
            }
        }
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
}
