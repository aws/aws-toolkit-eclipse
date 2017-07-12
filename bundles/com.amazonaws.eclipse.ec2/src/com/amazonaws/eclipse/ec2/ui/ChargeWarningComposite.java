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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Simple composite that displays a message to make sure users understand that
 * launching EC2 instances will charge their account.
 */
public class ChargeWarningComposite extends Composite {

    /**
     * Creates a new ChargeWarningComposite as a child in the specified
     * composite, with the specified styling information.
     * 
     * @param parent
     *            The parent composite that will contain this new
     *            ChargeWarningComposite.
     * @param style
     *            The styling bits for this new Composite.
     */
    public ChargeWarningComposite(Composite parent, int style) {
        super(parent, style);

        setLayout(new GridLayout(2, false));

        Label infoIconLabel = new Label(this, SWT.NONE);
        infoIconLabel.setImage(Ec2Plugin.getDefault().getImageRegistry().get("info"));
        infoIconLabel.setLayoutData(new GridData());
        
        Label infoLabel = new Label(this, SWT.WRAP);
        infoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        infoLabel.setText("You will be charged the hourly rate for any instances you launch until you successfully shut them down.");
        italicizeLabel(infoLabel);        
    }
    
    
    /*
     * Private Interface
     */

    /**
     * Changes the font style on the specified label so that the text is
     * displayed in italics.
     * 
     * @param label
     *            The label to change.
     */
    private void italicizeLabel(Label label) {
        Font font = label.getFont();
        FontData[] fontDataArray = font.getFontData();
        for (FontData fontData : fontDataArray) {
            fontData.setStyle(SWT.ITALIC);
        }
        
        Font newFont = new Font(Display.getDefault(), fontDataArray);
        label.setFont(newFont);
    }
    
}
