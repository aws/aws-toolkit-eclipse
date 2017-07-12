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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;

public class SNSContentProvider extends AbstractContentProvider {

    /**
     * Parse a topic name from a topic ARN, for friendlier display in
     * the UI.
     *
     *  @param topicARN the ARN of the topic
     *  @return the user-assigned name of the topic
     */
    public static String parseTopicName(final String topicARN) {
        int index = topicARN.lastIndexOf(':');
        if (index > 0) {
            return topicARN.substring(index + 1);
        }
        return topicARN;
    }

    public static class SNSRootElement {
        public static final SNSRootElement ROOT_ELEMENT = new SNSRootElement();
    }

    public static class TopicNode extends ExplorerNode {
        private final Topic topic;

        public TopicNode(Topic topic) {
            super(parseTopicName(topic.getTopicArn()),
                  0,
                  loadImage(AwsToolkitCore.IMAGE_TOPIC),
                  new OpenTopicEditorAction(topic));
            this.topic = topic;
        }

        public Topic getTopic() {
            return topic;
        }
    }


    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof SNSRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { SNSRootElement.ROOT_ELEMENT };
        }

        if (parentElement instanceof SNSRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonSNS sns = AwsToolkitCore.getClientFactory().getSNSClient();

                    List<TopicNode> topicNodes = new ArrayList<>();
                    ListTopicsResult listTopicsResult = null;

                    do {
                        if (listTopicsResult == null) {
                            listTopicsResult = sns.listTopics();
                        } else {
                            listTopicsResult = sns.listTopics(
                                new ListTopicsRequest(listTopicsResult.getNextToken()));
                        }

                        for (Topic topic : listTopicsResult.getTopics()) {
                            topicNodes.add(new TopicNode(topic));
                        }
                    } while (listTopicsResult.getNextToken() != null);

                    return topicNodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.SNS;
    }
}
