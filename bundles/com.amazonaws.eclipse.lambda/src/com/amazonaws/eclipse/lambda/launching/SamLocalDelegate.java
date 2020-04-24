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
package com.amazonaws.eclipse.lambda.launching;

import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_ACTION;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_CODE_URI;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_DEBUG_PORT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_ENV_VARS;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_EVENT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_HOST;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_LAMBDA_IDENTIFIER;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_MAVEN_GOALS;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PORT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PROFILE;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PROJECT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_REGION;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_SAM;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_TEMPLATE;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_TIME_OUT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.PROCESS_TYPE;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.telemetry.MetricsDataModel;
import com.amazonaws.eclipse.core.util.CliUtil;
import com.amazonaws.eclipse.core.util.MavenBuildLauncher;
import com.amazonaws.eclipse.core.util.PluginUtils;
import com.amazonaws.eclipse.core.util.RemoteDebugLauncher;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.launching.SamLocalExecution.LaunchMode;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel.SamAction;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.ui.LambdaPluginColors;

public class SamLocalDelegate implements ILaunchConfigurationDelegate {

    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {

        MetricsDataModel metricsDataModel = new MetricsDataModel(AwsToolkitMetricType.SAMLOCAL_LAUNCH);
        try {
            LaunchMode launchMode = LaunchMode.fromValue(mode);
            metricsDataModel.addAttribute("LaunchMode", launchMode.getMode());
            String projectName = configuration.getAttribute(A_PROJECT, (String) null);

            if (projectName == null || projectName.isEmpty()) {
                throw new IllegalArgumentException("The project name must be provided!");
            }
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

            subMonitor.setTaskName("Running Maven build to generate the artifact...");
            long startTime = System.currentTimeMillis();
            new MavenBuildLauncher(project, configuration.getAttribute(A_MAVEN_GOALS, RunSamLocalDataModel.DEFAULT_MAVEN_GOALS),
                    subMonitor.newChild(30)).launch();
            long endTime = System.currentTimeMillis();
            metricsDataModel.addMetric("MavenBuildTimeMilli", (double)(endTime-startTime));
            subMonitor.worked(30);

            List<String> commandLine = buildSamLocalCommandLine(launchMode, configuration.getAttributes());
            Map<String, String> envvar = new HashMap<>();
            String regionValue = configuration.getAttribute(A_REGION, RunSamLocalDataModel.DEFAULT_REGION);
            envvar.put("AWS_REGION", regionValue);
            Process samLocalCliProcess = CliUtil.buildProcess(commandLine, envvar);

            Map<String, String> attributes = new HashMap<>();
            attributes.put(IProcess.ATTR_PROCESS_TYPE, PROCESS_TYPE);
            IProcess samLocalProcess = DebugPlugin.newProcess(launch, samLocalCliProcess, projectName, attributes);

            IOConsole samLocalConsole = null;
            do {
                samLocalConsole = (IOConsole) DebugUITools.getConsole(samLocalProcess);
            } while (samLocalConsole == null
                    && !subMonitor.isCanceled()
                    && !samLocalProcess.isTerminated());

            IOConsoleOutputStream samLocalOutputStream = samLocalConsole.newOutputStream();
            samLocalOutputStream.setColor(LambdaPluginColors.GREY);
            safeWriteToConsole(samLocalOutputStream, "Running command: " + commandLine.stream().collect(Collectors.joining(" ")));

            int debugPort = Integer.parseInt(configuration.getAttribute(A_DEBUG_PORT, String.valueOf(RunSamLocalDataModel.DEFAULT_DEBUG_PORT)));
            SamAction command = SamAction.fromValue(configuration.getAttribute(A_ACTION, SamAction.INVOKE.getName()));
            subMonitor.worked(20);

            metricsDataModel.addAttribute("SamLocalCommand", command.getName());
            if (command == SamAction.INVOKE) {
                try {
                    waitAndLaunchDebugger(project, launchMode, debugPort, samLocalCliProcess, subMonitor, samLocalOutputStream, 50);
                } catch (CoreException e) {
                    samLocalProcess.terminate();
                    throw new RuntimeException("Failed to launch Eclipse Debugger", e);
                }
            } else if (command == SamAction.START_API) {
                try {
                    int invokeTimes = 0;
                    while (!subMonitor.isCanceled() && !samLocalProcess.isTerminated()) {
                        waitAndLaunchDebugger(project, launchMode, debugPort, samLocalCliProcess, subMonitor, samLocalOutputStream, 1);
                        ++invokeTimes;
                    }
                    metricsDataModel.addMetric("InvokeTimes", (double)invokeTimes);
                } catch (CoreException e) {
                    samLocalProcess.terminate();
                    throw new RuntimeException("Failed to launch Eclipse Debugger", e);
                }
            }
            if (subMonitor.isCanceled()) {
                samLocalProcess.terminate();
            }
            while (!samLocalProcess.isTerminated()) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                }
            }
            safeWriteToConsole(samLocalOutputStream, "SAM Local invocation done.");
            subMonitor.done();
            AwsAction.publishSucceededAction(metricsDataModel);
        } catch (Exception e) {
            LambdaPlugin.getDefault().reportException("Failed to launch SAM Local.",
                    new AwsActionException(AwsToolkitMetricType.SAMLOCAL_LAUNCH.getName(), e.getMessage(), e));
            AwsAction.publishFailedAction(metricsDataModel);
        }
    }

    /**
     * Wait the target port to be taken by the process and kick off Eclipse Debugger.
     *
     * @param project The target project the Debugger is running in
     * @param mode debug or run mode
     * @param debugPort The debug port
     * @param samLocalCliProcess SAM Local process
     * @param subMonitor {@link SubMonitor} for reporting the progress
     * @param outputStream The console output stream for showing the current status
     * @param totalWork Total work the Debugger takes
     * @throws CoreException
     */
    private void waitAndLaunchDebugger(IProject project, LaunchMode mode, int debugPort,
            Process samLocalCliProcess, SubMonitor subMonitor, IOConsoleOutputStream outputStream, int totalWork) throws CoreException {
        if (mode == LaunchMode.DEBUG) {
            String statusUpdateMessage = "Waiting for SAM Local to attach the port " + debugPort;
            subMonitor.setTaskName(statusUpdateMessage);
            safeWriteToConsole(outputStream, statusUpdateMessage);
        }

        if (waitForPortBeingTakenByProcess(samLocalCliProcess, debugPort, subMonitor)) {
            String statusUpdateMessage = "Running Eclipse Debugger...";
            subMonitor.setTaskName(statusUpdateMessage);
            safeWriteToConsole(outputStream, statusUpdateMessage);
            new RemoteDebugLauncher(project, debugPort, subMonitor.newChild(totalWork)).launch();
            subMonitor.worked(totalWork);
        }
    }

    private void safeWriteToConsole(IOConsoleOutputStream outputStream, String content) {
        try {
            outputStream.write("[AWS Toolkit] " + content + "\n");
        } catch (IOException e) {
            LambdaPlugin.getDefault().logError("Failed to write to SAM Local console.", e);
        }
    }

    /**
     * Create a temporary template file based on the provided configuration map. It modifies the
     * template file in A_TEMPLATE with the overriding value of A_CODE_URI and A_TIME_OUT.
     */
    private String createTemporaryTemplateFile(Map<String, Object> config) throws CoreException {
        String codeUri = (String) config.get(A_CODE_URI);
        String timeOut = (String) config.get(A_TIME_OUT);
        String project = (String) config.get(A_PROJECT);
        String templateFile = (String) config.get(A_TEMPLATE);

        String templateFilePath = PluginUtils.variablePluginReplace(templateFile);
        try {
            ServerlessModel serverlessModel = Serverless.load(templateFilePath);
            serverlessModel.getServerlessFunctions().forEach((k, v) -> {
                if (codeUri != null && !codeUri.isEmpty()) {
                    v.setCodeUri(codeUri);
                }
                v.setTimeout(Integer.parseInt(timeOut));
            });

            IPath tempTemplate = ResourcesPlugin.getWorkspace().getRoot().getProject(project).getLocation();

            String outputPath = tempTemplate.append(".serverless.template").toOSString();
            File outputFile = Serverless.write(serverlessModel, outputPath);
            outputFile.deleteOnExit();
            return outputPath;
        } catch (IOException e ) {
            // Any error occurs, fall back to the original template file.
            return templateFilePath;
        }
    }

    private List<String> buildSamLocalCommandLine(LaunchMode mode, Map<String, Object> config) throws CoreException {
        final Set<String> invokeParamSet = new HashSet<>(Arrays.asList(A_TEMPLATE, A_ENV_VARS, A_EVENT, A_PROFILE));
        final Set<String> startApiParamSet = new HashSet<>(Arrays.asList(A_TEMPLATE, A_ENV_VARS, A_PROFILE, A_HOST, A_PORT));

        if (mode == LaunchMode.DEBUG) {
            invokeParamSet.add(A_DEBUG_PORT);
            startApiParamSet.add(A_DEBUG_PORT);
        }

        List<String> commandLine = new ArrayList<>();
        commandLine.add(config.get(A_SAM).toString());
        commandLine.add("local");
        commandLine.add(config.get(A_ACTION).toString());

        SamAction samAction = SamAction.fromValue(config.get(A_ACTION).toString());
        switch (samAction) {
        case INVOKE:
            if (config.get(A_LAMBDA_IDENTIFIER) != null) {
                commandLine.add(config.get(A_LAMBDA_IDENTIFIER).toString());
            }
            commandLine = buildCommandLineFromMap(commandLine, config, invokeParamSet);
            break;
        case START_API:
            commandLine = buildCommandLineFromMap(commandLine, config, startApiParamSet);
            break;
        }

        return commandLine;
    }

    private List<String> buildCommandLineFromMap(List<String> commandLine, Map<String, Object> parameters, Set<String> parameterSet)
            throws CoreException {
        // Replacing these attributes with actually values from the path placeholder
        Set<String> replacableParams = new HashSet<>(Arrays.asList(A_TEMPLATE, A_EVENT, A_ENV_VARS));
        for (Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (!parameterSet.contains(key)) {
                continue;
            }
            String value = parameters.get(key).toString();
            if (replacableParams.contains(key)) {
                value = PluginUtils.variablePluginReplace(value);
            }
            commandLine.add("--" + key);
            if (key.equals(A_TEMPLATE)) {
                value = createTemporaryTemplateFile(parameters);
            }
            commandLine.add(value);
        }
        return commandLine;
    }

    // Wait for the port being take by the specified Process.
    // Return true if the port is taken in the given condition, otherwise false.
    private boolean waitForPortBeingTakenByProcess(Process process, int portNo, IProgressMonitor monitor) {
        long pingIntervalMilli = 100;
        while (process.isAlive() && !monitor.isCanceled()) {
            try (Socket socket = new Socket("localhost", portNo)) {
                return true;
            } catch (UnknownHostException e1) {
                return false;
            } catch (IOException e1) {}

            try {
                Thread.sleep(pingIntervalMilli);
            } catch (InterruptedException e) {}
        }
        return false;
    }
}
