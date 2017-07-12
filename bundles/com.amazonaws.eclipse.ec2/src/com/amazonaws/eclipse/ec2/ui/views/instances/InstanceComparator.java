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

import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.ui.SelectionTableComparator;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Comparator for sorting the instances by any column.
 */
class InstanceComparator extends SelectionTableComparator {

    /**
     * 
     */
    private final InstanceSelectionTable instanceSelectionTable;

    /**
     * @param defaultColumn
     * @param instanceSelectionTable TODO
     */
    public InstanceComparator(InstanceSelectionTable instanceSelectionTable, int defaultColumn) {
        super(defaultColumn);
        this.instanceSelectionTable = instanceSelectionTable;
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.views.instances.SelectionTableComparator#compareIgnoringDirection(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    protected int compareIgnoringDirection(Viewer viewer, Object e1, Object e2) {
        if (!(e1 instanceof Instance && e2 instanceof Instance)) {
            return 0;
        }

        Instance i1 = (Instance)e1;
        Instance i2 = (Instance)e2;

        switch (this.sortColumn) {
        case ViewContentAndLabelProvider.INSTANCE_ID_COLUMN:
            return (i1.getInstanceId().compareTo(i2.getInstanceId()));
        case ViewContentAndLabelProvider.PUBLIC_DNS_COLUMN:
            return (i1.getPublicDnsName().compareTo(i2.getPublicDnsName()));
        case ViewContentAndLabelProvider.ROOT_DEVICE_COLUMN:
            return (i1.getRootDeviceType().compareTo(i2.getRootDeviceType()));            
        case ViewContentAndLabelProvider.STATE_COLUMN:
            return (i1.getState().getName().compareTo(i2.getState().getName()));
        case ViewContentAndLabelProvider.INSTANCE_TYPE_COLUMN:
            return (i1.getInstanceType().compareTo(i2.getInstanceType()));
        case ViewContentAndLabelProvider.AVAILABILITY_ZONE_COLUMN:
            return (i1.getPlacement().getAvailabilityZone().compareTo(i2.getPlacement().getAvailabilityZone()));
        case ViewContentAndLabelProvider.IMAGE_ID_COLUMN:
            return (i1.getImageId().compareTo(i2.getImageId()));
        case ViewContentAndLabelProvider.KEY_NAME_COLUMN:
            String k1 = i1.getKeyName();
            String k2 = i2.getKeyName();
            if (k1 == null)
                k1 = "";
            if (k2 == null)
                k2 = "";
            return k1.compareTo(k2);
        case ViewContentAndLabelProvider.LAUNCH_TIME_COLUMN:
            return (i1.getLaunchTime().compareTo(i2.getLaunchTime()));
        case ViewContentAndLabelProvider.SECURITY_GROUPS_COLUMN:
            return compareSecurityGroups(i1, i2);
        case ViewContentAndLabelProvider.TAGS_COLUMN:
            return TagFormatter.formatTags(i1.getTags()).compareTo(
                    TagFormatter.formatTags(i2.getTags()));
        default:
            return 0;
        }
    }

    /**
     * Compares the security groups for the specified instances and returns
     * a -1, 0, or 1 depending on the comparison. See compareTo() for more
     * details on the returned value.
     *
     * @param i1
     *            The first instance to compare.
     * @param i2
     *            The second instance to compare.
     *
     * @return -1, 0, or 1 depending on the comparison of the security
     *         groups in the specified instances.
     */
    private int compareSecurityGroups(Instance i1, Instance i2) {
        List<String> groups1 = this.instanceSelectionTable.contentAndLabelProvider.getSecurityGroupsForInstance(i1.getInstanceId());
        List<String> groups2 = this.instanceSelectionTable.contentAndLabelProvider.getSecurityGroupsForInstance(i2.getInstanceId());

        String formattedList1 = formatSecurityGroups(groups1);
        String formattedList2 = formatSecurityGroups(groups2);

        return formattedList1.compareTo(formattedList2);
    }

    /**
     * Formats a list of security groups as a string for easy comparison.
     * It's assumed that the list of security groups is already sorted.
     *
     * @param securityGroupList
     *            The list to format as a single string.
     *
     * @return A single string containing the specified list of security
     *         groups.
     */
    private String formatSecurityGroups(List<String> securityGroupList) {
        if (securityGroupList == null) return "";

        String formattedList = "";
        for (String group : securityGroupList) {
            if (formattedList.length() > 0) {
                formattedList += ", ";
            }

            formattedList += group;
        }

        return formattedList;
    }

}
