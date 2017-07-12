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
package com.amazonaws.eclipse.core.ui.overview;

import org.eclipse.swt.widgets.Composite;

/**
 * Represents a contributed section in the AWS Toolkit Overview view. Components
 * of the AWS Toolkit for Eclipse can extend this class and use the
 * com.amazonaws.eclipse.core.overview extension point to register their
 * overview section and have it appear in the AWS Toolkit Overview view.
 */
public abstract class OverviewSection {

    /**
     * Marker interface for overview sections that support version 2 of the AWS
     * Toolkit for Eclipse overview page.
     */
    public static interface V2 {}
    
    /** AWS Toolkit for Eclipse UI toolkit for creating links, labels, etc. */ 
    protected final Toolkit toolkit = new Toolkit();

    /** Shared resources for all overview page components (images, fonts, colors, etc) */
    protected OverviewResources resources;

    /**
     * Sets the shared overview page resources for this overview section.
     * 
     * @param resources
     *            The shared overview page resources.
     */
    public void setResources(OverviewResources resources) {
        this.resources = resources;
        toolkit.setResources(resources);
    }
    
    /**
     * The AWS Toolkit Overview view will call this method on each overview
     * section that has registered and overviewSection extension for the
     * com.amazonaws.eclipse.core.overview extension point to allow the
     * implementation to fill in it's content.
     * 
     * @param parent
     *            The parent composite in which this OverviewSection
     *            implementation should create it's content.
     */
    public abstract void createControls(Composite parent);

}
