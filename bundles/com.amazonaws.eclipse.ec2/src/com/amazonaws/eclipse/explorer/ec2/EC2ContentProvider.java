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

import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;

public class EC2ContentProvider extends AbstractContentProvider {

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof EC2RootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { EC2RootElement.ROOT_ELEMENT };
        }

        if ( parentElement instanceof EC2RootElement ) {
            return new Object[] {
                    EC2ExplorerNodes.AMIS_NODE,
                    EC2ExplorerNodes.INSTANCES_NODE,
                    EC2ExplorerNodes.EBS_NODE,
                    EC2ExplorerNodes.SECURITY_GROUPS_NODE};
        }

        return null;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.EC2;
    }
}
