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
import java.util.Collection;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Dialog for editing an Amazon S3 object ACL.
 */
public class EditObjectPermissionsDialog extends EditPermissionsDialog {
    private Collection<S3ObjectSummary> objects;

    public EditObjectPermissionsDialog(Collection<S3ObjectSummary> objects) {
        super();
        this.objects = objects;
    }

    @Override
    protected List<PermissionOption> getPermissionOptions() {
        List<PermissionOption> list = new ArrayList<>();
        list.add(new PermissionOption(Permission.Read,     "Read Data"));
        list.add(new PermissionOption(Permission.ReadAcp,  "Read ACL"));
        list.add(new PermissionOption(Permission.WriteAcp, "Write ACL"));
        return list;
    }

    @Override
    protected String getShellTitle() {
        return "Edit Object Permissions";
    }

    @Override
    protected AccessControlList getAcl() {
        S3ObjectSummary firstObject = objects.iterator().next();

        String bucket = objects.iterator().next().getBucketName();
        AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucket);
        return s3.getObjectAcl(firstObject.getBucketName(), firstObject.getKey());
    }
}
