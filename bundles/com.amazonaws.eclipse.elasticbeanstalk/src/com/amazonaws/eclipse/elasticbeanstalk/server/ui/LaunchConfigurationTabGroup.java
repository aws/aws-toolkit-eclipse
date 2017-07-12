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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;

public class LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[1];
        tabs[0] = new ServerLaunchConfigurationTab(new String[] { ElasticBeanstalkPlugin.TOMCAT_6_SERVER_TYPE_ID, ElasticBeanstalkPlugin.TOMCAT_7_SERVER_TYPE_ID });
        tabs[0].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}
