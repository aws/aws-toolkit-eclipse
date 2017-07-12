/*
 * Copyright 2015 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.accounts.profiles;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;

/**
 * Used to monitor a specific credentials file and prompts the user to reload
 * the credentials whenever the file content is changed.
 */
public class SdkCredentialsFileContentMonitor {

    private static final Log debugLogger = LogFactory.getLog(SdkCredentialsFileContentMonitor.class);

    private final static long DEFAULT_POLLING_INTERVAL_MILLIS = 3 * 1000;

    private final File _target;
    private final FileAlterationObserver _observer;
    private final FileAlterationMonitor _monitor;
    private final FileAlterationListener _listener;

    private boolean debugMode = false;

    public SdkCredentialsFileContentMonitor(File target) {
        this(target, DEFAULT_POLLING_INTERVAL_MILLIS);
    }

    public SdkCredentialsFileContentMonitor(File target, long pollingIntervalInMs) {

        this(target, pollingIntervalInMs, new FileAlterationListenerAdaptor() {

            @Override
            public void onFileChange(final File changedFile) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (AwsToolkitCore.getDefault().getPreferenceStore()
                                .getBoolean(PreferenceConstants.P_ALWAYS_RELOAD_WHEN_CREDNENTIAL_PROFILE_FILE_MODIFIED)) {
                            AwsToolkitCore.getDefault().getAccountManager().reloadAccountInfo();
                        } else {
                            showCredentialsReloadConfirmBox(changedFile);
                        }
                    }
                });
            }
        });
    }

    public SdkCredentialsFileContentMonitor(
            File target,
            long pollingIntervalInMillis,
            FileAlterationListener listener) {

        _target = target;

        // IllegalArgumentException is expected if target.getParentFile == null
        _observer = new FileAlterationObserver(target.getParentFile(), new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.equals(_target);
            }
        });

        _monitor = new FileAlterationMonitor(pollingIntervalInMillis);
        _listener = listener;

        _observer.addListener(_listener);
        _monitor.addObserver(_observer);

        // Use daemon thread to avoid thread leakage
        _monitor.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                t.setName("aws-credentials-file-monitor-thread");
                return t;
            }
        });
    }

    public void start() {
        try {
            _monitor.start();
            logInfo("Monitoring content of " + _target.getAbsolutePath());
        } catch (Exception e) {
            logException("Unable to start file monitor on " +
                         _target.getAbsolutePath(), e);
        }
    }

    public void stop() {
        try {
            _monitor.stop();
            logInfo("Stopped monitoring content of " + _target.getAbsolutePath());
        } catch (Exception e) {
            logException("Unable to stop the file monitor on " +
                         _target.getAbsolutePath(), e);
        }
    }

    /**
     * Should only be invoked in the main thread
     */
    private static void showCredentialsReloadConfirmBox(File credentialsFile) {

        String message =
                "The AWS credentials file '" + credentialsFile.getAbsolutePath() +
                "' has been changed in the file system. " +
                "Do you want to reload the credentials from the updated file content?";

        MessageDialog dialog = new MessageDialog(
                null, // use the top-level shell
                "AWS Credentials File Changed",
                AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                message,
                MessageDialog.CONFIRM,
                new String[] { "No", "Yes", "Always Reload" },
                1 // default to YES
                );

        int result = dialog.open();
        if (result == 2) {
            AwsToolkitCore.getDefault().getPreferenceStore()
                    .setValue(
                            PreferenceConstants.P_ALWAYS_RELOAD_WHEN_CREDNENTIAL_PROFILE_FILE_MODIFIED,
                            true);
        }
        if (result == 1 || result == 2) {
            AwsToolkitCore.getDefault().getAccountManager().reloadAccountInfo();
        }
    }

    /**
     * For testing purpose only.
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    private void logInfo(String info) {
        if (debugMode) {
            debugLogger.debug(info);
        } else {
            AwsToolkitCore.getDefault().logInfo(info);
        }
    }

    private void logException(String msg, Exception e) {
        if (debugMode) {
            debugLogger.debug(msg, e);
        } else {
            AwsToolkitCore.getDefault().logError(msg, e);
        }
    }
}
