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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Volume;

/**
 * Thread for making a service call to EC2 to create a new EBS volume and
 * attach it to a specified instance.
 */
class AttachNewVolumeThread extends Thread {

    /** A shared client factory */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /** The snapshot from which to create the new volume */
    private final String snapshotId;

    /** The instance to attach the new volume */
    private final Instance instance;

    /** The size of the new volume */
    private final int size;

    /** The device the new volume should be attached to */
    private final String device;

    /**
     * Creates a new AttachNewVolumeThread ready to be started to create a
     * new volume and attach it to the specified instance.
     *
     * @param instance
     *            The instance to attach the new volume to.
     * @param size
     *            The size of the new volume (ignored if a snapshot is
     *            specified).
     * @param snapshotId
     *            An ID of a snapshot to create the new volume from.
     * @param device
     *            The device the EBS volume should be attached to on the
     *            remote instance.
     * @param instanceSelectionTable TODO
     */
    public AttachNewVolumeThread(Instance instance, int size, String snapshotId, String device) {
        this.instance = instance;
        this.size = size;
        this.snapshotId = snapshotId;
        this.device = device;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest();
            createVolumeRequest.setAvailabilityZone(instance.getPlacement().getAvailabilityZone());
            // Only set size if we're not using a snapshot
            if (snapshotId == null) createVolumeRequest.setSize(size);
            createVolumeRequest.setSnapshotId(snapshotId);

            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            CreateVolumeResult createVolumeResponse = ec2.createVolume(createVolumeRequest);

            AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest();
            Volume volume = createVolumeResponse.getVolume();
            attachVolumeRequest.setDevice(device);
            attachVolumeRequest.setInstanceId(instance.getInstanceId());
            attachVolumeRequest.setVolumeId(volume.getVolumeId());

            ec2.attachVolume(attachVolumeRequest);
        } catch (Exception e) {
            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to attach new volume: " + e.getMessage());
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
}
