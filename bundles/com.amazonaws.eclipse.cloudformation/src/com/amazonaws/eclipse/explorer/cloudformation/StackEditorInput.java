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

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;

public class StackEditorInput extends AbstractAwsResourceEditorInput {

    private final String stackName;
    private final boolean autoRefresh;

    public StackEditorInput(String stackName, String endpoint, String accountId) {
        this(stackName, endpoint, accountId, false);
    }

    public StackEditorInput(String stackName, String endpoint, String accountId, boolean autoRefresh) {
        super(endpoint, accountId);
        this.stackName = stackName;
        this.autoRefresh = autoRefresh;
    }

    public String getStackName() {
        return stackName;
    }

    @Override
    public String getName() {
        return stackName;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    @Override
    public String getToolTipText() {
        return "Amazon CloudFormation Stack Editor - " + getName();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_STACK);
    }
}