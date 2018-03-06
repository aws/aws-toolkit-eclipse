/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.core.exceptions;

/**
 * Base exception for all AWS actions.
 */
public class AwsActionException extends RuntimeException {
    private final String actionName;

    public AwsActionException(String actionName, String errorMessage, Throwable e) {
        super(errorMessage, e);
        this.actionName = actionName;
    }

    public String getActionName() {
        return actionName;
    }
}
