/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation;

import java.util.List;

import com.amazonaws.eclipse.cloudformation.CloudFormationUtils;
import com.amazonaws.eclipse.cloudformation.CloudFormationUtils.StackSummaryConverter;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.cloudformation.model.StackSummary;

public class CloudFormationContentProvider extends AbstractContentProvider {

    public static final class CloudFormationRootElement {
        public static final CloudFormationRootElement ROOT_ELEMENT = new CloudFormationRootElement();
    }

    public static class StackNode extends ExplorerNode {
        private final StackSummary stack;

        public StackNode(StackSummary stack) {
            super(stack.getStackName(), 0,
                loadImage(AwsToolkitCore.IMAGE_STACK), new OpenStackEditorAction(stack.getStackName()));
            this.stack = stack;
        }
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof CloudFormationRootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] {CloudFormationRootElement.ROOT_ELEMENT};
        }

        if (parentElement instanceof CloudFormationRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    List<StackNode> stackNodes = CloudFormationUtils.listExistingStacks(
                            new StackSummaryConverter<StackNode>() {
                                @Override
                                public StackNode convert(StackSummary stack) {
                                    return new StackNode(stack);
                                }
                            });
                    return stackNodes.toArray();
                }
            }.start();
        }
        
        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.CLOUD_FORMATION;
    }

}
