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

package com.amazonaws.eclipse.ec2.ui.securitygroups;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.model.SecurityGroup;

/**
 * Label and content provider for the security group selection table.
 */
class SecurityGroupTableProvider extends LabelProvider
    implements ITreeContentProvider, ITableLabelProvider {

    List<SecurityGroup> securityGroups;

    /*
     * IStructuredContentProvider
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        if (!(newInput instanceof List)) return;

        securityGroups = (List<SecurityGroup>)newInput;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
     */
    @Override
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object parent) {
        return securityGroups.toArray();
    }


    /*
     * ITableLabelProvider Interface
     */

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
     */
    @Override
    public String getColumnText(Object obj, int index) {
        if (!(obj instanceof SecurityGroup)) return "???";

        SecurityGroup securityGroup = (SecurityGroup)obj;

        switch (index) {
        case 0:
            return securityGroup.getGroupName();
        case 1:
            return securityGroup.getDescription();
        }

        return "N/A";
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
     */
    @Override
    public Image getColumnImage(Object obj, int index) {
        if (index == 0)
            return Ec2Plugin.getDefault().getImageRegistry().get("shield");
        return null;        
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage(Object obj) {
        return null;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return false;
    }
}
