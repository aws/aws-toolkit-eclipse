/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.wst.server.core.IServer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.EnvironmentBehavior;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;

public class TerminateEnvironmentJob extends Job {
    private final Environment environment;

    public TerminateEnvironmentJob(Environment environment) {
        super("Stopping AWS Elastic Beanstalk environment " + environment.getEnvironmentName());
        this.environment = environment;

        setProperty(IProgressConstants.ICON_PROPERTY,
            AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON));

        setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId()).getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
        EnvironmentBehavior behavior = (EnvironmentBehavior)environment.getServer().loadAdapter(EnvironmentBehavior.class, monitor);

        final DialogHolder dialogHolder = new DialogHolder();
        Display.getDefault().syncExec(new Runnable() {            
            @Override
            public void run() {
                MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(),
                        "Confirm environment termination", AwsToolkitCore.getDefault().getImageRegistry()
                                .get(AwsToolkitCore.IMAGE_AWS_ICON), "Are you sure you want to terminate the environment "
                                + environment.getEnvironmentName()
                                + "?  All EC2 instances in the environment will be terminated and you will be unable to use "
                                + "this environment again until you have recreated it.", MessageDialog.QUESTION_WITH_CANCEL,
                        new String[] { "OK", "Cancel" }, 1);
                dialogHolder.dialog = dialog;
                dialog.open();
            }
        });
        
        if ( dialogHolder.dialog.getReturnCode() != 0 ) {
            behavior.updateServerState(IServer.STATE_STOPPED);
            return Status.OK_STATUS;
        }
        
        try {
            if (doesEnvironmentExist()) {
                client.terminateEnvironment(new TerminateEnvironmentRequest().withEnvironmentName(environment.getEnvironmentName()));
            }
            
            // It's more correct to set the state to stopping, rather than stopped immediately, 
            // but if we set it to stopping, WTP will block workspace actions waiting for the 
            // environment's state to get updated to stopped.  To prevent this, we stop immediately.
            behavior.updateServerState(IServer.STATE_STOPPED);
            return Status.OK_STATUS;
        } catch (AmazonClientException ace) {
            return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                "Unable to terminate environment " + environment.getEnvironmentName() + " : " + ace.getMessage(), ace);
        }
    }
    
    private boolean doesEnvironmentExist() throws AmazonClientException, AmazonServiceException {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId()).getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());

        List<EnvironmentDescription> environments = client.describeEnvironments().getEnvironments();
        for (EnvironmentDescription env : environments) {
            if (env.getEnvironmentName().equals(environment.getEnvironmentName())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static final class DialogHolder {
        private MessageDialog dialog;               
    }
}
