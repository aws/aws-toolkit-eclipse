/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.identitymanagement;

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;

public class EditorInput extends AbstractAwsResourceEditorInput {

    private final String titleName;

    public EditorInput(String titleName, String endpoint, String accountId) {
       super(endpoint, accountId);
        this.titleName = titleName;
    }

    @Override
    public String getName() {
        return titleName;
    }

    @Override
    public String getToolTipText() {
        return "Amazon Identity Management Editor - " + getName();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_STACK);
    }

    @Override
    public int hashCode() {
          final int prime = 31;
          int hashCode = 1;

          hashCode = prime * hashCode + ((getName() == null) ? 0 : getName().hashCode());
          hashCode = prime * hashCode + ((getRegionEndpoint() == null) ? 0 : getRegionEndpoint().hashCode());
          hashCode = prime * hashCode + ((getRegionEndpoint() == null) ? 0 : getAccountId().hashCode());
          return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof EditorInput == false) return false;
        EditorInput other = (EditorInput)obj;

        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false;
        if (other.getAccountId() == null ^ this.getAccountId() == null) return false;
        if (other.getAccountId() != null && other.getAccountId().equals(this.getAccountId()) == false) return false;
        if (other.getRegionEndpoint() == null ^ this.getRegionEndpoint() == null) return false;
        if (other.getRegionEndpoint() != null && other.getRegionEndpoint().equals(this.getRegionEndpoint()) == false) return false;
        return true;
    }
}
