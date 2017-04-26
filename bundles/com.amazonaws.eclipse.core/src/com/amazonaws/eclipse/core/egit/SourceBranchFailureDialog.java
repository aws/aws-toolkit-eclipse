/*******************************************************************************
 * Copyright (C) 2012, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.amazonaws.eclipse.core.egit;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * Creates and shows custom error dialog for failed ls-remotes operation
 */
@SuppressWarnings("restriction")
public class SourceBranchFailureDialog extends MessageDialog {

    /**
     * Creates and shows custom error dialog for failed ls-remotes operation
     *
     * @param parentShell
     * @param uri
     *            the uri of the remote repository
     */
    public static void show(Shell parentShell, URIish uri) {
        SourceBranchFailureDialog dialog = new SourceBranchFailureDialog(
                parentShell, uri);
        dialog.setShellStyle(dialog.getShellStyle() | SWT.SHEET | SWT.RESIZE);
        dialog.open();
    }

    private Button toggleButton;

    private URIish uri;

    private SourceBranchFailureDialog(Shell parentShell, URIish uri) {
        super(parentShell, UIText.CloneFailureDialog_tile, null, null,
                MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
        this.uri = uri;
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (toggleButton != null)
            Activator
                    .getDefault()
                    .getPreferenceStore()
                    .setValue(UIPreferences.CLONE_WIZARD_SHOW_DETAILED_FAILURE_DIALOG,
                            !toggleButton.getSelection());

        super.buttonPressed(buttonId);
    }

    @Override
    protected Control createMessageArea(Composite composite) {
        Composite main = new Composite(composite, SWT.NONE);
        main.setLayout(new GridLayout(2, false));
        GridDataFactory.fillDefaults().indent(0, 0).grab(true, true)
                .applyTo(main);
        // add error image
        super.createMessageArea(main);

        StyledText text = new StyledText(main, SWT.FULL_SELECTION | SWT.WRAP);
        text.setEnabled(false);
        text.setBackground(main.getBackground());

        String messageText = NLS.bind(UIText.CloneFailureDialog_checkList,
                uri.toString());
        int bullets = 2;
        if (!uri.getPath().endsWith(".git")) { //$NON-NLS-1$
            messageText = messageText + UIText.CloneFailureDialog_checkList_git;
            bullets += 1;
        }
        if ("ssh".equals(uri.getScheme())) { //$NON-NLS-1$
            messageText = messageText + UIText.CloneFailureDialog_checkList_ssh;
            bullets += 1;
        } else if ("https".equals(uri.getScheme())) { //$NON-NLS-1$
            messageText = messageText
                    + UIText.CloneFailureDialog_checkList_https;
            bullets += 1;
        }
        int newLinesCount = messageText.split("\n").length; //$NON-NLS-1$
        Bullet bullet = createBullet(main);

        text.setText(messageText);
        text.setLineBullet(newLinesCount - bullets, bullets, bullet);

        return main;
    }

    private Bullet createBullet(Composite main) {
        StyleRange style = new StyleRange();
        style.metrics = new GlyphMetrics(0, 0, 40);
        style.foreground = main.getDisplay().getSystemColor(SWT.COLOR_BLACK);
        Bullet bullet = new Bullet(style);
        return bullet;
    }

    @Override
    protected Control createCustomArea(Composite parent) {
        toggleButton = new Button(parent, SWT.CHECK | SWT.LEFT);
        toggleButton.setText(UIText.CloneFailureDialog_dontShowAgain);

        return null;
    }

}