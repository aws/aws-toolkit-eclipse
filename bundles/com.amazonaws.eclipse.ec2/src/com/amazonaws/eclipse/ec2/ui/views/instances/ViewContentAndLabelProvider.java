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

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;

/**
 * Label and content provider for the EC2 Instance table.
 */
class ViewContentAndLabelProvider extends BaseLabelProvider
        implements ITreeContentProvider, ITableLabelProvider {

    static final int INSTANCE_ID_COLUMN = 0;
    static final int INSTANCE_NAME_COLUMN = 1;
    static final int PUBLIC_DNS_COLUMN = 2;
    static final int IMAGE_ID_COLUMN = 3;
    static final int ROOT_DEVICE_COLUMN = 4;
    static final int STATE_COLUMN = 5;
    static final int INSTANCE_TYPE_COLUMN = 6;
    static final int AVAILABILITY_ZONE_COLUMN = 7;
    static final int KEY_NAME_COLUMN = 8;
    static final int LAUNCH_TIME_COLUMN = 9;
    static final int SECURITY_GROUPS_COLUMN = 10;
    static final int TAGS_COLUMN = 11;

    private final DateFormat dateFormat;
    private KeyPairManager keyPairManager = new KeyPairManager();

    /** The input to be displayed by this content / label provider */
    private InstancesViewInput instancesViewInput;

    /** Map of instance states to images representing those states */
    private static final Map<String, Image> stateImageMap = new HashMap<>();

    static {
        stateImageMap.put("running", Ec2Plugin.getDefault().getImageRegistry().get("status-running"));
        stateImageMap.put("rebooting", Ec2Plugin.getDefault().getImageRegistry().get("status-rebooting"));
        stateImageMap.put("shutting-down", Ec2Plugin.getDefault().getImageRegistry().get("status-waiting"));
        stateImageMap.put("pending", Ec2Plugin.getDefault().getImageRegistry().get("status-waiting"));
        stateImageMap.put("stopping", Ec2Plugin.getDefault().getImageRegistry().get("status-waiting"));
        stateImageMap.put("stopped", Ec2Plugin.getDefault().getImageRegistry().get("status-terminated"));
        stateImageMap.put("terminated", Ec2Plugin.getDefault().getImageRegistry().get("status-terminated"));
    }

    /** Default constructor */
    ViewContentAndLabelProvider() {
        dateFormat = DateFormat.getDateTimeInstance();
    }


    /*
     * IStructuredContentProvider Interface
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        instancesViewInput = (InstancesViewInput)newInput;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
     */
    @Override
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object parent) {
        if (instancesViewInput == null) {
            return new Object[0];
        }

        return instancesViewInput.instances.toArray();
    }


    /*
     * ITableLabelProvider Interface
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    @Override
    public String getColumnText(Object obj, int index) {
        Instance instance = (Instance)obj;

        switch (index) {
        case INSTANCE_ID_COLUMN:
            return instance.getInstanceId();
        case INSTANCE_NAME_COLUMN:
            return getInstanceName(instance);
        case PUBLIC_DNS_COLUMN:
            return instance.getPublicDnsName();
        case ROOT_DEVICE_COLUMN:
            return instance.getRootDeviceType();
        case STATE_COLUMN:
            return instance.getState().getName();
        case INSTANCE_TYPE_COLUMN:
            return instance.getInstanceType().toString();
        case AVAILABILITY_ZONE_COLUMN:
            return instance.getPlacement().getAvailabilityZone();
        case IMAGE_ID_COLUMN:
            return instance.getImageId();
        case KEY_NAME_COLUMN:
            return instance.getKeyName();
        case LAUNCH_TIME_COLUMN:
            if ( instance.getLaunchTime() == null )
                return "";
            return dateFormat.format(instance.getLaunchTime());
        case SECURITY_GROUPS_COLUMN:
            return formatSecurityGroups(instancesViewInput.securityGroupMap.get(instance.getInstanceId()));
        case TAGS_COLUMN:
            return TagFormatter.formatTags(instance.getTags());
        default:
            return "???";
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    @Override
    public Image getColumnImage(Object obj, int index) {

        Instance instance = (Instance)obj;

        switch (index) {
        case INSTANCE_ID_COLUMN:
            return Ec2Plugin.getDefault().getImageRegistry().get("server");
        case KEY_NAME_COLUMN:
            if (keyPairManager.isKeyPairValid(AwsToolkitCore.getDefault().getCurrentAccountId(), instance.getKeyName())) {
                return Ec2Plugin.getDefault().getImageRegistry().get("check");
            } else {
                return Ec2Plugin.getDefault().getImageRegistry().get("error");
            }

        case STATE_COLUMN:
            String state = instance.getState().getName().toLowerCase();
            return stateImageMap.get(state);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object obj) {
        return null;
    }

    /*
     * Package Interface
     */

    /**
     * Provides access to the instance -> security groups mapping contained in
     * this class. This should probably be shifted around into another location,
     * but for now we'll provide this data from here. If the amount of other
     * data we need to associated with the Instance datatype grows a lot,
     * we should definitely clean this up so that other objects can more easily
     * access this data.
     *
     * @param instanceId
     *            The ID of the instance to look up.
     *
     * @return A list of the security groups the specified instance is in.
     */
    List<String> getSecurityGroupsForInstance(String instanceId) {
        if (instancesViewInput == null) {
            return null;
        }

        return instancesViewInput.securityGroupMap.get(instanceId);
    }


    /*
     * Private Interface
     */

    /**
     * Takes the list of security groups and turns it into a comma separated
     * string list.
     *
     * @param securityGroups
     *            A list of security groups to turn into a comma separated
     *            string list.
     *
     * @return A comma separated list containing the contents of the specified
     *         list of security groups.
     */
    private String formatSecurityGroups(List<String> securityGroups) {
        if (securityGroups == null) return "";

        String allSecurityGroups = "";
        for (String securityGroup : securityGroups) {
            if (allSecurityGroups.length() > 0) allSecurityGroups += ", ";
            allSecurityGroups += securityGroup;
        }

        return allSecurityGroups;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return false;
    }

    private String getInstanceName(Instance instance) {
        List<Tag> tags = instance.getTags();
        if (tags != null && !tags.isEmpty()) {
            for (Tag tag : tags) {
                if (tag.getKey().equals("Name")) {
                    return tag.getValue();
                }
            }
        }
        return "";
    }
}

/**
 * Simple container for the instances and security group mappings displayed
 * by the viewer associated with this content/label provider.
 */
class InstancesViewInput {
    /** The EC2 instances being displayed by this viewer */
    public final List<Instance> instances;

    /** A map of instance ids -> security groups */
    public final Map<String, List<String>> securityGroupMap;

    /**
     * Constructs a new InstancesViewInput object with the specified list of
     * instances and mapping of instances to security groups.
     *
     * @param instances
     *            A list of the instances that should be displayed.
     * @param securityGroupMap
     *            A map of instance ids to the list of security groups in which
     *            that instance was launched.
     */
    public InstancesViewInput(final List<Instance> instances, final Map<String, List<String>> securityGroupMap) {
        this.instances = instances;
        this.securityGroupMap = securityGroupMap;
    }
}
