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
package com.amazonaws.eclipse.elasticbeanstalk;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.amazonaws.eclipse.core.BrowserUtils;

public class OpenElasticBeanstalkConsoleAction implements IObjectActionDelegate {
    private static final String ELASTIC_BEANSTALK_CONSOLE_URL = "https://console.aws.amazon.com/elasticbeanstalk";

    @Override
    public void run(IAction action) {
        BrowserUtils.openExternalBrowser(ELASTIC_BEANSTALK_CONSOLE_URL);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {}
    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {}
}
