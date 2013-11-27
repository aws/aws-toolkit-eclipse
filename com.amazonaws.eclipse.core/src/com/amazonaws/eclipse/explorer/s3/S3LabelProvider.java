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
package com.amazonaws.eclipse.explorer.s3;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.services.s3.model.Bucket;

public class S3LabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public String getText(Object element) {
        if ( element instanceof Bucket ) {
            return ((Bucket)element).getName();
        }

        if ( element instanceof S3RootElement ) {
            return "Amazon S3";
        }

        return getExplorerNodeText(element);
    }

    /*
     * All elements in the tree right now are buckets, and we want a bucket to
     * represent the root node as well
     */
    @Override
    public Image getDefaultImage(Object element) {
        if ( element instanceof S3RootElement ) {
            return AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_S3_SERVICE);
        } else {
            return AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_BUCKET);
        }
    }

}
