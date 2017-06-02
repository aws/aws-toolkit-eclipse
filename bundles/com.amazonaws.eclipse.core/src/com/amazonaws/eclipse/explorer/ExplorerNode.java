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
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.plugin.AbstractAwsPlugin;

public class ExplorerNode extends PlatformObject {

    private int sortPriority;
    private final String name;
    private Image image;
    private IAction openAction;

    public ExplorerNode(String name) {
        this.name = name;
    }

    public ExplorerNode(String name, int sortPriority) {
        this(name);
        setSortPriority(sortPriority);
    }

    public ExplorerNode(String name, int sortPriority, Image image) {
        this(name, sortPriority);
        setImage(image);
    }

    public ExplorerNode(String name, int sortPriority, Image image, IAction openAction) {
        this(name, sortPriority, image);
        setOpenAction(openAction);
    }

    public int getSortPriority() {
        return sortPriority;
    }

    public String getName() {
        return name;
    }

    public void setSortPriority(int sortPriority) {
        this.sortPriority = sortPriority;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public void setOpenAction(IAction openAction) {
        this.openAction = openAction;
    }

    public IAction getOpenAction() {
        return openAction;
    }

    @Override
    public String toString() {
        return "ExplorerNode: " + name;
    }

    protected static Image loadImage(final String imageId) {
        return loadImage(AwsToolkitCore.getDefault(), imageId);
    }

    protected static Image loadImage(AbstractAwsPlugin plugin, final String imageId) {
        return plugin.getImageRegistry().get(imageId);
    }
}
