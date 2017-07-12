/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;

/**
 * Thread for making a service call to EC2 to reboot a list of instances.
 */
class RebootInstancesThread extends Thread {

    private final InstanceSelectionTable instanceSelectionTable;
    /** The instances to be rebooted */
    private final List<Instance> instances;

    /** A shared client factory */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /**
     * Creates a new RebootInstancesThread ready to be started and reboot
     * the specified instances.
     *
     * @param instances
     *            The instances to reboot.
     */
    public RebootInstancesThread(InstanceSelectionTable instanceSelectionTable, final List<Instance> instances) {
        this.instanceSelectionTable = instanceSelectionTable;
        this.instances = instances;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            List<String> instanceIds = new ArrayList<>();
            for (Instance instance : instances) {
                instanceIds.add(instance.getInstanceId());
            }

            RebootInstancesRequest request = new RebootInstancesRequest();
            request.setInstanceIds(instanceIds);

            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            ec2.rebootInstances(request);
            this.instanceSelectionTable.refreshInstances();
        } catch (Exception e) {
            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to reboot instance: " + e.getMessage());
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
}
