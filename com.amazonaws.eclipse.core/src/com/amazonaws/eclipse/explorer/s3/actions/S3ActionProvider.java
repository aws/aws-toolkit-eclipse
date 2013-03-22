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
package com.amazonaws.eclipse.explorer.s3.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.explorer.s3.OpenBucketEditorAction;
import com.amazonaws.services.s3.model.Bucket;

public class S3ActionProvider extends CommonActionProvider {

    @Override
    public void fillContextMenu(IMenuManager menu) {
        IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

        if (selection.size() == 1 && selection.toList().get(0) instanceof Bucket) {
            Bucket bucket = (Bucket)selection.toList().get(0);
            menu.add(new OpenBucketEditorAction(bucket.getName()));
            menu.add(new Separator());
        }

        boolean onlyBucketsSelected = true;
        for (Object obj : selection.toList()) {
            if (obj instanceof Bucket == false) {
                onlyBucketsSelected = false;
                break;
            }
        }

        menu.add(new CreateBucketAction());

        if (onlyBucketsSelected) {
            menu.add(new DeleteBucketAction(selection.toList()));
        }
    }

}
