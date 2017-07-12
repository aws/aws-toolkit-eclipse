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

/**
 * Root element for S3 resources in the resource explorer
 */
public class S3RootElement {

    public static final S3RootElement ROOT_ELEMENT = new S3RootElement();

    private S3RootElement() { }

    @Override
    public String toString() {
        return "Amazon S3";
    }

}
