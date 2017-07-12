/*
 * Copyright 2009-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2.utils;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * Action class to create dynamic Menu objects. The menu is created everytime
 * the widget is invoked, enabling the menu to be re-populated with fresh data
 */
public class DynamicMenuAction extends MenuAction {

    /**
     * Constructor
     * 
     * @param text
     *            Text that gets displayed on the Menu
     * @param toolTip
     *            Tooltip that gets displayed for the menu
     * @param imageDescriptor
     *            The icon used for the menu
     * @param menuHandler
     *            The MenuHandler object used to manage actions
     */
    public DynamicMenuAction(String text, String toolTip, 
                String imageDescriptor, MenuHandler dropdownMenuHandler) {
        super(text, toolTip, imageDescriptor, dropdownMenuHandler);
    }

    /**
     * If there is already a menu from previous, dispose it for being able to
     * create a fresh one
     * 
     * @see com.amazonaws.eclipse.ec2.utils.MenuAction#getMenu(Menu parent)
     */
    @Override 
    public Menu getMenu(Control parent) {
        if (menu != null) {
            menu.dispose();
        }
        
        return constructMenu(parent);
    }
}
