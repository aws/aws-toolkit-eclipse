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
package com.amazonaws.eclipse.rds;

import org.eclipse.core.databinding.observable.value.DecoratingObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;

final class ControlDataObservableValue extends DecoratingObservableValue {
    private final ISWTObservableValue swtObservableValue;

    // TODO: This doesn't have to be a SWTObservable, it could just be the control...
    public ControlDataObservableValue(ISWTObservableValue swtObservableValue, boolean disposeDecoratedOnDispose) {
        super(swtObservableValue, disposeDecoratedOnDispose);
        this.swtObservableValue = swtObservableValue;
    }

    @Override
    public Object getValue() {
        return swtObservableValue.getWidget().getData((String)swtObservableValue.getValue());
    }

    @Override
    public void setValue(Object value) {
        System.out.println("Setting value for ControlDataObservableValue: " + value);
        super.setValue(value);
    }

    @Override
    public Object getValueType() {
        return null;
    }
}
