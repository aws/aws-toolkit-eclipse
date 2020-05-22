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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.preferences.PreferenceConstants;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Utilities for executing remote commands on EC2 Instances.
 */
public class RemoteCommandUtils {

    /** The key pair manager to provide key pairs for remote access */
    private static final KeyPairManager keyPairManager = new KeyPairManager();

    /** The interval before retrying a remote command */
    private static final int RETRY_INTERVAL = 5000;

    /** The max number of retries for failed remote commands */
    private static final int MAX_RETRIES = 3;

    /** Shared logger */
    private static final Logger logger = Logger.getLogger(RemoteCommandUtils.class.getName());

    /**
     * Executes the specified command on the specified instance, possibly
     * retrying the command a few times if it initially fails for any reason.
     *
     * @param command
     *            The command to execute.
     * @param instance
     *            The instance to execute the command on.
     *
     * @return A list with one element for each time the command was attempted,
     *         and a description of the attempt (stdout, stderr, exit code).
     *
     * @throws ShellCommandException
     *             If the command is unable to be executed, and fails, even after retrying.
     */
    public List<ShellCommandResults> executeRemoteCommand(String command, Instance instance)
            throws ShellCommandException {

        List<ShellCommandResults> results = new LinkedList<>();
        while (true) {
            logger.info("Executing remote command: " + command);

            ShellCommandResults shellCommandResults = excuteRemoteCommandWithoutRetrying(command, instance);
            results.add(shellCommandResults);

            logger.info(" - exit code " + shellCommandResults.exitCode + "\n");

            if (shellCommandResults.exitCode == 0) {
                return results;
            }

            if (results.size() >= MAX_RETRIES) {
                throw new ShellCommandException("Unable to execute the following command:\n" + command, results);
            }

            try {Thread.sleep(RETRY_INTERVAL);} catch (InterruptedException ie) {}
        }
    }

    /**
     * Copies the specified local file to a specified path on the specified
     * host, possibly retrying if the copy initially failed for any reason.
     *
     * @param localFile
     *            The file to copy.
     * @param remoteFile
     *            The remote location to copy the file to.
     * @param instance
     *            The instance to copy the file to.
     *
     * @throws RemoteFileCopyException
     *             If there were any problems copying the remote file.
     */
    public void copyRemoteFile(String localFile, String remoteFile, Instance instance)
            throws RemoteFileCopyException {

        int totalTries = 0;
        List<RemoteFileCopyResults> allFileCopyAttempts = new ArrayList<>();

        while (true) {
            logger.info("Copying file " + localFile + " to " + instance.getPublicDnsName() + ":" + remoteFile);

            RemoteFileCopyResults fileCopyResults = copyRemoteFileWithoutRetrying(localFile, remoteFile, instance);
            if (fileCopyResults.isSucceeded()) return;

            allFileCopyAttempts.add(fileCopyResults);

            totalTries++;
            if (totalTries > MAX_RETRIES) {
                throw new RemoteFileCopyException(localFile, remoteFile, allFileCopyAttempts);
            }

            try {Thread.sleep(RETRY_INTERVAL);} catch (InterruptedException ie) {}
        }
    }

    /**
     * Executes the specified command locally and waits for it to complete so
     * the exit status can be returned. Not technically a remote command utility
     * method, but here for convenience. If command execution fails, the command
     * will be retried up to three times.
     *
     * @param command
     *            The command to execute.
     * @return The exit status of the command.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public int executeCommand(String command) throws IOException, InterruptedException {
        int retries = 3;

        logger.info("Executing: " + command);

        /*
         * We occasionally get problems logging in after an instance
         * comes up, so we retry the connection a few times after
         * waiting a few seconds between to make sure the EC2
         * firewall has been setup correctly.
         */
        while (true) {
            Process p = Runtime.getRuntime().exec(command);
            int exitCode = p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String errors = "";
            String s;
            while ((s = reader.readLine()) != null) {
                errors += s;
            }

            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = "";
            while ((s = reader.readLine()) != null) {
                output += s;
            }

            logger.info(" - exitCode: " + exitCode + "\n"
                        + " - stderr: " + errors + "\n"
                        + " - stdout: " + output);

            if (exitCode == 0) return 0;

            if (retries-- < 0) {
                throw new IOException("Unable to execute command.  "
                        + "Exit code: " + exitCode
                        + " errors: '" + errors
                        + "', output = '" + output + "'");
            }

            logger.info("Retrying...");
            try {Thread.sleep(RETRY_INTERVAL);} catch(InterruptedException ie) {}
        }
    }


    /*
     * Private Interface
     */

    /**
     * Tries to execute the specified command on the specified host one time and
     * returns the exit code.
     *
     * @param command
     *            The remote command to execute.
     * @param instance
     *            The instance to run the command on.
     *
     * @return A description of the attempt to execute this command (stdout,
     *         stderr, exit code).
     */
    private ShellCommandResults excuteRemoteCommandWithoutRetrying(String command, Instance instance) {
        Session session = null;
        ChannelExec channel = null;

        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();

        try {
            session = createSshSession(instance);

            channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            try (BufferedInputStream in = new BufferedInputStream(channel.getInputStream());
                    BufferedInputStream err = new BufferedInputStream(channel.getErrStream())) {

                channel.connect();

                while (true) {
                    drainInputStream(in, output);
                    drainInputStream(err, errors);

                    if (channel.isClosed()) {
                        return new ShellCommandResults(
                                output.toString(), errors.toString(), channel.getExitStatus());
                    }

                    try {Thread.sleep(1000);} catch (Exception e) {}
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            logger.info(" - output: " + output.toString() + "\n"
                        + " - errors: " + errors.toString());

            try {channel.disconnect();} catch (Exception e) {}
            try {session.disconnect();} catch (Exception e) {}
        }

        // TODO: we're missing error message information from JSchException and IOExcetpion.
        //       we could use ShellCommandException to pass it along...

        return new ShellCommandResults(output.toString(), errors.toString(), 1);
    }

    /**
     * Reads all available data from the specified input stream and writes it to
     * the specified StringBuiler.
     *
     * @param in
     *            The InputStream to read.
     * @param builder
     *            The StringBuilder to write to.
     *
     * @throws IOException
     *             If there were any problems reading from the specified
     *             InputStream.
     */
    private void drainInputStream(InputStream in, StringBuilder builder) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(in);

        byte[] buffer = new byte[1024];

        while (bufferedInputStream.available() > 0) {
            int read = bufferedInputStream.read(buffer, 0, buffer.length);

            if (read > 0) {
                builder.append(new String(buffer, 0, read));
            }
        }
    }

    /**
     * Tries exactly once to copy the specified file to the specified remote
     * location and returns a summary of the attempt to copy the local file to
     * the remote location, indicating if the copy was successful or not.
     *
     * @param localFile
     *            The local file to copy.
     * @param remoteFile
     *            The location to copy the file on the remote host.
     * @param instance
     *            The remote host to copy the file to.
     *
     * @return The results of trying to copy the local file to the remote
     *         location, indicating whether the copy was successful or not as
     *         well as providing information on why it failed if applicable.
     */
    private RemoteFileCopyResults copyRemoteFileWithoutRetrying(String localFile, String remoteFile, Instance instance) {

        RemoteFileCopyResults results = new RemoteFileCopyResults(localFile, remoteFile);
        results.setSucceeded(false);

        Session session = null;
        ChannelExec channel = null;
        try {
            session = createSshSession(instance);

            String command = "scp -p -t " + remoteFile;
            channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);

            try (OutputStream out = channel.getOutputStream();
                InputStream in  = channel.getInputStream();
                InputStream ext = channel.getExtInputStream()) {

                StringBuilder extStringBuilder = new StringBuilder();

                channel.connect();
                if (checkAck(in) != 0) {
                    drainInputStream(ext, extStringBuilder);
                    results.setExternalOutput(extStringBuilder.toString());

                    results.setErrorMessage("Error connecting to the secure channel for file transfer");
                    return results;
                }

                sendFileHeader(localFile, out);
                if (checkAck(in) != 0) {
                    drainInputStream(ext, extStringBuilder);
                    results.setExternalOutput(extStringBuilder.toString());

                    results.setErrorMessage("Error sending file header on the secure channel");
                    return results;
                }

                sendFileData(localFile, out);
                if (checkAck(in) != 0) {
                    drainInputStream(ext, extStringBuilder);
                    results.setExternalOutput(extStringBuilder.toString());

                    results.setErrorMessage("Error sending file data on the secure channel");
                    return results;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            results.setErrorMessage("Unexpected exception: " + e.getMessage());
            results.setError(e);

            return results;
        } finally {
            try {channel.disconnect();} catch (Exception e) {}
            try {session.disconnect();} catch (Exception e) {}
        }

        results.setSucceeded(true);

        return results;
    }

    /**
     * Creates a connected SSH session to the specified host.
     *
     * @param instance
     *            The EC2 instance to connect to.
     *
     * @return The connected session.
     *
     * @throws JSchException
     * @throws IOException
     */
    private Session createSshSession(Instance instance) throws JSchException, IOException {
        String keyPairFilePath = keyPairManager.lookupKeyPairPrivateKeyFile(AwsToolkitCore.getDefault().getCurrentAccountId(), instance.getKeyName());

        if (keyPairFilePath == null) {
            throw new IOException("No private key file found for key " + instance.getKeyName());
        }

        JSch jsch = new JSch();
        jsch.addIdentity(keyPairFilePath);

        /*
         * We use a no-op implementation of a host key repository to ensure that
         * we don't add any hosts to the known_hosts file. Since EC2 hosts are
         * transient by nature and the DNS names are reused, we want to avoid
         * any problems with mismatches in the known_hosts file.
         */
        jsch.setHostKeyRepository(new NullHostKeyRepository());

        /*
         * We could configure a session proxy, but if the user has configured a
         * SOCKS proxy in Eclipse's preferences, we'll automatically use that
         * since Eclipse sets the socksProxyHost system property.
         *
         * We could additionally look for an HTTP/HTTPS proxy and configure
         * that, but it seems fairly unlikely that the vast majority of
         * HTTP/HTTPS proxies will be configured to allow SSH traffic through to
         * a remote host on port 22.
         *
         * If that turns out not to be the case, then it'd be easy to look for
         * an HTTP/HTTPS proxy here and configure the JSch session to use it.
         * I've already tested that it works for an open HTTP proxy.
         */
        String sshUser = Ec2Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SSH_USER);
        Session session = jsch.getSession(sshUser, instance.getPublicDnsName(), 22);

        // We need this avoid being asked to accept the key
        session.setConfig("StrictHostKeyChecking", "no");

        // Make sure Kerberos authentication is disabled
        session.setConfig("GSSAPIAuthentication", "no");

        /*
         * These are necessary to avoid problems with latent connections being
         * closed. This tells JSCH to place a 120 second SO timeout on the
         * underlying socket. When that interrupt is received, JSCH will send a
         * keep alive message. This will repeat up to a 1000 times, which should
         * be more than enough for any long operations to prevent the socket
         * from being closed.
         *
         * SSH has a TCPKeepAlive option, but JSCH doesn't seem to ever check it:
         * session.setConfig("TCPKeepAlive", "yes");
         */
        session.setServerAliveInterval(120 * 1000);
        session.setServerAliveCountMax(1000);
        session.setConfig("TCPKeepAlive", "yes");

        session.connect();

        return session;
    }

    /**
     * Sends the SCP file header for the specified file to the specified output
     * stream.
     *
     * @param localFile
     *            The file to generate the header for.
     * @param out
     *            The output stream to write the header to.
     * @throws IOException
     *             If there are any problems writing the header.
     */
    private void sendFileHeader(String localFile, OutputStream out) throws IOException {
        long filesize = (new File(localFile)).length();
        String command = "C0644 " + filesize + " ";
        command += localFile.substring(localFile.lastIndexOf('/') + 1);
        command += "\n";

        out.write(command.getBytes());
        out.flush();
    }

    /**
     * Writes the contents of the specified file to the specified output stream.
     *
     * @param localFile
     *            The file to write to the output stream.
     * @param out
     *            The output stream to write to.
     *
     * @throws FileNotFoundException
     * @throws IOException
     *             If any problems writing the file contents.
     */
    private void sendFileData(String localFile, OutputStream out)
            throws FileNotFoundException, IOException {

        try (FileInputStream fis = new FileInputStream(localFile)) {
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0)
                    break;
                out.write(buf, 0, len);
            }
        }

        out.write('\0');
        out.flush();
    }

    /**
     * Reads a status byte from the specified input stream and checks its value.
     * If it's an error code, an error message is read from the input stream as
     * well.
     *
     * @param in
     *            The input stream to read from.
     * @return The status code read from the input stream.
     *
     * @throws IOException
     *             If there were any problems reading from the input stream.
     */
    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error
        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            System.out.print(sb.toString());
        }
        return b;
    }

    /**
     * No-op implementation of HostKeyRepository to ensure that we don't store
     * host keys for EC2 hosts since EC2 hosts are transient.
     */
    private class NullHostKeyRepository implements HostKeyRepository {

        @Override
        public void add(HostKey hostkey, UserInfo ui) {}

        @Override
        public int check(String host, byte[] key) {
            return NOT_INCLUDED;
        }

        @Override
        public HostKey[] getHostKey() {
            return null;
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return null;
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return null;
        }

        @Override
        public void remove(String host, String type) {}

        @Override
        public void remove(String host, String type, byte[] key) {}
    }

}
