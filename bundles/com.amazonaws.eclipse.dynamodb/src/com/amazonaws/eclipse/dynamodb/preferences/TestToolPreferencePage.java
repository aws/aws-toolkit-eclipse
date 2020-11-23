/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolManager;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolVersionTable;

public class TestToolPreferencePage extends AwsToolkitPreferencePage
        implements IWorkbenchPreferencePage {

    public static final String DOWNLOAD_DIRECTORY_PREFERENCE_NAME =
        "com.amazonaws.eclipse.dynamodb.local.preferences.downloadDirectory";

    public static final String DEFAULT_PORT_PREFERENCE_NAME =
        "com.amazonaws.eclipse.dynamodb.local.preferences.defaultPort";

    private static final int MAX_FIELD_EDITOR_COLUMNS = 3;

    private DirectoryFieldEditor downloadDirectory;
    private IntegerFieldEditor defaultPort;

    public TestToolPreferencePage() {
        super("DynamoDB Local Test Tool");
    }

    /** {@inheritDoc} */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(DynamoDBPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected Control createContents(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.LEFT);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (TestToolManager.INSTANCE.isJava7Available()) {

            Composite group = new Composite(composite, SWT.LEFT);
            group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            createDownloadDirectory(group);
            createDefaultPort(group);

            GridLayout layout = (GridLayout) group.getLayout();
            layout.numColumns = MAX_FIELD_EDITOR_COLUMNS;

            TestToolVersionTable versionTable =
                new TestToolVersionTable(composite);

            GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
            data.heightHint = 400;
            versionTable.setLayoutData(data);

        } else {

            Label warning = new Label(composite, SWT.WRAP);
            warning.setText("The DynamoDB Local Test Tool requires a "
                            + "JavaSE-1.8 compatible execution environment!");

            GridData data = new GridData(SWT.LEFT, SWT.TOP, true, false);
            data.widthHint = 500;
            warning.setLayoutData(data);

            Link link = new Link(composite, SWT.NONE);
            link.setText("Click <A>here</A> to configure Installed JREs");
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    PreferencesUtil.createPreferenceDialogOn(
                        composite.getShell(),
                        "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage",
                        null,
                        null
                    );
                }
            });
            link.setToolTipText("Configure Installed JREs");

            data = new GridData(SWT.LEFT, SWT.TOP, true, false);
            data.widthHint = 500;
            link.setLayoutData(data);

        }

        return composite;
    }

    @Override
    protected void performDefaults() {
        if (downloadDirectory != null) {
            downloadDirectory.loadDefault();
        }
        if (defaultPort != null) {
            defaultPort.loadDefault();
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (downloadDirectory != null) {
            downloadDirectory.store();
        }
        if (defaultPort != null) {
            defaultPort.store();
        }
        return super.performOk();
    }

    @Override
    public void performApply() {
        if (downloadDirectory != null) {
            downloadDirectory.store();
        }
        if (defaultPort != null) {
            defaultPort.store();
        }
        super.performApply();
    }

    private void createDownloadDirectory(final Composite composite) {
        downloadDirectory = new DirectoryFieldEditor(
            DOWNLOAD_DIRECTORY_PREFERENCE_NAME,
            "Install Directory:",
            composite
        );
        customizeEditor(composite, downloadDirectory);
    }

    private void createDefaultPort(final Composite composite) {
        defaultPort = new IntegerFieldEditor(
            DEFAULT_PORT_PREFERENCE_NAME,
            "Default Port:",
            composite
        );
        customizeEditor(composite, defaultPort);
    }

    private void customizeEditor(final Composite composite,
                                 final FieldEditor editor) {
        editor.setPage(this);
        editor.setPreferenceStore(getPreferenceStore());
        editor.load();
        editor.fillIntoGrid(composite, MAX_FIELD_EDITOR_COLUMNS);
    }

}
