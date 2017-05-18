/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.services.s3.model.Bucket;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SelectOrCreateBucketDataModel {
    public static final String P_BUCKET = "bucket";

    public static final Bucket LOADING = new Bucket("Loading...");
    public static final Bucket NONE_FOUND = new Bucket("None found");

    private String bucketName;
    @JsonIgnore
    private Bucket bucket;
    private Boolean createNewBucket;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        Bucket oldValue = this.bucket;
        this.bucket = bucket;
        this.bucketName = bucket.getName();
        this.pcs.firePropertyChange(P_BUCKET, oldValue, bucket);
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Boolean getCreateNewBucket() {
        return createNewBucket;
    }

    public void setCreateNewBucket(Boolean createNewBucket) {
        this.createNewBucket = createNewBucket;
    }
}
