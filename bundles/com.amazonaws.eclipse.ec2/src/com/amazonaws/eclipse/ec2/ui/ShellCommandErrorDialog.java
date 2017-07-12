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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.ec2.ShellCommandException;
import com.amazonaws.eclipse.ec2.ShellCommandResults;

/**
 * Dialog for showing the details of a failed attempt at executing a command,
 * including support for possible retries, and directing users to more help.
 */
public class ShellCommandErrorDialog extends MessageDialog {

    private final ShellCommandException sce;
    private Text outputText;
    private Text errorOutputText;
    private List<Button> attemptRadioButtons = new LinkedList<>();

    /**
     * Creates a new ShellCommandErrorDialog, ready to be opened and display
     * information the specified ShellCommandException.
     * 
     * @param sce
     *            The ShellCommandException to display.
     */
    public ShellCommandErrorDialog(ShellCommandException sce) {
        super(Display.getDefault().getShells()[0],
                "Error Executing Command",
                null,
                sce.getMessage(),
                MessageDialog.ERROR,
                new String[] {"Ok"},
                1);
        
        this.sce = sce;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createCustomArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new GridLayout(1, false));        
    
        if (sce.getNumberOfAttempts() > 1) {
            createAttemptSelectionArea(composite);
            attemptRadioButtons.get(0).setSelection(true);
        }
        createCommandOutputArea(composite);
        createHelpLinkArea(composite);
        
        update();

        return composite;
    }

    /*
     * Private Interface
     */

    /**
     * Creates a new UI section displaying a link where users can go for more
     * help.
     * 
     * @param composite
     *            The parent composite for this new section.
     */
    private void createHelpLinkArea(Composite composite) {
        Link link = new Link(composite, SWT.WRAP);
        link.setText("Need help?  " +
                "<a href=\"http://aws.amazon.com/eclipse/\">http://aws.amazon.com/eclipse/</a>");
        WebLinkListener webLinkListener = new WebLinkListener();
        link.addListener(SWT.Selection, webLinkListener);
    }

    /**
     * Creates a new UI section displaying the details of the selected attempt
     * to execute a command.
     * 
     * @param composite
     *            The parent composite for this new section.
     */
    private void createCommandOutputArea(Composite composite) {
        Composite outputComposite = new Composite(composite, SWT.NONE);
        outputComposite.setLayout(new GridLayout(1, false));
        outputComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Group outputGroup = new Group(outputComposite, SWT.None);
        outputGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        outputGroup.setLayout(new GridLayout(2, false));
        outputGroup.setText("Command Output:");
        
        newLabel(outputGroup, "Output:");
        outputText = newText(outputGroup, "");
        
        newLabel(outputGroup, "Errors:");
        errorOutputText = newText(outputGroup, "");
    }

    /**
     * Creates a new UI section displaying a list of attempts that a user can
     * select to see more information.
     * 
     * @param composite
     *            The parent composite for this new section.
     */
    private void createAttemptSelectionArea(Composite composite) {
        Composite attemptsComposite = new Composite(composite, SWT.NONE);
        attemptsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        attemptsComposite.setLayout(new GridLayout(1, true));
        
        Group attemptsGroup = new Group(attemptsComposite, SWT.None);
        attemptsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        attemptsGroup.setLayout(new GridLayout(1, false));

        Label l = new Label(attemptsGroup, SWT.WRAP);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        l.setText("Select an attempt to see the output from it:");
        
        SelectionListener listener = new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
            @Override
            public void widgetSelected(SelectionEvent e) {
                update();
            }                
        };

        attemptRadioButtons = new LinkedList<>();
        for (ShellCommandResults results : sce.getShellCommandResults()) {
            Button button = new Button(attemptsGroup, SWT.RADIO);
            button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            attemptRadioButtons.add(button);

            button.addSelectionListener(listener);
            
            button.setText("Attempt " + attemptRadioButtons.size() 
                    + "  (exit code: " + results.exitCode + ")");
            button.setData(results);
        }
    }

    /**
     * Returns the ShellCommandResults associated with the selected command
     * execution attempt.
     * 
     * @return The ShellCommandResults associated with the selected attempt.
     */
    private ShellCommandResults getSelectedAttempt() {
        for (Button button : attemptRadioButtons) {
            if (button.getSelection()) {
                return (ShellCommandResults)button.getData();
            }
        }

        return sce.getShellCommandResults().get(0);
    }

    /**
     * Updates the command output section so that the correct output is
     * displayed based on what command execution attempt is selected.
     */
    private void update() {
        ShellCommandResults results = getSelectedAttempt();

        outputText.setText(results.output);
        errorOutputText.setText(results.errorOutput);
    }
    
    /**
     * Utility method for creating a Text widget.
     */
    private Text newText(Composite composite, String s) {
        Text text = new Text(composite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setText(s);
        
        GridData data = new GridData(GridData.FILL_BOTH);
        data.minimumHeight = 80;
        data.heightHint = 100;
        data.minimumWidth  = 100;
        data.widthHint = 250;
        text.setLayoutData(data);
        
        return text;
    }
    
    /**
     * Utility method for creating a Label widget.
     */
    private Label newLabel(Composite outputComposite, String text) {
        Label label = new Label(outputComposite, SWT.NONE);
        label.setText(text);
        
        GridData data = new GridData();
        data.verticalAlignment = SWT.TOP;
        label.setLayoutData(data);
        
        return label;
    }
    
}
