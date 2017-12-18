/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2;

import java.io.File;
import static com.amazonaws.eclipse.core.util.OsPlatformUtils.isWindows;
import static com.amazonaws.eclipse.core.util.OsPlatformUtils.isMac;
import static com.amazonaws.eclipse.core.util.OsPlatformUtils.isLinux;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;

/**
 * Basic utilities for working with platform differences.
 */
public class PlatformUtils {

    private static final String PPK_CONVERTER_EXE = "/lib/PemToPPKConverter.exe";
    private static final Logger logger = Logger.getLogger(PlatformUtils.class.getName());

    /**
     * Returns true if the platform specific SSH client is correctly configured
     * and ready to be used on this system.
     *
     * @return True if the platform specific SSH client is correctly configured
     *         and ready to be used on this system.
     */
    public boolean isSshClientConfigured() {
        if (isWindows()) {
            String puttyPath = Ec2Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_PUTTY_EXECUTABLE);

            // First make sure something is specified...
            if (puttyPath == null || puttyPath.length() == 0) return false;

            // Next make sure it's a file
            if (! new File(puttyPath).isFile()) return false;
        }

        return true;
    }

    /**
     * Opens a shell to the specified host using a platform specific terminal
     * window.
     *
     * @param user
     *            The user to connect to the remote host as.
     * @param host
     *            The remote host to connect to.
     * @param identityFile
     *            The file containing the identity file for the connection.
     * @throws IOException
     *             If any problems are encountered opening the remote shell.
     */
    public void openShellToRemoteHost(String user, String host, String identityFile) throws IOException, InterruptedException, URISyntaxException {

        IPreferenceStore preferenceStore = Ec2Plugin.getDefault().getPreferenceStore();

        String sshOptions = preferenceStore.getString(PreferenceConstants.P_SSH_OPTIONS);
        String sshCommand = preferenceStore.getString(PreferenceConstants.P_SSH_CLIENT);
        sshCommand += " " + sshOptions + " -i " + identityFile + " " + user + "@" + host;

        if (isMac()) {
            URL locationUrl =
                    FileLocator.find(Ec2Plugin.getDefault().getBundle(),new Path("/"), null);
            URL fileUrl = FileLocator.toFileURL(locationUrl);
            executeAsynchronousCommand(new String[] {"osascript", fileUrl.getFile() + "scripts/openMacTerminalShell.scpt", sshCommand});
        } else if (isLinux()) {
            String terminalCommand = preferenceStore.getString(PreferenceConstants.P_TERMINAL_EXECUTABLE);

            executeAsynchronousCommand(new String[] {terminalCommand, "-e", sshCommand});
        } else if (isWindows()) {
            openRemoteShellFromWindows(user, host, identityFile);
        } else {
            String osName = System.getProperty("os.name");

            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to determine what platform '" + osName + "' is.");
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }

    /*
     * Private Interface
     */

    /**
     * Opens a remote shell connection from a windows host as the specified user
     * to the specified host using the OpenSSH identity file to authenticate.
     * This method assumes that the user has already configured their SSH tools
     * (PuTTY), so it doesn't check for that, but it does check to see if the
     * required PuTTY private key exists, and if it doesn't, it will convert it.
     *
     * @param user
     *            The user to log into the remote host as.
     * @param host
     *            The host to connect to.
     * @param identityFile
     *            The OpenSSH private key file that provides the user
     *            passwordless access to the specified host.
     * @throws IOException
     *             If any problems are encountered executing the SSH client.
     */
    private void openRemoteShellFromWindows(String user, String host, String identityFile) throws IOException, InterruptedException, URISyntaxException {
        String puttyExecutable = Ec2Plugin.getDefault().getPreferenceStore().getString(
                PreferenceConstants.P_PUTTY_EXECUTABLE);

        File privateKeyFile = new File(identityFile);
        if (!privateKeyFile.isFile()) {
            throw new IOException("Unable to find the required OpenSSH private key '" + identityFile + "'.");
        }

        String puttyPrivateKeyFile = translateOpenSshPrivateKeyFileToPuttyPrivateKeyFile(identityFile);

        File ppkFile = new File(puttyPrivateKeyFile);
        if (! ppkFile.exists()) {
            executeAsynchronousCommand(new String[] {getPuttyGenConversionExecutable(), "\"" + identityFile + "\"", "\"" + ppkFile.getAbsolutePath() + "\""}).waitFor();
        }

        String[] openShellCommand = new String[] {puttyExecutable, "-ssh", "-i", puttyPrivateKeyFile, user + "@" + host};
        executeAsynchronousCommand(openShellCommand);
    }

    /**
     * Returns the path to the bundled puttygen conversion utility
     * @throws IOException
     */
    private String getPuttyGenConversionExecutable() throws URISyntaxException, IOException {
        URL conversionExe = FileLocator.resolve(FileLocator.find(Ec2Plugin.getDefault().getBundle(), new Path(PPK_CONVERTER_EXE), null));
        return new File(conversionExe.toURI()).getAbsolutePath();
    }

    /**
     * Translates the full path to an OpenSSH private key file to a full path
     * for the corresponding PuTTY private key file.
     *
     * @param identityFile
     *            The full path to an OpenSSH private key file.
     *
     * @return The full path for the corresponding PuTTY private key.
     *
     * @throws IOException
     *             If any problems were encountered translating the OpenSSH
     *             private key file path.
     */
    private String translateOpenSshPrivateKeyFileToPuttyPrivateKeyFile(String identityFile) throws IOException {
        int suffixIndex = identityFile.lastIndexOf(".");
        if (suffixIndex < 0) {
            throw new IOException("Unable to translate '" + identityFile + "' to a PuTTY private key file path.");
        }

        String puttyPrivateKeyFile = identityFile.substring(0, suffixIndex);
        puttyPrivateKeyFile = puttyPrivateKeyFile + ".ppk";

        return puttyPrivateKeyFile;
    }


    /**
     * Executes the specified command array, but does NOT wait for it to finish,
     * therefore no exit code is returned.
     *
     * @param commandArray
     *            The command array to execute.
     * @throws IOException
     *             If there were any problems kicking off the command.
     */
    public Process executeAsynchronousCommand(String[] commandArray) throws IOException {
        String commandString = "";
        for (String command : commandArray) {
            commandString += command + " ";
        }
        logger.info("Executing: " + commandString);

        return Runtime.getRuntime().exec(commandArray);
    }

}
