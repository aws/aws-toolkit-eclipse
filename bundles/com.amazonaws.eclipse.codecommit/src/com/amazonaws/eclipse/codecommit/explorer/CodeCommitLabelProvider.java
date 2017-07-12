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

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.explorer.ExplorerNodeLabelProvider;
import com.amazonaws.services.codecommit.model.RepositoryNameIdPair;

public class CodeCommitLabelProvider extends ExplorerNodeLabelProvider {
    @Override
    public Image getDefaultImage(Object element) {
        ImageRegistry imageRegistry = CodeCommitPlugin.getDefault().getImageRegistry();
        if ( element instanceof CodeCommitRootElement ) {
            return imageRegistry.get(CodeCommitPlugin.IMG_SERVICE);
        }

        if ( element instanceof RepositoryNameIdPair ) {
            return imageRegistry.get(CodeCommitPlugin.IMG_REPOSITORY);
        }

        return null;
    }

    @Override
    public String getText(Object element) {
        if ( element instanceof CodeCommitRootElement ) {
            return "AWS CodeCommit";
        }

        if ( element instanceof RepositoryNameIdPair ) {
            return ((RepositoryNameIdPair) element).getRepositoryName();
        }

        return getExplorerNodeText(element);
    }
}
