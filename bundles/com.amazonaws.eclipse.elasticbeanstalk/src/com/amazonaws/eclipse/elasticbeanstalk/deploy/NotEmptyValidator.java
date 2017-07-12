/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.deploy;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.widgets.Button;

public class NotEmptyValidator implements IValidator {

    protected final ControlDecoration controlDecoration;
    protected Button button;

    public NotEmptyValidator(ControlDecoration controlDecoration, Button button) {
        this.controlDecoration = controlDecoration;
        this.button = button;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.databinding.validation.IValidator#validate(java.lang.Object)
     */
    @Override
    public IStatus validate(Object value) {
        if (value instanceof String == false) {
            throw new RuntimeException("Only string validation is supported.");
        }

        // If a button has been specified and it isn't selected, then
        // we don't need to do any validation and can bail out.
        if (button != null && button.getSelection() == false) {
            controlDecoration.hide();
            return ValidationStatus.ok();
        }

        String s = (String)value;
        if (s != null && s.trim().length() > 0) {
            controlDecoration.hide();
            return ValidationStatus.ok();
        } else {
            controlDecoration.show();
            return ValidationStatus.info(controlDecoration.getDescriptionText());
        }
    }
}
