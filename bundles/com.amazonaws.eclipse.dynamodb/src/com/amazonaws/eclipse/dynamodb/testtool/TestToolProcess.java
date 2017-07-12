/*
 * Copyright 2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.dynamodb.testtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;

/**
 * A DynamoDB Local Test Tool process.
 */
public class TestToolProcess {

    private final IVMInstall jre;
    private final File installDirectory;
    private final int port;

    private final Thread shutdownHook = new Thread() {
        @Override
        public void run() {
            TestToolProcess.this.stopProcess();
        }
    };

    private Process process;

    /**
     * Create a new {@code TestToolProcess}.
     *
     * @param jre               the JRE to use to run DynamoDBLocal
     * @param installDirectory  the root install directory for DynamoDBLocal
     * @param port              the TCP port to bind to
     */
    public TestToolProcess(final IVMInstall jre,
                           final File installDirectory,
                           final int port) {
        this.jre = jre;
        this.installDirectory = installDirectory;
        this.port = port;
    }

    /**
     * @return the port that this DynamoDBLocal process is listening on
     */
    public int getPort() {
        return port;
    }

    /**
     * Start the DynamoDBLocal process.
     *
     * @param  onExitAction optional action to be executed when the process
     *                      exits
     * @throws IOException  if starting the process fails
     */
    public synchronized void start(final Runnable onExitAction)
            throws IOException {

        if (process != null) {
            throw new IllegalStateException("Already started!");
        }

        ProcessBuilder builder = new ProcessBuilder();

        builder.directory(installDirectory);
        builder.command(
            jre.getInstallLocation().getAbsolutePath().concat("/bin/java"),
            "-Djava.library.path=".concat(findLibraryDirectory().getAbsolutePath()),
            "-jar",
            "DynamoDBLocal.jar",
            "--port",
            Integer.toString(port)
        );

        // Drop STDERR into STDOUT so we can handle them together.
        builder.redirectErrorStream(true);

        // Register a shutdown hook to kill DynamoDBLocal if Eclipse exits.
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Start the DynamoDBLocal process.
        process = builder.start();

        // Start a background thread to read any output from DynamoDBLocal
        // and dump it to an IConsole.
        new ConsoleOutputLogger(process.getInputStream(), onExitAction)
            .start();
    }

    /**
     * Searches within the install directory for the native libraries required
     * by DyanmoDB Local (i.e. SQLite) and returns the directory containing the
     * native libraries.
     *
     * @return The directory within the install directory where native libraries
     *         were found; otherwise, if no native libraries are found, the
     *         install directory is returned.
     */
    private File findLibraryDirectory() {
        // Mac and Linux libraries start with "libsqlite4java-" so
        // use that pattern to identify the library directory
        IOFileFilter fileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("libsqlite4java-");
            }
        };

        Collection<File> files = FileUtils.listFiles(installDirectory, fileFilter, TrueFileFilter.INSTANCE);

        // Log a warning if we can't identify the library directory,
        // and then just try to use the install directory
        if (files == null || files.isEmpty()) {
            Status status = new Status(IStatus.WARNING, DynamoDBPlugin.PLUGIN_ID,
                    "Unable to find DynamoDB Local native libraries in " + installDirectory);
            AwsToolkitCore.getDefault().getLog().log(status);
            return installDirectory;
        }

        return files.iterator().next().getParentFile();
    }

    /**
     * Stop the DynamoDBLocal process and deregister the shutdown hook.
     */
    public synchronized void stop() {
        stopProcess();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    /**
     * Internal helper method to actually stop the DynamoDBLocal process.
     */
    private void stopProcess() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    /**
     * A background thread that reads output from the DynamoDBLocal process
     * and pumps it to a console window.
     */
    private static class ConsoleOutputLogger extends Thread {

        private final BufferedReader input;
        private final Runnable onExitAction;

        /**
         * Create a new logger.
         *
         * @param stream        the input stream to read from
         * @param onExitaction  an optional action to be run when the process
         *                      exits
         */
        public ConsoleOutputLogger(final InputStream stream,
                                   final Runnable onExitAction) {

            this.input = new BufferedReader(new InputStreamReader(stream));
            this.onExitAction = onExitAction;

            super.setDaemon(true);
            super.setName("DynamoDBLocal Console Output Logger");
        }

        @Override
        public void run() {
            MessageConsole console = findConsole();
            displayConsole(console);

            MessageConsoleStream stream = console.newMessageStream();

            try {

                while (true) {
                    String line = input.readLine();
                    if (line == null) {
                        stream.println("*** DynamoDB Local Exited ***");
                        break;
                    }

                    stream.println(line);
                }

            } catch (IOException exception) {
                stream.println("Exception reading the output of "
                               + "DynamoDB Local: " + exception.toString());
            } finally {
                try {
                    input.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

                try {
                    stream.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

                if (onExitAction != null) {
                    onExitAction.run();
                }
            }
        }

        /**
         * Find or create a new console window for DynamoDBLocal's output.
         *
         * @return  the console to log output to
         */
        private MessageConsole findConsole() {
            ConsolePlugin plugin = ConsolePlugin.getDefault();
            IConsoleManager manager = plugin.getConsoleManager();

            IConsole[] existing = manager.getConsoles();
            for (int i = 0; i < existing.length; ++i) {
                if (existing[i].getName().equals("DynamoDB Local")) {
                    return (MessageConsole) existing[i];
                }
            }

            MessageConsole console = new MessageConsole("DynamoDB Local", null);
            manager.addConsoles(new IConsole[] { console });

            return console;
        }

        /**
         * Display the given console if it's not already visible.
         *
         * @param console   the console to display
         */
        private void displayConsole(final IConsole console) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage();

                    try {

                        IConsoleView view = (IConsoleView)
                            page.showView(IConsoleConstants.ID_CONSOLE_VIEW);

                        view.display(console);

                    } catch (PartInitException exception) {
                        // Is there something more intelligent we should be
                        // doing with this?
                        exception.printStackTrace();
                    }
                }
            });
        }
    }
}
