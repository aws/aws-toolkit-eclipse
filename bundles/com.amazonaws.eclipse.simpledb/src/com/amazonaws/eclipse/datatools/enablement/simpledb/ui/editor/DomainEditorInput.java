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
package com.amazonaws.eclipse.datatools.enablement.simpledb.ui.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.amazonaws.eclipse.core.AwsToolkitCore;


/**
 * Editor input for the custom query editor
 */
public class DomainEditorInput implements IEditorInput {

    private final String domainName;
    private final String accountId;

    String getAccountId() {
        return this.accountId;
    }

    String getDomainName() {
        return this.domainName;
    }

    public DomainEditorInput(final String domainName, final String accountId) {
        super();
        this.domainName = domainName;
        this.accountId = accountId;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Class adapter) {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_TABLE);
    }

    @Override
    public String getName() {
        return this.domainName;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return "Amazon SimpleDB Query Editor - " + this.domainName;
    }

}
