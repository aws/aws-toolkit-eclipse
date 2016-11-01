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
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;

public class CloudFormationUtils {

    /**
     * Iterate all the existing stacks and return the preferred list of elements which could be converted from StackSummary.
     */
    public static <T> List<T> listExistingStacks(StackSummaryConverter<T> converter) {
        AmazonCloudFormation cloudFormation = AwsToolkitCore.getClientFactory().getCloudFormationClient();

        List<T> newItems = new ArrayList<T>();
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
}
