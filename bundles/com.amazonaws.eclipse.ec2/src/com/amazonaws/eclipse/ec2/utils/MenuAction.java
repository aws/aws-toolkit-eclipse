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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.utils.IMenu.MenuItem;

/**
 * Action class to create static Menu objects
 */
public class MenuAction extends Action implements IMenuCreator {

    /** Holds MenuHandler object to process the menus */
    private MenuHandler menuHandler;
    
    /** Menu that gets displayed */
    protected Menu menu;

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
    public MenuAction(String text, String toolTip, 
                String imageDescriptor, MenuHandler menuHandler) {
        setText(text);
        setToolTipText(toolTip);
        setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor(imageDescriptor));
        this.menuHandler = menuHandler;
        setMenuCreator(this);
    }
    
    /**
     * @see org.eclipse.jface.action.IMenuCreator#getMenu(Menu)
     */
    @Override
    public Menu getMenu(Menu parent) {
        return menu;
    }
    
    /**
     * @see org.eclipse.jface.action.IMenuCreator#getMenu(Control)
     */
    @Override
    public Menu getMenu(Control parent) {
        if (menu != null) {
            return menu;
        }
        return constructMenu(parent);
    }

    /**
     * Creates the menu dynamically
     * 
     * @param parent
     *            Composite on which the menu is drawn
     *
     * @return The created menu
     */
    protected Menu constructMenu(Control parent) {
        menu = new Menu(parent);

        for (final MenuItem menuItem : menuHandler.getMenuItems()) {
            if (menuItem.equals(MenuItem.SEPARATOR)) {
                new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);                
            } else {
                IAction action = new Action(menuItem.getMenuText(), AS_RADIO_BUTTON) {
                    @Override
                    public void run() {
                        if (isChecked()) {
                            menuHandler.setCurrentSelection(menuItem);
                        }
                    }                
                };
                
                addActionToMenu(action);
                
                // Every Time new object is created, so getMenuId is used for
                // determining the current selection
                action.setChecked(menuHandler.getCurrentSelection().getMenuId().equals(menuItem.getMenuId()));
            }
        }
        
        return menu;
    }

    /**
     * Use to add different MenuItems to the menu
     * 
     * @param action
     *            The Action to be added
     */
    protected void addActionToMenu(IAction action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(menu, -1);
    }

    /**
     * @see org.eclipse.jface.action.IMenuCreator#dispose()
     */
    @Override
    public void dispose() {
        if (menu != null) {
            menu.dispose();
        }
    }

    /**
     * Used to popup the menu
     * 
     * @see org.eclipse.jface.action.Action#run()
     */    
    @Override
    public void run() {
        if (menu != null)
            menu.setVisible(true);
    }
}
