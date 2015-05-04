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
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SdkCredentialsFileContentMonitorTest {

    private File targetFile;
    private static final long MONITOR_POLLING_INTERVAL_MILLIS = 1000;

    @Before
    public void setup() throws IOException {
        targetFile = File.createTempFile("aws-eclipse-credentials-file-monitor-file", null);
    }

    @Test
    public void testFileChangedCallback() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        SdkCredentialsFileContentMonitor monitor = new SdkCredentialsFileContentMonitor(
                targetFile, MONITOR_POLLING_INTERVAL_MILLIS, new FileAlterationListenerAdaptor() {

                    @Override
                    public void onFileChange(final File changedFile) {
                        latch.countDown();
                    }
                });
        monitor.setDebugMode(true);
        monitor.start();

        touch(targetFile);

        long waitTime = MONITOR_POLLING_INTERVAL_MILLIS * 2;
        Assert.assertTrue(
                "File monitor callback not invoked after waiting for " + waitTime + " ms.",
                latch.await(waitTime, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testMonitorInStoppedStatus() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);

        SdkCredentialsFileContentMonitor monitor = new SdkCredentialsFileContentMonitor(
                targetFile, MONITOR_POLLING_INTERVAL_MILLIS, new FileAlterationListenerAdaptor() {

                    @Override
                    public void onFileChange(final File changedFile) {
                        System.err.println("stopped");
                        latch.countDown();
                    }
                });
        monitor.setDebugMode(true);
        monitor.start();
        monitor.stop();

        touch(targetFile);

        long waitTime = MONITOR_POLLING_INTERVAL_MILLIS * 2;
        Assert.assertFalse(
                "Who counted it down to zero???",
                latch.await(waitTime, TimeUnit.MILLISECONDS));
    }

    private void touch(File file) {
        file.setLastModified(System.currentTimeMillis() / 1000);
    }
}
