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
package com.amazonaws.eclipse.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Utility for calling a CLI command, and managing the input/output of the process.
 */
public class CliUtil {

    public static CliProcessTracker executeCommand(List<String> commandLine, Map<String, String> envp, OutputStream stdOut, OutputStream stdErr)
            throws IOException {
        Process process = buildProcess(commandLine, envp);
        PipeThread stdInputOutput = new PipeThread("CLI stdout", process.getInputStream(), stdOut);
        PipeThread stdErrThread = new PipeThread("CLI stderr", process.getErrorStream(), stdErr);
        stdInputOutput.start();
        stdErrThread.start();
        return new CliProcessTracker(process, stdInputOutput, stdErrThread);
    }

    /**
     * Using {@link ProcessBuilder} to build a {@link Process}. {@link ProcessBuilder} aggregates a copy of the current process environment
     * so that we don't have to manually copy these.
     *
     * @see {@link ProcessBuilder#environment()}
     */
    public static Process buildProcess(List<String> commandLine, Map<String, String> envp) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.environment().putAll(envp);
        return processBuilder.start();
    }

    /**
     * Tracks the process along with its two stream threads.
     */
    public static class CliProcessTracker {
        private final Process process;
        private final PipeThread stdOutThread;
        private final PipeThread stdErrThread;

        public CliProcessTracker(Process process, PipeThread stdOutThread, PipeThread stdErrThread) {
            this.process = process;
            this.stdOutThread = stdOutThread;
            this.stdErrThread = stdErrThread;
        }

        public Process getProcess() {
            return process;
        }

        /**
         * Destroy the process and wait for the Stream threads to finish.
         */
        public void destroy() {
            process.destroy();
            waitForStream();
        }

        /**
         * Wait for the Stream threads to finish. Ie. to drain the input streams, and return the exit code.
         */
        public int waitForStream() {
            try {
                stdOutThread.join();
                stdErrThread.join();
            } catch (InterruptedException e) {
            }
            return process.exitValue();
        }
    }

    /**
     * A thread that keeps reading from a target InputStream, and writes to an OutputStream.
     */
    private static class PipeThread extends Thread {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        private PipeThread(String name, InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.setName(name);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    outputStream.write(String.format("%s%n", line).getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {}
        }
    }
}
