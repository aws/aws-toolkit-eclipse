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
package com.amazonaws.eclipse.explorer.sns;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.sns.model.Topic;

public class OpenTopicEditorAction extends Action {
    private final Topic topic;

    public OpenTopicEditorAction(Topic topic) {
        this.setText("Open in SNS Topic Editor");
        this.topic = topic;
    }

    @Override
    public void run() {
        String endpoint = RegionUtils.getCurrentRegion().getServiceEndpoints()
            .get(ServiceAbbreviations.SNS);

        String accountId = AwsToolkitCore.getDefault().getCurrentAccountId();
        final IEditorInput input = new TopicEditorInput(topic, endpoint, accountId);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWindow.getActivePage().openEditor(input, "com.amazonaws.eclipse.explorer.sns.topicEditor");
                } catch (PartInitException e) {
                    AwsToolkitCore.getDefault().reportException("Unable to open the Amazon SNS topic editor", e);
                }
            }
        });
    }
}
