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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;

/**
 * Action to detach a volume from a specified instance.
 */
class DetachVolumeAction extends Action {

    /** The volume that will be detached */
    private final Volume volume;

    /** The instance to attach the specified volume to */
    private final Instance instance;

    /** Shared client factory */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /**
     * Creates a new detach volume action ready to be run.
     *
     * @param volume
     *            The volume that will be detached when this action is
     *            run.
     * @param instance
     *            The instance from which to detach the specified volume.
     */
    public DetachVolumeAction(Volume volume, Instance instance) {
        this.volume = volume;
        this.instance = instance;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        String selectedInstanceId = instance.getInstanceId();

        for (VolumeAttachment attachmentInfo : volume.getAttachments()) {
            String instanceId = attachmentInfo.getInstanceId();
            String volumeId = attachmentInfo.getVolumeId();
            String device = attachmentInfo.getDevice();

            // We want to ensure that we only detach the specified instance
            if (!instanceId.equals(selectedInstanceId)) continue;

            try {
                DetachVolumeRequest request = new DetachVolumeRequest();
                request.setVolumeId(volumeId);
                request.setInstanceId(instanceId);
                request.setDevice(device);
                request.setForce(true);

                AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
                ec2.detachVolume(request);
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to detach volume: " + e.getMessage());
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Detach " + volume.getVolumeId() + " (" + volume.getSize() + " GB)";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return "Detach volume " + volume.getVolumeId() + " from selected instance " + instance.getInstanceId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("volume");
    }
}
