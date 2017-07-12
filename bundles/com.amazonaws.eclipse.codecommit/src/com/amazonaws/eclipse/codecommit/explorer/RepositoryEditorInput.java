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

import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.codecommit.CodeCommitPlugin;
import com.amazonaws.eclipse.explorer.AbstractAwsResourceEditorInput;
import com.amazonaws.services.codecommit.model.RepositoryNameIdPair;

public class RepositoryEditorInput extends AbstractAwsResourceEditorInput {

    private final RepositoryNameIdPair repository;

    public RepositoryEditorInput(RepositoryNameIdPair repository, String regionEndpoint, String accountId, String regionId) {
        super(regionEndpoint, accountId, regionId);
        this.repository = repository;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return CodeCommitPlugin.getDefault().getImageRegistry().getDescriptor(CodeCommitPlugin.IMG_REPOSITORY);
    }

    public RepositoryNameIdPair getRepository() {
        return repository;
    }

    @Override
    public String getToolTipText() {
        return "AWS CodeCommit Repository Editor - " + repository.getRepositoryName();
    }

    @Override
    public String getName() {
        return repository.getRepositoryName();
    }

}
