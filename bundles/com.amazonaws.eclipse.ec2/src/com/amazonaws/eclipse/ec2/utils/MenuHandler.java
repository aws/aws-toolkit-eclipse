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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.ec2.utils.IMenu.MenuItem;

/**
 * Handler to process Menu selections
 */
public class MenuHandler {

    /** Holds the currently selected Menu Item */
    private MenuItem currentSelection;
    
    /** Holds all the Menu Items that needs to be displayed */
    private List<MenuItem> menuItems;
    
    /** Holds the client from which the Menu is invoked */
    private IMenu observer;
    
    /**
     * Constructor
     */
    public MenuHandler() {
        menuItems = new ArrayList<>();
    }

    /**
     * Creates a MenuItem for the menu
     * 
     * @param menuId
     *            An Id for the menu
     * @param menuText
     *            The text that gets displayed
     * @return Created MenuItem
     * 
     * @see MenuHandler#add(String, String, boolean)
     */
    public MenuItem add(String menuId, String menuText) {
        return add(menuId, menuText, false);
    }

    /**
     * Creates a MenuItem for the menu
     * 
     * @param menuId
     *            An Id for the menu
     * @param menuText
     *            The text that gets displayed
     * @param boolean The current MenuItem is marked for selection
     *
     * @return Created MenuItem
     */    
    public MenuItem add(String menuId, String menuText, boolean selected) {
        MenuItem menuItem = new MenuItem(menuId, menuText);
        menuItems.add(menuItem);
        currentSelection = selected ? menuItem : currentSelection;
        return menuItem;
    }

    /**
     * Adds a MenuItem to the current MenuList
     * 
     * @param menuItem
     *            MenuItem to be added
     */
    public void add(MenuItem menuItem) {
        menuItems.add(menuItem);
    }

    /**
     * Clears the MenuItem list. Used when Menu needs to be refreshed with new
     * data
     */
    public void clear() {
        menuItems.clear();
    }

    /**
     * Returns the current MenuItem selected
     * 
     * @return MenuItem selected
     */
    public MenuItem getCurrentSelection() {
        return currentSelection;
    }

    /**
     * Sets the current selection of the MEnuItem
     * 
     * @param currentSelection
     *            Current selected MenuItem
     */
    public void setCurrentSelection(MenuItem currentSelection) {
        this.currentSelection = currentSelection;
        
        if (observer != null)
            observer.menuClicked(currentSelection);
    }

    /**
     * Returns the list of MenuItems
     * 
     * @return List The menu list
     */
    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    /**
     * Registers the client as the listener, which will be notified using
     * callback
     * 
     * @param observer
     *            The client which wishes to get notified for every MenuItems
     *            selected
     */
    public void addListener(IMenu observer) {
        this.observer = observer;
    }
}
