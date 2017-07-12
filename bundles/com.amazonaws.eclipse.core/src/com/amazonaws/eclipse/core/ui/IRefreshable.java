/*
 * Copyright 2008-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.core.ui;

/**
 * Simple interface to tag controls so that callers can request they refresh the
 * data they are displaying.
 */
public interface IRefreshable {
    
    /**
     * Refreshes the data displayed by this IRefreshable control.  
     */
    public void refreshData();
    
}
