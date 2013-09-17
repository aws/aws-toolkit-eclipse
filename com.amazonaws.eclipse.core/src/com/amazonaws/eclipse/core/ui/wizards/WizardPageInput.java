/*
 * Copyright 2008-2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;

/**
 * An input field of a CompositeWizardPage. Captures a single value from the
 * user.
 */
public interface WizardPageInput {
    /**
     * Initialize the input.
     *
     * @param parent the parent composite
     * @param context the data binding context for the page
     */
    void init(Composite parent, DataBindingContext context);

    /**
     * @return the value the user added to this input
     */
    Object getValue();

    /**
     * Dispose any resources owned by this input.
     */
    void dispose();
}
