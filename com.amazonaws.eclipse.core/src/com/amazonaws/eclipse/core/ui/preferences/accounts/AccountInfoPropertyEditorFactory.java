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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AccountInfo;

/**
 * The factory class that returns AccountInfoPropertyEditor instances.
 */
public class AccountInfoPropertyEditorFactory {

    /**
     * Returns an AccountInfoPropertyEditor instance that binds to a specific
     * property of an AccountInfo object.
     *
     * @param accountInfo
     *            The initial AccountInfo object
     * @param propertyName
     *            The name of the property which is managed by this editor.
     * @param propertyType
     *            The type of the property; either a String type or a File type.
     * @param bindingContext
     *            The context for the data-binding with the AccountInfo model
     */
    public static AccountInfoPropertyEditor
        getAccountInfoPropertyEditor(AccountInfo accountInfo,
                                     String propertyName,
                                     PropertyType propertyType,
                                     String labelText,
                                     Composite parent,
                                     DataBindingContext bindingContext) {
        if (propertyType == PropertyType.STRING_PROPERTY) {
            return new AccountInfoStringPropertyEditor(accountInfo,
                    propertyName, labelText, parent, bindingContext);
        } else {
            return new AccountInfoFilePropertyEditor(accountInfo,
                    propertyName, labelText, parent, bindingContext);
        }
    }

    /**
     * Different types of account info property which requires different UI
     * widgets as the editor.
     */
    public enum PropertyType {
        STRING_PROPERTY,
        FILE_PROPERTY
    }

    /**
     * AccountInfoPropertyEditor implementation that uses JFace
     * StringFieldEditor as the UI widget.
     */
    public static class AccountInfoStringPropertyEditor extends AccountInfoPropertyEditor {

        private final SimpleStringFieldEditor stringEditor;
        private final Composite parent;

        AccountInfoStringPropertyEditor(AccountInfo accountInfo,
                                        String propertyName,
                                        String labelText,
                                        Composite parent,
                                        DataBindingContext bindingContext) {
            super(accountInfo, propertyName, bindingContext);

            this.stringEditor = new SimpleStringFieldEditor(labelText, parent);
            this.parent       = parent;

            resetDataBinding();
        }

        @Override
        public Text getTextControl() {
            return stringEditor.getTextControl(parent);
        }

        /**
         * A package-private method returns the StringFieldEditor object.
         * AwsAccountPreferencePageTab class will use this method to call the
         * fillIntoGrid method.
         */
        public StringFieldEditor getStringFieldEditor() {
            return stringEditor;
        }
    }

    /**
     * A subclass of StringFieldEditor that is not backed by any preference
     * store, and both store and load methods are overridden as no-op.
     */
    private static class SimpleStringFieldEditor extends StringFieldEditor {

        public SimpleStringFieldEditor(String labelText, Composite parent) {
            super("", labelText, parent);
        }

        @Override
        public void store() {
            // no-op
        }

        @Override
        public void load() {
            // no-op
        }
    }

    /**
     * AccountInfoPropertyEditor implementation that uses JFace
     * FileFieldEditor as the UI widget.
     */
    public static class AccountInfoFilePropertyEditor extends AccountInfoPropertyEditor {

        private final SimpleFileFieldEditor fileEditor;
        private final Composite parent;

        AccountInfoFilePropertyEditor(AccountInfo accountInfo,
                                      String propertyName,
                                      String labelText,
                                      Composite parent,
                                      DataBindingContext bindingContext) {
            super(accountInfo, propertyName, bindingContext);

            this.fileEditor = new SimpleFileFieldEditor(labelText, parent);
            this.parent     = parent;

            resetDataBinding();
        }

        @Override
        public Text getTextControl() {
            return fileEditor.getTextControl(parent);
        }

        /**
         * A package-private method returns the FileFieldEditor object.
         * AwsAccountPreferencePageTab class will use this method to call the
         * fillIntoGrid method.
         */
        public FileFieldEditor getFileFieldEditor() {
            return fileEditor;
        }
    }

    /**
     * A subclass of StringFieldEditor that is not backed by any preference
     * store, and both store and load methods are overridden as no-op.
     */
    private static class SimpleFileFieldEditor extends FileFieldEditor {

        public SimpleFileFieldEditor(String labelText, Composite parent) {
            super("", labelText, parent);
        }

        @Override
        public void store() {}

        @Override
        public void load() {}
    }
}
