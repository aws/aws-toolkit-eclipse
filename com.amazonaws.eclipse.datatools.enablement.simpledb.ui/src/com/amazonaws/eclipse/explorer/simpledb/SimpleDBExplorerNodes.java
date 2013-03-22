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
package com.amazonaws.eclipse.explorer.simpledb;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor.OpenQueryEditorAction;
import com.amazonaws.eclipse.explorer.ExplorerNode;

public class SimpleDBExplorerNodes {

    public static class DomainNode extends ExplorerNode {

        public DomainNode(final String domainName) {
            super(domainName, 0, loadImage(AwsToolkitCore.IMAGE_TABLE), new OpenQueryEditorAction(domainName));
        }
    }

    private static Image loadImage(final String imageId) {
        return AwsToolkitCore.getDefault().getImageRegistry().get(imageId);
    }
}
