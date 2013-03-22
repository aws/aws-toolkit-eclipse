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
package com.amazonaws.eclipse.explorer;

import org.eclipse.core.runtime.PlatformObject;

/**
 * Simple marker object for asynchronous tree updates.
 */
public class Loading extends PlatformObject {

    public static final Object[] LOADING = new Object[] { new Loading() };
    
    private Loading() { }
    
    @Override
    public String toString() {
        return "Loading...";
    }
}
