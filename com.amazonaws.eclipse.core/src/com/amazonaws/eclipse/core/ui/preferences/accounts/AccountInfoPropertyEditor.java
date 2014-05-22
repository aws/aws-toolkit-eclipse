/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.preferences.accounts;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AccountInfo;

/**
 * The abstract class that defines the basic behavior of a property editor for
 * an AccountInfo object. Subclasses are free to use any kind of UI widgets, as
 * long as it contains a SWT Text control that can be used for data-binding.
 */
public abstract class AccountInfoPropertyEditor {

    /** The AccountInfo object that is currently managed by this editor */
    protected AccountInfo accountInfo;

    /** The name of the AccountInfo's POJO property that is managed by this editor */
    protected final String propertyName;

    /** The DataBindingContext instance used for binding the AccountInfo object with the UI control */
    private final DataBindingContext bindingContext;

    private Binding bindingWithCurrentAccountInfo;

    AccountInfoPropertyEditor(AccountInfo accountInfo, String propertyName,
            DataBindingContext bindingContext) {
        this.accountInfo    = accountInfo;
        this.propertyName   = propertyName;
        this.bindingContext = bindingContext;
    }

    /**
     * Update the accountInfo variable, and reset the databinding.
     */
    public void accountChanged(AccountInfo newAccountInfo) {
        this.accountInfo = newAccountInfo;
        resetDataBinding();
    }

    /**
     * Pro-actively push the text value of the editor to the underlying
     * AccountInfo model data.
     */
    public void forceUpdateEditorValueToAccountInfoModel() {
        if (bindingWithCurrentAccountInfo != null) {
            bindingWithCurrentAccountInfo.updateTargetToModel();
        }
    }

    /**
     * Implement this method to return the text control object used by the
     * editor. The returned text control object will be used as the target of
     * the data-binding.
     */
    public abstract Text getTextControl();

    /**
     * Reset the data-binding between the property of the AccountInfo POJO and
     * the text control provided by the concrete subclass.
     */
    protected void resetDataBinding() {
        // Remove the current binding
        if (bindingWithCurrentAccountInfo != null) {
            bindingWithCurrentAccountInfo.dispose();
        }

        IObservableValue modelValue = BeansObservables.observeValue(
                accountInfo, propertyName);
        IObservableValue viewValue = SWTObservables.observeText(getTextControl(), SWT.Modify);

        bindingWithCurrentAccountInfo = bindingContext.bindValue(viewValue, modelValue);
    }

}
