/*
 * Copyright 2008-2011 Amazon Technologies, Inc. 
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * A simple dialog that instructs users how to use PuTTYgen to translate an
 * OpenSSH private key file to a PuTTY compatible private key.
 */
public class PuTTYgenTranslationDialog extends MessageDialog {

	/**
	 * The full path where the translated PuTTY key must be saved in order
	 * for Eclipse to find it.
	 */
	private final String puttyPrivateKeyFile;

	/**
	 * Creates a new dialog explaining how to convert an OpenSSH private key
	 * file to a PuTTY compatible private key.
	 * 
	 * @param puttyPrivateKeyFile
	 *            The full path where the user must save the PuTTY key in
	 *            order for Eclipse to find it.
	 */
	public PuTTYgenTranslationDialog(String puttyPrivateKeyFile) {			
		super(Display.getDefault().getShells()[0],
				"Translate Key with PuTTYgen",
				null, 
				"This key needs to be translated with PuTTYgen before PuTTY can use it.",
				MessageDialog.INFORMATION,
				new String[] {"Ok"},
				1);
		
		this.puttyPrivateKeyFile = puttyPrivateKeyFile;			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createCustomArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		String instructions = "When PuTTYgen opens, select 'Save private key'.\n"
			+ "For Eclipse to find the key, it must be saved as: ";

		Label instructionsLabel = new Label(composite, SWT.WRAP);
		instructionsLabel.setText(instructions);
		instructionsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label puttyKeyLabel = new Label(composite, SWT.WRAP);
		puttyKeyLabel.setText(puttyPrivateKeyFile);
		puttyKeyLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Font font = puttyKeyLabel.getFont();
		FontData oldFontData = font.getFontData()[0];
		
		FontData fontData = new FontData();
		fontData.setName(oldFontData.getName());
		fontData.setHeight(oldFontData.getHeight() + 4);
		fontData.setStyle(SWT.BOLD);
		
		puttyKeyLabel.setFont(new Font(Display.getCurrent(), fontData));
		
		return composite;
	}
}
