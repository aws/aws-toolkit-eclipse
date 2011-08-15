/*
 * Copyright 2011 Amazon Technologies, Inc.
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

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.ExplorerNode;

class EC2ExplorerNodes {

    public static final ExplorerNode AMIS_NODE =
        new ExplorerNode("Amazon Machine Images (AMIs)", 1,
            loadImage("ami"), newOpenViewAction("Open EC2 AMIs View", "com.amazonaws.eclipse.ec2.ui.views.AmiBrowserView"));

    public static final ExplorerNode INSTANCES_NODE =
        new ExplorerNode("Instances", 2,
            loadImage("server"), newOpenViewAction("Open EC2 Instances View", "com.amazonaws.eclipse.ec2.ui.views.InstanceView"));

    public static final ExplorerNode EBS_NODE =
        new ExplorerNode("Elastic Block Storage (EBS)", 3,
            loadImage("volume"), newOpenViewAction("Open EC2 Elastic Block Storage View", "com.amazonaws.eclipse.ec2.ui.views.ElasticBlockStorageView"));

    public static final ExplorerNode SECURITY_GROUPS_NODE =
        new ExplorerNode("Security Groups", 4,
            loadImage("shield"), newOpenViewAction("Open EC2 Security Groups View", "com.amazonaws.eclipse.ec2.views.SecurityGroupView"));


    private static Image loadImage(String imageId) {
        return Ec2Plugin.getDefault().getImageRegistry().get(imageId);
    }

    private static Action newOpenViewAction(String title, String viewId) {
        OpenViewAction action = new OpenViewAction(viewId);
        action.setText(title);
        return action;
    }
}