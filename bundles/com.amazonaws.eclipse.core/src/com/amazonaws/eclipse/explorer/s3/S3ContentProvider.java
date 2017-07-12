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
package com.amazonaws.eclipse.explorer.s3;

import java.util.Iterator;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class S3ContentProvider extends AbstractContentProvider {

    private static S3ContentProvider instance;

    private final IOpenListener listener = new IOpenListener() {

        @Override
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection)event.getSelection();

            Iterator<?> i = selection.iterator();
            while ( i.hasNext() ) {
                Object obj = i.next();
                if ( obj instanceof Bucket ) {
                    Bucket bucket = (Bucket) obj;
                    OpenBucketEditorAction action = new OpenBucketEditorAction(bucket.getName());
                    action.run();
                }
            }
        }
    };

    @Override
    public void dispose() {
        viewer.removeOpenListener(listener);
        super.dispose();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        this.viewer.addOpenListener(listener);
    }

    public S3ContentProvider() {
        instance = this;
    }

    public static S3ContentProvider getInstance() {
        return instance;
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement || element instanceof S3RootElement);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { S3RootElement.ROOT_ELEMENT };
        }

        if ( parentElement instanceof S3RootElement ) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonS3 s3 = AwsToolkitCore.getClientFactory().getS3Client();
                    return s3.listBuckets().toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.S3;
    };
}
