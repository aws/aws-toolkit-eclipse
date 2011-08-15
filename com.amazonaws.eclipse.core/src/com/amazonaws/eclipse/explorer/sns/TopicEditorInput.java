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
package com.amazonaws.eclipse.explorer.sns;

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.services.sns.model.Topic;

public class TopicEditorInput extends AbstractAwsResourceEditorInput {

    private final Topic topic;

    public TopicEditorInput(Topic topic, String regionEndpoint, String accountId) {
        super(regionEndpoint, accountId);
        this.topic = topic;
    }

    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_TOPIC);
    }

    public String getName() {
        return topic.getTopicArn();
    }

    public String getToolTipText() {
        return "Amazon SNS Topic Editor - " + getName();
    }

    public Topic getTopic() {
        return topic;
    }
}