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
package com.amazonaws.eclipse.explorer.ec2;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;

public class EC2LabelProvider extends ExplorerNodeLabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof EC2RootElement) return "Amazon EC2";

        return getExplorerNodeText(element);
    }

    @Override
    public Image getDefaultImage(Object element) {
        if ( element instanceof EC2RootElement ) {
            return Ec2Plugin.getDefault().getImageRegistry().get("ec2-service");
        }

        return null;
    }
}
