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

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.eclipse.explorer.sqs.SQSContentProvider.SQSRootElement;

public class SQSLabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof SQSRootElement) return "Amazon SQS";

        return getExplorerNodeText(element);
    }

    @Override
    public Image getDefaultImage(Object element) {
        if (element instanceof SQSRootElement) {
            return AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_SQS_SERVICE);
        } else {
            return AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_QUEUE);
        }
    }
}
