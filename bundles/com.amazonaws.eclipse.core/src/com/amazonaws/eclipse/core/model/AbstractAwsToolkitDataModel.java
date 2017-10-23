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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Abstract data model with the property change support.
 */
public abstract class AbstractAwsToolkitDataModel {
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    protected <T> void setProperty(String propertyName, T newProperty, Supplier<T> getter, Consumer<T> setter) {
        T oldValue = getter.get();
        setter.accept(newProperty);
        pcs.firePropertyChange(propertyName, oldValue, newProperty);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}