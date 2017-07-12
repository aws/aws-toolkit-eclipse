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
package com.amazonaws.eclipse.explorer.s3.acls;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Permission;

/**
 * Dialog for editing an Amazon S3 bucket ACL.
 */
public class EditBucketPermissionsDialog extends EditPermissionsDialog {
    private Bucket bucket;

    public EditBucketPermissionsDialog(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    protected String getShellTitle() {
        return "Edit Bucket Permissions";
    }

    @Override
    protected List<PermissionOption> getPermissionOptions() {
        List<PermissionOption> list = new ArrayList<>();
        list.add(new PermissionOption(Permission.Read,     "List Contents"));
        list.add(new PermissionOption(Permission.Write,    "Edit Contents"));
        list.add(new PermissionOption(Permission.ReadAcp,  "Read ACL"));
        list.add(new PermissionOption(Permission.WriteAcp, "Write ACL"));
        return list;
    }

    @Override
    protected AccessControlList getAcl() {
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucket.getName());
        return s3.getBucketAcl(bucket.getName());
    }
}
