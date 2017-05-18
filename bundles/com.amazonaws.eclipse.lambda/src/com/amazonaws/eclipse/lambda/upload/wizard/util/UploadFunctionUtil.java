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
package com.amazonaws.eclipse.lambda.upload.wizard.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.s3.AmazonS3;

public class UploadFunctionUtil {

    private static final String LAMBDA_REQUEST_HANDLER_INTERFACE = "com.amazonaws.services.lambda.runtime.RequestHandler";
    private static final String LAMBDA_REQUEST_STREAM_HANDLER_INTERFACE = "com.amazonaws.services.lambda.runtime.RequestStreamHandler";

    public static void performFunctionUpload(
            UploadFunctionWizardDataModel dataModel,
            final IProgressMonitor monitor, int totalUnitOfWork)
            throws IOException {
        monitor.subTask("Exporting Lambda function project...");
        File jarFile = FunctionJarExportHelper.exportProjectToJarFile(
                dataModel.getProject(), true);
        monitor.worked((int)(totalUnitOfWork * 0.2));

        AWSLambda client = AwsToolkitCore.getClientFactory().getLambdaClientByRegion(dataModel.getRegion().getId());

        monitor.subTask("Uploading function code to S3...");
        String bucketName = dataModel.getFunctionConfigPageDataModel().getBucketName();
        // TODO use non-random key name
        //String randomKeyName = UUID.randomUUID().toString();
        String randomKeyName = dataModel.isCreatingNewFunction() ?
                dataModel.getNewFunctionName() : dataModel.getExistingFunction().getFunctionName();
        randomKeyName += ".zip";
        AmazonS3 s3 = AwsToolkitCore.getClientFactory()
                .getS3ClientForBucket(bucketName);

        LambdaAnalytics.trackExportedJarSize(jarFile.length());
        long startTime = System.currentTimeMillis();
        s3.putObject(bucketName, randomKeyName, jarFile);
        long uploadTime = System.currentTimeMillis() - startTime;

        LambdaAnalytics.trackUploadS3BucketTime(uploadTime);
        LambdaAnalytics.trackUploadS3BucketSpeed((double)jarFile.length() / (double)uploadTime);

        monitor.worked((int)(totalUnitOfWork * 0.4));

        String functionName;
        String functionArn;

        if (dataModel.isCreatingNewFunction()) {
            monitor.subTask("Creating new Lambda function...");

            CreateFunctionRequest createReq = dataModel.toCreateFunctionRequest();
            createReq.setCode(new FunctionCode()
                    .withS3Bucket(bucketName)
                    .withS3Key(randomKeyName));

            CreateFunctionResult createResult = client
                    .createFunction(createReq);

            functionName = createResult.getFunctionName();
            functionArn = createResult.getFunctionArn();
            LambdaPlugin.getDefault().logInfo(
                    "Function " + functionArn + " created.");

        } else {
            monitor.subTask("Updating function configuration");
            UpdateFunctionConfigurationResult updateConfigResult = client
                    .updateFunctionConfiguration(dataModel
                            .toUpdateFunctionConfigRequest());

            functionName = updateConfigResult.getFunctionName();

            monitor.subTask("Updating function code");
            client.updateFunctionCode(new UpdateFunctionCodeRequest()
                    .withFunctionName(functionName)
                    .withS3Bucket(bucketName)
                    .withS3Key(randomKeyName));

            functionArn = updateConfigResult.getFunctionArn();
            LambdaPlugin.getDefault().logInfo(
                    "Function " + functionArn + " updated.");
        }
        monitor.worked((int)(totalUnitOfWork * 0.2));

        monitor.subTask("Saving project metadata");
        LambdaFunctionProjectMetadata md = dataModel.getProjectMetadataBeforeUpload();
        md.setLastDeploymentHandler(dataModel.getFunctionConfigPageDataModel().getHandler());
        md.setLastDeploymentRegion(dataModel.getRegion().getId());
        md.setLastDeploymentFunctionName(functionName);
        md.setLastDeploymentBucketName(bucketName);
        md.setLastDeploymentRoleName(dataModel.getFunctionConfigPageDataModel().getRole().getRoleName());
        ProjectMetadataManager.saveLambdaProjectMetadata(dataModel.getProject(), md);
        FunctionProjectUtil.refreshProject(dataModel.getProject());
        monitor.worked((int)(totalUnitOfWork * 0.2));
        LambdaPlugin.getDefault().logInfo("Project metadata saved.");

        LambdaPlugin.getDefault().logInfo("Upload complete! Funtion arn " + functionArn);
    }

    public static Set<String> findValidHandlerClass(IProject project) {
        return findAllConcreteSubTypes(project, LAMBDA_REQUEST_HANDLER_INTERFACE);
    }

    public static Set<String> findValidStreamHandlerClass(IProject project) {
        return findAllConcreteSubTypes(project, LAMBDA_REQUEST_STREAM_HANDLER_INTERFACE);
    }

    /**
     * @see #findValidLambdaHandlerClass(IJavaProject, String)
     */
    private static Set<String> findAllConcreteSubTypes(IProject project, final String lambdaHandlerClass) {

        boolean isJavaProject = false;
        try {
            isJavaProject = project.hasNature(JavaCore.NATURE_ID);
        } catch (Exception e) {
            LambdaPlugin.getDefault()
                    .logWarning("Failed read the project nature of "
                            + project.getName(), e);
        }

        if (isJavaProject) {
            IJavaProject javaProject = JavaCore.create(project);
            return findValidLambdaHandlerClass(javaProject, lambdaHandlerClass);
        }
        return Collections.emptySet();
    }

    /**
     * @return a list of FQCNs of the concrete classes within the specified
     *         project that implement the specified lambda request handler interface, or
     *         null if any error occurred during the search.
     */
    private static Set<String> findValidLambdaHandlerClass(IJavaProject javaProject, final String lambdaHandlerClass) {
        try {
            IType type = javaProject.findType(lambdaHandlerClass);
            if (type == null) {
                return Collections.emptySet();
            }

            ITypeHierarchy typeHierarchy = type.newTypeHierarchy(javaProject, null);

            Set<String> allHandlerImplementers = new HashSet<String>();
            IType[] allSubtypes = typeHierarchy.getAllSubtypes(type);
            // filter out abstract class and interfaces
            for (IType subtype : allSubtypes) {
                if (!subtype.isInterface() && !isAbstract(subtype)) {
                    allHandlerImplementers.add(subtype.getFullyQualifiedName());
                }
            }

            return allHandlerImplementers;

        } catch (JavaModelException e) {
            LambdaPlugin.getDefault()
                    .logWarning("Failed to search for lambda request handler implementer classes ",
                          e);
            return null;
        }
    }

    private static boolean isAbstract(IType type) {
        try {
            return Flags.isAbstract(type.getFlags());
        } catch (JavaModelException e) {
            return false;
        }
    }

}
