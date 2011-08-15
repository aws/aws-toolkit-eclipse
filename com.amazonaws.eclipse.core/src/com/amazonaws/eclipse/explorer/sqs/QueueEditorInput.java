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
package com.amazonaws.eclipse.explorer.sqs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;

public class QueueEditorInput extends AbstractAwsResourceEditorInput {
    private final String queueUrl;

    public QueueEditorInput(String queueUrl, String endpoint, String accountId) {
        super(endpoint, accountId);
        this.queueUrl = queueUrl;
    }

    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_QUEUE);
    }

    public String getName() {
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) return queueUrl.substring(index + 1);
        return queueUrl;
    }
    
    public String getQueueUrl() {
        return queueUrl;
    }
    
    public String getToolTipText() {
        return "Amazon SQS Queue Editor - " + getName();
    }
}