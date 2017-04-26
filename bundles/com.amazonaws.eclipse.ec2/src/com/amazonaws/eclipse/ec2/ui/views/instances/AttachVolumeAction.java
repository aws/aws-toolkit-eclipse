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
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Volume;

/**
 * Action subclass which, when run, will attach a specified volume to a
 * specified instance.
 */
class AttachVolumeAction extends Action {

    /** The volume being attached to an instance */
    private final Volume volume;

    /** The instance to which a volume is being attached */
    private final Instance instance;

    /** A shared client factory */
    private final AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory();

    /**
     * Creates a new action which, when run, will attach the specified volume to
     * the specified instance.
     *
     * @param instance
     *            The instance to attach the volume to.
     * @param volume
     *            The volume to attach.
     */
    public AttachVolumeAction(Instance instance, Volume volume) {
        this.instance = instance;
        this.volume = volume;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        if (!volume.getState().equalsIgnoreCase("available")) return false;

        if (!instance.getState().getName().equalsIgnoreCase("running")) return false;

        String volumeZone = volume.getAvailabilityZone();
        String instanceZone = instance.getPlacement().getAvailabilityZone();
        if (!volumeZone.equalsIgnoreCase(instanceZone)) return false;

        return super.isEnabled();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        DeviceDialog deviceDialog = new DeviceDialog();
        if (deviceDialog.open() == DeviceDialog.CANCEL) {
            return;
        }

        try {
            AttachVolumeRequest request = new AttachVolumeRequest();
            request.setDevice(deviceDialog.getDevice());
            request.setInstanceId(instance.getInstanceId());
            request.setVolumeId(volume.getVolumeId());
            AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();
            ec2.attachVolume(request);
        } catch (Exception e) {
            Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to attach volume: " + e.getMessage());
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Attach " + volume.getVolumeId() + " (" + volume.getSize() + " GB) ...";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("volume");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return "Attach an Elastic Block Storage volume to this instance";
    }

}
