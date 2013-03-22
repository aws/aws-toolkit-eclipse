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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;

/**
 * Thread for making a service call to EC2 to list available EBS volumes and
 * add them to the specified menu based on whether they can be detached or
 * attached to the specified instance.
 */
class PopulateEbsMenuThread extends Thread {

    /** The menu to add items to */
    private final MenuManager menu;
    /** The instance to acted on by menu items */
    private final Instance instance;

    /** A shared client factory */
    private static AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /**
     * Creates a new thread ready to be started to populate the specified
     * menu with actions to attach or detach EBS volumes to the specified
     * instance.
     *
     * @param instance
     *            The instance to detach or attach EBS volumes to.
     * @param menu
     *            The menu to add menu items to.
     * @param instanceSelectionTable TODO
     */
    public PopulateEbsMenuThread(final Instance instance, final MenuManager menu) {
        this.instance = instance;
        this.menu = menu;
    }

    /* (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            List<Volume> volumes = ec2.describeVolumes(new DescribeVolumesRequest()).getVolumes();

            for (Volume volume : volumes) {
                String status = volume.getState();

                // We don't want to allow users to attach any volumes that aren't
                // available or are in different availability zones.
                if (!status.equalsIgnoreCase("available")) continue;
                if (!volume.getAvailabilityZone().equalsIgnoreCase(instance.getPlacement().getAvailabilityZone())) continue;

                menu.add(new AttachVolumeAction(instance, volume));
            }

            menu.add(new Separator());

            for (Volume volume : volumes) {
                for (VolumeAttachment attachmentInfo : volume.getAttachments()) {
                    String instanceId = attachmentInfo.getInstanceId();

                    if (!instanceId.equals(instance.getInstanceId())) continue;

                    menu.add(new DetachVolumeAction(volume, instance));
                }
            }
        } catch (Exception e) {
            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to query EBS volumes: " + e.getMessage());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
    }
}
