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
package com.amazonaws.eclipse.cloudformation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.services.cloudformation.model.Tag;

public class CloudFormationUtils {

    /**
     * Iterate all the existing stacks and return the preferred list of elements which could be converted from StackSummary.
     */
    public static <T> List<T> listExistingStacks(StackSummaryConverter<T> converter) {
        return listExistingStacks(RegionUtils.getCurrentRegion().getId(), converter);
    }

    public static List<StackSummary> listExistingStacks(String regionId) {
        return listExistingStacks(regionId, new StackSummaryConverter<StackSummary>() {
            @Override
            public StackSummary convert(StackSummary stack) {
                return stack;
            }
        });
    }

    private static <T> List<T> listExistingStacks(String regionId, StackSummaryConverter<T> converter) {
        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClientByRegion(regionId);

        List<T> newItems = new ArrayList<>();
        ListStacksRequest request = new ListStacksRequest();

        ListStacksResult result = null;
        do {
            result = cloudFormation.listStacks(request);

            for (StackSummary stack : result.getStackSummaries()) {
                if (stack.getStackStatus().equalsIgnoreCase(StackStatus.DELETE_COMPLETE.toString())) continue;
                if (stack.getStackStatus().equalsIgnoreCase(StackStatus.DELETE_IN_PROGRESS.toString())) continue;
                newItems.add(converter.convert(stack));
            }
            request.setNextToken(result.getNextToken());
        } while (result.getNextToken() != null);

        return newItems;
    }

    public interface StackSummaryConverter<T> {
        T convert(StackSummary stack);
    }

    public static List<Tag> getTags(String stackName) {
        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClient();
        try {
            return cloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackName)).getStacks().get(0).getTags();
        } catch (Exception e) {
            CloudFormationPlugin.getDefault().logError(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
