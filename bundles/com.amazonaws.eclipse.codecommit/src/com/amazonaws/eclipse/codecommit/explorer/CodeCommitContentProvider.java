/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codecommit.explorer;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.codecommit.explorer.CodeCommitActionProvider.OpenRepositoryEditorAction;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.codecommit.AWSCodeCommit;
import com.amazonaws.services.codecommit.model.ListRepositoriesRequest;
import com.amazonaws.services.codecommit.model.RepositoryNameIdPair;

public class CodeCommitContentProvider extends AbstractContentProvider implements ITreeContentProvider {

    private final IOpenListener listener = new IOpenListener() {

        @Override
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection) event.getSelection();

            Iterator<?> i = selection.iterator();
            while ( i.hasNext() ) {
                Object obj = i.next();
                if ( obj instanceof RepositoryNameIdPair ) {
                    RepositoryNameIdPair nameIdPair = (RepositoryNameIdPair) obj;
                    OpenRepositoryEditorAction action = new OpenRepositoryEditorAction(nameIdPair);
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

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.CODECOMMIT;
    }

    @Override
    public Object[] loadChildren(final Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement ) {
            return new Object[] { CodeCommitRootElement.ROOT_ELEMENT};
        }

        if ( parentElement instanceof CodeCommitRootElement ) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AWSCodeCommit codeCommit = AwsToolkitCore.getClientFactory()
                            .getCodeCommitClient();
                    List<RepositoryNameIdPair> applications = codeCommit.listRepositories(new ListRepositoriesRequest()).getRepositories();
                    return applications.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof CodeCommitRootElement);
    }

}
