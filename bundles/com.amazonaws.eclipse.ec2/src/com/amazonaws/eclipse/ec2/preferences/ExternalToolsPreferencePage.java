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

package com.amazonaws.eclipse.ec2.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.util.OsPlatformUtils;
import com.amazonaws.eclipse.ec2.Ec2Plugin;

/**
 * Preference page for configuring how external tools (ex: ssh clients) are invoked.
 */
public class ExternalToolsPreferencePage
    extends PreferencePage
    implements IWorkbenchPreferencePage {

    private FileFieldEditor terminalExecutable;
    private FileFieldEditor sshClient;
    private StringFieldEditor sshOptions;
    private FileFieldEditor puttyExecutable;
    private StringFieldEditor sshUser;

    private static final int MAX_FIELD_EDITOR_COLUMNS = 3;

    public ExternalToolsPreferencePage() {
        super("External Tool Configuration");
        setPreferenceStore(Ec2Plugin.getDefault().getPreferenceStore());
        setDescription("External Tool Configuration");
    }

    @Override
    public void init(IWorkbench workbench) {}

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite top = new Composite(parent, SWT.LEFT);

        // Sets the layout data for the top composite's
        // place in its parent's layout.
        top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        top.setLayout(new GridLayout());

        Group sshClientGroup = new Group(top, SWT.LEFT);
        sshClientGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sshClientGroup.setText("SSH Client:");

        if (OsPlatformUtils.isWindows()) {
            String puttyUrl = "http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html";

            WebLinkListener webLinkListener = new WebLinkListener();
            Link l = new Link(sshClientGroup, SWT.NONE);
            l.setText("PuTTY is required for remote shell connections "
                    + "to EC2 instances from Windows.  \nYou can download it for free "
                    + "from <a href=\"" + puttyUrl + "\">" + puttyUrl + "</a>.");
            l.addListener(SWT.Selection, webLinkListener);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.horizontalSpan = MAX_FIELD_EDITOR_COLUMNS;
            l.setLayoutData(data);

            puttyExecutable = newFileFieldEditor(PreferenceConstants.P_PUTTY_EXECUTABLE, "PuTTY Executable:", sshClientGroup);
        } else {
            // For the Mac, we use a custom AppleScript script that we ship with the
            // plugin so we don't need a seperate terminal exectuable.
            if (!OsPlatformUtils.isMac()) {
                terminalExecutable = newFileFieldEditor(PreferenceConstants.P_TERMINAL_EXECUTABLE, "Terminal:", sshClientGroup);
            }

            sshClient = newFileFieldEditor(PreferenceConstants.P_SSH_CLIENT, "SSH Client:", sshClientGroup);

            sshOptions = new StringFieldEditor(PreferenceConstants.P_SSH_OPTIONS, "SSH Options: ", sshClientGroup);
            sshOptions.setPage(this);
            sshOptions.setPreferenceStore(this.getPreferenceStore());
            sshOptions.load();
            sshOptions.fillIntoGrid(sshClientGroup, MAX_FIELD_EDITOR_COLUMNS);
        }

        sshUser = new StringFieldEditor(PreferenceConstants.P_SSH_USER, "SSH User: ", sshClientGroup);
        sshUser.setPage(this);
        sshUser.setPreferenceStore(this.getPreferenceStore());
        sshUser.load();
        sshUser.fillIntoGrid(sshClientGroup, MAX_FIELD_EDITOR_COLUMNS);

        // Reset the layout to three columns after the FieldEditors have mucked with it
        GridLayout layout = (GridLayout)(sshClientGroup.getLayout());
        layout.numColumns = MAX_FIELD_EDITOR_COLUMNS;
        layout.marginWidth = 10;
        layout.marginHeight = 8;

        return top;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        if (terminalExecutable != null) terminalExecutable.loadDefault();
        if (sshClient != null) sshClient.loadDefault();
        if (sshOptions != null) sshOptions.loadDefault();
        if (sshUser != null) sshUser.loadDefault();

        if (puttyExecutable != null) puttyExecutable.loadDefault();

        super.performDefaults();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        if (terminalExecutable != null) terminalExecutable.store();
        if (sshClient != null) sshClient.store();
        if (sshOptions != null) sshOptions.store();
        if (sshUser != null) sshUser.store();

        if (puttyExecutable != null) puttyExecutable.store();

        return super.performOk();
    }

    /*
     * Private Interface
     */

    /**
     * Convenience method for creating a FileFieldEditor.
     *
     * @param preferenceName
     *            The preference managed by this FileFieldEditor.
     * @param label
     *            The label for this FieldEditor.
     * @param parent
     *            The parent for this new FieldEditor widget.
     *
     * @return The new FileFieldEditor.
     */
    private FileFieldEditor newFileFieldEditor(String preferenceName, String label, Composite parent) {
        FileFieldEditor editor = new FileFieldEditor(preferenceName, label, parent);
        editor.setPage(this);
        editor.setPreferenceStore(this.getPreferenceStore());
        editor.load();
        editor.fillIntoGrid(parent, MAX_FIELD_EDITOR_COLUMNS);

        return editor;
    }
}
