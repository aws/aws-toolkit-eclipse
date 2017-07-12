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

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.services.sns.model.Topic;

public class TopicEditorInput extends AbstractAwsResourceEditorInput {

    private final Topic topic;
    private String name;

    public TopicEditorInput(final Topic topic,
                            final String regionEndpoint,
                            final String accountId) {

        super(regionEndpoint, accountId);
        this.topic = topic;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_TOPIC);
    }

    @Override
    public String getName() {
        if (name == null) {
            name = SNSContentProvider.parseTopicName(topic.getTopicArn());
        }
        return name;
    }

    @Override
    public String getToolTipText() {
        return "Amazon SNS Topic Editor - " + getName();
    }

    public Topic getTopic() {
        return topic;
    }

    @Override
    public String toString() {
        return topic.getTopicArn();
    }

    @Override
    public int hashCode() {
        return topic.getTopicArn().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TopicEditorInput)) {
            return false;
        }

        TopicEditorInput that = (TopicEditorInput) obj;

        return topic.getTopicArn().equals(that.topic.getTopicArn());
    }
}
