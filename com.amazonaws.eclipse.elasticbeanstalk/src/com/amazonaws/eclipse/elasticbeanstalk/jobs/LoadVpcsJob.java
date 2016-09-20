/*
 * Copyright 2016 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.jobs;

import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Vpc;

public final class LoadVpcsJob extends LoadResourcesJob<Vpc> {

    private final AmazonEC2 ec2;

    public LoadVpcsJob(Region region, LoadResourcesCallback<Vpc> callback) {
        super("Load Vpcs", callback);
        ec2 = AwsToolkitCore.getClientFactory().getEC2ClientByEndpoint(
                region.getServiceEndpoint(ServiceAbbreviations.EC2));
    }

    @Override
    protected List<Vpc> getAllResources() {
        return ec2.describeVpcs().getVpcs();
    }

    @Override
    protected boolean isInsufficientPermissions(Exception e) {
        // TODO we don't tell apart InsufficientPermissionException and other Exceptions for now.
        return false;
    }

    @Override
    protected String getOnFailureMessage() {
        return "Unable to query AWS EC2 for the VPCs";
    }

}
