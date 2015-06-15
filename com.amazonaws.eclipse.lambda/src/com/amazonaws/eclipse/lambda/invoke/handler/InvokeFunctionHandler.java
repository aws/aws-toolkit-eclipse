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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.invoke.ui.InvokeFunctionInputDialog;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;
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

    private static final String LAMBDA_CONSOLE_NAME = "AWS Lambda Console";

    public Object execute(ExecutionEvent event) throws ExecutionException {

        ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event)
                .getActivePage().getSelection();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structurredSelection = (IStructuredSelection)selection;
            Object firstSeleciton = structurredSelection.getFirstElement();

            IProject selectedProject = null;

            if (firstSeleciton instanceof IProject) {
                selectedProject = (IProject) firstSeleciton;
            } else if (firstSeleciton instanceof IJavaProject) {
                selectedProject = ((IJavaProject) firstSeleciton).getProject();
            } else {
                LambdaPlugin.getDefault().logInfo(
                        "Invalid selection: " + firstSeleciton + " is not a project.");
                return null;
            }

            try {
                invokeLambdaFunctionProject(selectedProject);
            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException(
                        "Failed to launch upload function wizard.", e);
            }
        }

        return null;
    }

    public static void invokeLambdaFunctionProject(IProject project) {

        LambdaFunctionProjectMetadata md = FunctionProjectUtil
                .loadLambdaProjectMetadata(project);

        if (md != null && md.isValid()) {
            InvokeFunctionInputDialog inputDialog = new InvokeFunctionInputDialog(
                    Display.getCurrent().getActiveShell(), project);
            int retCode = inputDialog.open();

            if (retCode == InvokeFunctionInputDialog.INVOKE_BUTTON_ID) {
                String input = inputDialog.getInputBoxContent();
                invokeAfterRepeatingLastDeployment(input, project, md);
            }

        } else {
            askForDeploymentFirst(project);
        }
    }

    private static void invokeAfterRepeatingLastDeployment(final String invokeInput,
            final IProject project, final LambdaFunctionProjectMetadata metadata) {

        MessageConsole console = getOrCreateLambdaConsoleIfNotExist();
        console.clearConsole();
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
        final MessageConsoleStream consoleOutput = console.newMessageStream();

        final AWSLambda lambda = AwsToolkitCore.getClientFactory()
                .getLambdaClientByEndpoint(metadata.getLastDeploymentEndpoint());
        final String funcName = metadata.getLastDeploymentFunctionName();
        final String bucketName = metadata.getLastDeploymentBucketName();

        new Job("Running " + funcName + " on Lambda...") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    updateFunctionCode(lambda, project, funcName, bucketName, consoleOutput);
                    invokeFunction(lambda, invokeInput, funcName, consoleOutput);
                    saveInvokeInputInProjectMetadata(invokeInput, project);
                } catch (Exception e) {
                    consoleOutput.println("==================== INVOCATION ERROR ====================");
                    consoleOutput.println(e.toString());
                }

                try {
                    consoleOutput.close();
                } catch (IOException e) {
                    LambdaPlugin.getDefault().warn(
                            "Failed to close console message stream.", e);
                }

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private static MessageConsole getOrCreateLambdaConsoleIfNotExist() {

        IConsoleManager consoleManager = ConsolePlugin.getDefault()
                .getConsoleManager();

        // Search existing consoles
        if (consoleManager.getConsoles() != null) {
            for (IConsole console : consoleManager.getConsoles()) {
                if (LAMBDA_CONSOLE_NAME.equals(console.getName())
                        && (console instanceof MessageConsole)) {
                    return (MessageConsole)console;
                }
            }
        }

        // If not found, create a new console
        MessageConsole newConsole = new MessageConsole(LAMBDA_CONSOLE_NAME, null);
        ConsolePlugin.getDefault().getConsoleManager()
                .addConsoles(new IConsole[] { newConsole });
        return newConsole;
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

    private static void invokeFunction(AWSLambda lambda, String input,
            String funcName, MessageConsoleStream out) {

        out.println("Invoking function...");

        InvokeResult result = lambda.invoke(new InvokeRequest()
                .withFunctionName(funcName)
                .withInvocationType(InvocationType.RequestResponse)
                .withLogType(LogType.None)
                .withPayload(input));

        out.println("==================== FUNCTION OUTPUT ====================");
        out.print(readPayload(result.getPayload()));
    }

    private static void saveInvokeInputInProjectMetadata(String invokeInput,
            IProject project) {

        // Load existing metadata so it doens't clobber other metadata fields
        LambdaFunctionProjectMetadata md = FunctionProjectUtil
                .loadLambdaProjectMetadata(project);
        md.setLastInvokeInput(invokeInput);

        FunctionProjectUtil.addLambdaProjectMetadata(project, md);
    }

    private static void askForDeploymentFirst(IProject project) {
        MessageDialog dialog = new MessageDialog(
                Display.getCurrent().getActiveShell(),
                "Function not uploaded yet", null,
                "You need to upload the function to Lambda before invoking it.",
                MessageDialog.INFORMATION,
                new String[] { "Upload now", "Cancel"}, 0);
        int result = dialog.open();

        if (result == 0) {
            UploadFunctionToLambdaCommandHandler.doUploadFunctionProjectToLambda(project);
        }
    }

    private static String readPayload(ByteBuffer payload) {
        return new String(payload.array(), StringUtils.UTF8);
    }

}
