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
package com.amazonaws.eclipse.explorer.sqs;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.sqs.AmazonSQS;

public class SQSContentProvider extends AbstractContentProvider {

    public static class SQSRootElement {
        public static final SQSRootElement ROOT_ELEMENT = new SQSRootElement();                
    }

    public static class QueueNode extends ExplorerNode {

        private final String queueUrl;

        public QueueNode(String queueUrl) {
            super(parseQueueName(queueUrl), 0,
                loadImage(AwsToolkitCore.IMAGE_QUEUE),
                new OpenQueueEditorAction(queueUrl));
            this.queueUrl = queueUrl;
        }

        public String getQueueUrl() {
            return queueUrl;
        }

        private static String parseQueueName(String queueUrl) {
            int position = queueUrl.lastIndexOf('/');
            if (position > 0) return queueUrl.substring(position + 1);
            else return queueUrl;
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof SQSRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { SQSRootElement.ROOT_ELEMENT };
        }

        if (parentElement instanceof SQSRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonSQS sqs = AwsToolkitCore.getClientFactory().getSQSClient();

                    List<QueueNode> queueNodes = new ArrayList<>();
                    for (String queueUrl : sqs.listQueues().getQueueUrls()) {
                        queueNodes.add(new QueueNode(queueUrl));
                    }

                    return queueNodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.SQS;
    }

}
