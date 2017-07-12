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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.eclipse.explorer.simpledb.SimpleDBExplorerNodes.DomainNode;
import com.amazonaws.services.simpledb.AmazonSimpleDB;

public class SimpleDBContentProvider extends AbstractContentProvider implements ITreeContentProvider {

    private static SimpleDBContentProvider instance;

    public SimpleDBContentProvider() {
        instance = this;
    }

    public static SimpleDBContentProvider getInstance() {
        return instance;
    }

    @Override
    public boolean hasChildren(final Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof SimpleDBRootElement);
    }

    @Override
    public Object[] loadChildren(final Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { SimpleDBRootElement.ROOT_ELEMENT };
        }

        if ( parentElement instanceof SimpleDBRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonSimpleDB client = AwsToolkitCore.getClientFactory().getSimpleDBClient();

                    // Translate the domain names to objects so we can work with them more easily
                    List<DomainNode> domainNodes = new ArrayList<>();
                    for (String domainName : client.listDomains().getDomainNames()) {
                        domainNodes.add(new DomainNode(domainName));
                    }

                    return domainNodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.SIMPLEDB;
    }
}
