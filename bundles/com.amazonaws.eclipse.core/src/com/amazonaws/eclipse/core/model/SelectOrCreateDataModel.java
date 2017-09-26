/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.eclipse.core.ui.SelectOrCreateComposite;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Data model for {@link SelectOrCreateComposite} which is intended to be extended by a
 * subclass with a concrete type for the generic type T.
 */
public abstract class SelectOrCreateDataModel<T, P extends AbstractAwsResourceScopeParam<P>> implements AwsResourceMetadata<T, P> {

    public static final String P_EXISTING_RESOURCE = "existingResource";

    @JsonIgnore
    private T existingResource;
    private boolean createNewResource = false;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public T getExistingResource() {
        return existingResource;
    }

    public void setExistingResource(T existingResource) {
        T oldValue = this.existingResource;
        this.existingResource = existingResource;
        this.pcs.firePropertyChange(P_EXISTING_RESOURCE, oldValue, existingResource);
    }

    public boolean isCreateNewResource() {
        return createNewResource;
    }

    public void setCreateNewResource(boolean createNewResource) {
        this.createNewResource = createNewResource;
    }
}
