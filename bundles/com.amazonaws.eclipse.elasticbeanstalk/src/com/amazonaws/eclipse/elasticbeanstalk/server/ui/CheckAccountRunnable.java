/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;

public final class CheckAccountRunnable implements IRunnableWithProgress {
    private final AWSElasticBeanstalk client;
    public CheckAccountRunnable(AWSElasticBeanstalk client) {
        this.client = client;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            monitor.beginTask("Checking connection to AWS Elastic Beanstalk...", IProgressMonitor.UNKNOWN);
            client.listAvailableSolutionStacks();
            client.describeEnvironments();
        } finally {
            monitor.done();
        }
    }
}
