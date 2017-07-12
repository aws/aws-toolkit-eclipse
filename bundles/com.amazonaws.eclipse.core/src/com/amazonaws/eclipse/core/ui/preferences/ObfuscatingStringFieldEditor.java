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

package com.amazonaws.eclipse.core.ui.preferences;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Extension of StringFieldEditor that obfuscates its data.
 */
public class ObfuscatingStringFieldEditor extends StringFieldEditor {

    /**
     * Creates a new ObfuscatingStringFieldEditor.
     * 
     * @param preferenceKey
     *            The name of the preference key.
     * @param labelText
     *            The field editor's label text.
     * @param parent
     *            The parent for this field editor.
     */
    public ObfuscatingStringFieldEditor(String preferenceKey, String labelText, Composite parent) {
        super(preferenceKey, labelText, parent);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.StringFieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        Text textField = getTextControl();
        if (textField == null) return;
        
        String value = getPreferenceStore().getString(getPreferenceName());
        if (value == null) return;

        if (isBase64(value)) value = decodeString(value);
        
        textField.setText(value);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.StringFieldEditor#doStore()
     */
    @Override
    protected void doStore() {
        Text textField = getTextControl();        
        if (textField == null) return;
        
        String encodedValue = encodeString(textField.getText());
        
        getPreferenceStore().setValue(getPreferenceName(), encodedValue);
    }
    
    public static boolean isBase64(String s) {
        return Base64.isArrayByteBase64(s.getBytes());
    }
    
    public static String encodeString(String s) {
        return new String(Base64.encodeBase64(s.getBytes()));
    }
    
    public static  String decodeString(String s) {
        return new String(Base64.decodeBase64(s.getBytes()));
    }

}
