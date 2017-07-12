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

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;

public final class BucketEditorInput extends AbstractAwsResourceEditorInput {

    private final String bucketName;

    public BucketEditorInput(String bucketName, String endpoint, String accountId) {
        super(endpoint, accountId);
        this.bucketName = bucketName;
    }

    @Override
    public String getToolTipText() {
        return "Amazon S3 Bucket Editor - " + bucketName;
    }

    @Override
    public String getName() {
        return bucketName;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_BUCKET);
    }
    
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null || !(obj instanceof BucketEditorInput) )
            return false;
        return ((BucketEditorInput) obj).getBucketName().equals(this.getBucketName());
    }        
}
