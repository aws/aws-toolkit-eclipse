/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.invoke.handler;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.util.PluginUtils;
import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.invoke.logs.CloudWatchLogsUtils;
import com.amazonaws.eclipse.lambda.invoke.ui.InvokeFunctionInputDialog;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionDeploymentMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionInvokeMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata.LambdaFunctionMetadata;
import com.amazonaws.eclipse.lambda.project.metadata.ProjectMetadataManager;
import com.amazonaws.eclipse.lambda.ui.LambdaJavaProjectUtil;
import com.amazonaws.eclipse.lambda.upload.wizard.handler.UploadFunctionToLambdaCommandHandler;
import com.amazonaws.eclipse.lambda.upload.wizard.util.FunctionJarExportHelper;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.util.StringUtils;

public class InvokeFunctionHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IJavaElement selectedJavaElement = LambdaJavaProjectUtil.getSelectedJavaElementFromCommandEvent(event);
        if (selectedJavaElement != null) {
            try {
                invokeLambdaFunctionProject(selectedJavaElement);
            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException(
                        "Failed to launch upload function wizard.",
                        new AwsActionException(AwsToolkitMetricType.LAMBDA_INVOKE_FUNCTION_DIALOG.getName(), e.getMessage(), e));
            }
        }

        return null;
    }

    public static void invokeLambdaFunctionProject(IJavaElement selectedJavaElement) {

        LambdaFunctionProjectMetadata md = null;
        IProject project = selectedJavaElement.getJavaProject().getProject();
        try {
            md = ProjectMetadataManager.loadLambdaProjectMetadata(project);
        } catch (IOException e) {
            // Log this error but still proceed.
            LambdaPlugin.getDefault().logError("Failed to read metadata in the project: " + e.getMessage(), e);
        }

        if (md != null) {
            // Invoke related properties are to be populated within inputDialog
            InvokeFunctionInputDialog inputDialog = new InvokeFunctionInputDialog(
                    Display.getCurrent().getActiveShell(), selectedJavaElement, md);
            int retCode = inputDialog.open();

            if (retCode == InvokeFunctionInputDialog.INVOKE_BUTTON_ID) {
                boolean isProjectDirty = LambdaPlugin.getDefault()
                        .getProjectChangeTracker().isProjectDirty(project);

                if (!md.isLastInvokedHandlerDeployed()) {
                    askForDeploymentFirst(selectedJavaElement);
                    return;
                }
                if (isProjectDirty) {
                    invokeAfterRepeatingLastDeployment(project, md);
                } else {
                    invokeWithoutDeployment(project, md);
                }
            } else {
                LambdaAnalytics.trackInvokeCanceled();
            }

        } else {
            askForDeploymentFirst(selectedJavaElement);
        }
    }

    private static void invokeAfterRepeatingLastDeployment(final IProject project, final LambdaFunctionProjectMetadata metadata) {
        _doInvoke(project, metadata, true);
    }

    private static void invokeWithoutDeployment(final IProject project, final LambdaFunctionProjectMetadata metadata) {
        _doInvoke(project, metadata, false);
    }

    private static void _doInvoke(final IProject project,
            final LambdaFunctionProjectMetadata metadata,
            final boolean updateFunctionCode) {

        String handlerToBeInvoked = metadata.getLastInvokeHandler();

        MessageConsole lambdaConsole = PluginUtils.getOrCreateMessageConsole(
                handlerToBeInvoked + " Lambda Console");
        lambdaConsole.clearConsole();
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(lambdaConsole);
        final MessageConsoleStream lambdaOutput = lambdaConsole.newMessageStream();
        final MessageConsoleStream lambdaError = lambdaConsole.newMessageStream();
        lambdaError.setColor(new Color(Display.getDefault(), 255, 0, 0));

        new Job("Running " + handlerToBeInvoked + " on Lambda...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    invokeLatestLambdaFunction(project, metadata, updateFunctionCode, lambdaOutput, lambdaError);
                } catch (Exception e) {
                    showLambdaInvocationError(lambdaError, e);
                }

                LambdaAnalytics.trackInvokeSucceeded();
                safelyCloseMessageConsoleStreams(lambdaOutput, lambdaError);

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private static void invokeLatestLambdaFunction(IProject project,
            LambdaFunctionProjectMetadata metadata,
            boolean updateFunctionCode,
            MessageConsoleStream lambdaOutput,
            MessageConsoleStream lambdaError) throws IOException {

        String handlerToBeInvoked = metadata.getLastInvokeHandler();
        LambdaFunctionMetadata functionMetadata = metadata.getHandlerMetadata().get(handlerToBeInvoked);
        assert (functionMetadata != null && functionMetadata.getDeployment() != null && functionMetadata.getInvoke() != null);

        LambdaFunctionDeploymentMetadata deploymentMetadata = functionMetadata.getDeployment();
        LambdaFunctionInvokeMetadata invokeMetadata = functionMetadata.getInvoke();
        AWSLambda lambda = AwsToolkitCore.getClientFactory().getLambdaClientByRegion(
                deploymentMetadata.getRegionId());
        String funcName = deploymentMetadata.getAwsLambdaFunctionName();
        String bucketName = deploymentMetadata.getAwsS3BucketName();
        String invokeInput = invokeMetadata.getInvokeInput();
        boolean showLiveLog = invokeMetadata.isShowLiveLog();

        if (updateFunctionCode) {
            updateFunctionCode(lambda, project, funcName, bucketName, lambdaOutput);
            /*
             * clear the dirty flag so that the next invoke will not
             * attempt to re-upload the code if no change is made to
             * the project.
             */
            LambdaPlugin.getDefault().getProjectChangeTracker()
                    .markProjectAsNotDirty(project);
        } else {
            lambdaOutput
                    .println("Skip uploading function code since no local change is found...");
        }
        InvokeResult result = invokeFunction(lambda, invokeInput, funcName, lambdaOutput);
        if (showLiveLog) showLambdaLiveLog(result, lambdaOutput, lambdaError);
        ProjectMetadataManager.saveLambdaProjectMetadata(project, metadata);
    }

    private static void safelyCloseMessageConsoleStreams(MessageConsoleStream... streams) {
        if (streams == null) return;
        try {
            for (MessageConsoleStream stream : streams) {
                stream.close();
            }
        } catch (IOException e) {
            LambdaPlugin.getDefault().logWarning(
                    "Failed to close console message stream.", e);
        }
    }

    private static void showLambdaInvocationError(MessageConsoleStream lambdaError, Exception e) {
        LambdaAnalytics.trackInvokeFailed();
        lambdaError.println("==================== INVOCATION ERROR ====================");
        lambdaError.println(e.toString());
    }

    private static void showLambdaLiveLog(InvokeResult result, MessageConsoleStream lambdaOutput, MessageConsoleStream lambdaError) {
        lambdaOutput.println();
        lambdaOutput.println("==================== FUNCTION LOG OUTPUT ====================");
        String log = CloudWatchLogsUtils.fetchLogsForLambdaFunction(result);
        if (log != null) {
            lambdaOutput.print(log);
            if (log.length() >= CloudWatchLogsUtils.MAX_LAMBDA_LOG_RESULT_LENGTH) {
                lambdaError.println("WARNING: Log is truncated for being longer than 4Kb.");
                lambdaError.println("To see the complete log, go to AWS CloudWatch Logs console.");
            }
        }
    }

    private static void updateFunctionCode(AWSLambda lambda, IProject project,
            String funcName, String bucketName, MessageConsoleStream out)
            throws IOException {

        out.println("Uploading function code to " + funcName + "...");

        File funcCodeFile = FunctionJarExportHelper.exportProjectToJarFile(
                project, false);
        String randomKeyName = UUID.randomUUID().toString();

        AmazonS3 s3 = AwsToolkitCore.getClientFactory()
                .getS3ClientForBucket(bucketName);
        s3.putObject(bucketName, randomKeyName, funcCodeFile);

        UpdateFunctionCodeResult result =  lambda.updateFunctionCode(
                new UpdateFunctionCodeRequest()
                        .withFunctionName(funcName)
                        .withS3Bucket(bucketName)
                        .withS3Key(randomKeyName));

        // Clean up ourself after the function is created
        try {
            s3.deleteObject(bucketName, randomKeyName);
        } catch (Exception e) {
            out.println(String.format(
                    "Failed to cleanup function code in S3. " +
                    "Please remove the object manually s3://%s/%s",
                    bucketName, randomKeyName));
        }

        out.println("Upload success. Function ARN: " + result.getFunctionArn());
    }

    private static InvokeResult invokeFunction(AWSLambda lambda, String input,
            String funcName, MessageConsoleStream out) {

        out.println("Invoking function...");

        InvokeRequest invokeRequest = new InvokeRequest()
            .withFunctionName(funcName)
            .withInvocationType(InvocationType.RequestResponse)
            .withLogType(LogType.Tail)
            .withPayload(input);

        InvokeResult result = lambda.invoke(invokeRequest);

        out.println("==================== FUNCTION OUTPUT ====================");
        out.print(readPayload(result.getPayload()));

        return result;
    }

    private static void askForDeploymentFirst(IJavaElement selectedJavaElement) {
        MessageDialog dialog = new MessageDialog(
                Display.getCurrent().getActiveShell(),
                "Function not uploaded yet", null,
                "You need to upload the function to Lambda before invoking it.",
                MessageDialog.INFORMATION,
                new String[] { "Upload now", "Cancel"}, 0);
        int result = dialog.open();

        if (result == 0) {
            LambdaAnalytics.trackUploadWizardOpenedBeforeFunctionInvoke();
            UploadFunctionToLambdaCommandHandler.doUploadFunctionProjectToLambda(selectedJavaElement);
        }
    }

    private static String readPayload(ByteBuffer payload) {
        return new String(payload.array(), StringUtils.UTF8);
    }
}
