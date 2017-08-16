/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.ec2;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.Image;

import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.AMIS_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.INSTANCES_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.EBS_VIEW_ID;
import static com.amazonaws.eclipse.ec2.Ec2PluginConstants.SECURITY_GROUPS_VIEW_ID;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.ExplorerNode;

class EC2ExplorerNodes {

    public static final ExplorerNode AMIS_NODE =
        new ExplorerNode("Amazon Machine Images (AMIs)", 1,
            loadImage("ami"), newOpenViewAction("Open EC2 AMIs View", AMIS_VIEW_ID));

    public static final ExplorerNode INSTANCES_NODE =
        new ExplorerNode("Instances", 2,
            loadImage("server"), newOpenViewAction("Open EC2 Instances View", INSTANCES_VIEW_ID));

    public static final ExplorerNode EBS_NODE =
        new ExplorerNode("Elastic Block Storage (EBS)", 3,
            loadImage("volume"), newOpenViewAction("Open EC2 Elastic Block Storage View", EBS_VIEW_ID));

    public static final ExplorerNode SECURITY_GROUPS_NODE =
        new ExplorerNode("Security Groups", 4,
            loadImage("shield"), newOpenViewAction("Open EC2 Security Groups View", SECURITY_GROUPS_VIEW_ID));


    private static Image loadImage(String imageId) {
        return Ec2Plugin.getDefault().getImageRegistry().get(imageId);
    }

    private static Action newOpenViewAction(String title, String viewId) {
        OpenViewAction action = new OpenViewAction(viewId);
        action.setText(title);
        return action;
    }
}
