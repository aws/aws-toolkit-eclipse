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

package com.amazonaws.eclipse.ec2.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A SwappableComposite allows only one of its children to be displayed at
 * once.  When one child is displayed, all the others are hidden.
 */
public class SwappableComposite extends Composite {
    /**
     * Creates a new SwappableComposite with the specified parent and SWT
     * style.
     * 
     * @param parent
     *            The parent of this composite.
     * @param style
     *            The SWT style for this composite.
     */
    public SwappableComposite(Composite parent, int style) {
        super (parent, style);
    }

    /**
     * Displays the specified composite and hides all other child
     * composites.
     * 
     * @param composite
     *            The child Composite to display.
     */
    public void setActiveComposite(Control composite) {
        if (composite == null) return;
        
        for (Control control : getChildren()) {
            if (composite.equals(control)) {
                control.setVisible(true);
                control.setBounds(getClientArea());
                control.getParent().layout();
            } else {
                control.setVisible(false);
            }
        }
    }
}
