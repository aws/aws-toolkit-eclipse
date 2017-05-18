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

import com.amazonaws.eclipse.core.ui.SelectOrInputComposite;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Data model for {@link SelectOrInputComposite} which is intended to be extended by a
 * subclass with a concrete type for the generic type T.
 */
public abstract class SelectOrInputDataModel<T> {
    public static final String P_EXISTING_RESOURCE = "existingResource";
    public static final String P_NEW_RESOURCE_NAME = "newResourceName";
    public static final String P_SELECT_EXISTING_RESOURCE = "selectExistingResource";
    public static final String P_CREATE_NEW_RESOURCE = "createNewResource";

    @JsonIgnore
    private T existingResource;
    private String newResourceName;
    private boolean selectExistingResource = false;
    private boolean createNewResource = true;

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

    public String getNewResourceName() {
        return newResourceName;
    }

    public void setNewResourceName(String newResourceName) {
        String oldValue = this.newResourceName;
        this.newResourceName = newResourceName;
        this.pcs.firePropertyChange(P_NEW_RESOURCE_NAME, oldValue, newResourceName);
    }

    public boolean isSelectExistingResource() {
        return selectExistingResource;
    }

    public void setSelectExistingResource(boolean selectExistingResource) {
        boolean oldValue = this.selectExistingResource;
        this.selectExistingResource = selectExistingResource;
        this.pcs.firePropertyChange(P_SELECT_EXISTING_RESOURCE, oldValue, selectExistingResource);
    }

    public boolean isCreateNewResource() {
        return createNewResource;
    }

    public void setCreateNewResource(boolean createNewResource) {
        boolean oldValue = this.createNewResource;
        this.createNewResource = createNewResource;
        this.pcs.firePropertyChange(P_CREATE_NEW_RESOURCE, oldValue, createNewResource);
    }
}
