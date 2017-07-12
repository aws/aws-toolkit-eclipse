/*
 * Copyright 2009-2012 Amazon Technologies, Inc. 
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Command Handler to open AWS Toolkit Overview Editor.
 */
public class DisplayAwsOverviewEditorCmdHandler extends AbstractHandler {
    
    /* 
     * @see org.eclipse.core.commands.IHandler#execute(ExecutionEvent)
     */    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Startup.displayAwsToolkitOverviewEditor();
        return null;
    }
}
