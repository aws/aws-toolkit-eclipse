package com.amazonaws.eclipse.sdk.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;

/**
 * Abstract preferences page for determining the sdk installation directory, as
 * well whether to download the sdk automatically or not.
 */
public abstract class AbstractSDKPreferencesPage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    protected FieldEditor downloadAutomaticallyFieldEditor;
    protected DirectoryFieldEditor downloadDirectory;

    public AbstractSDKPreferencesPage(String name) {
        super(name);
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);

        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

        Composite downloadDirectoryComposite = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(downloadDirectoryComposite);
        downloadDirectory = new DirectoryFieldEditor(getDownloadDirectoryPreferenceName(), "SDK directory",
                downloadDirectoryComposite);      
        downloadDirectory.setPreferenceStore(getPreferenceStore());

        Composite downloadAutomaticallyComposite = new Composite(composite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(downloadAutomaticallyComposite);
        downloadAutomaticallyFieldEditor = new BooleanFieldEditor(getDownloadAutomaticallyPreferenceName(),
                "Download new SDKs automatically", downloadAutomaticallyComposite);
        downloadAutomaticallyFieldEditor.setPreferenceStore(getPreferenceStore());
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(downloadAutomaticallyComposite);
        Button checkNow = new Button(downloadAutomaticallyComposite, SWT.None);
        checkNow.setText("Check for updates now");
        checkNow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkForSDKUpdates();
            }
        });

        downloadAutomaticallyFieldEditor.load();
        downloadDirectory.load();
        
        return composite;
    }

    protected abstract String getDownloadAutomaticallyPreferenceName();

    protected abstract String getDownloadDirectoryPreferenceName();

    protected abstract void checkForSDKUpdates();

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        downloadAutomaticallyFieldEditor.loadDefault();
        downloadDirectory.loadDefault();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        downloadAutomaticallyFieldEditor.store();
        downloadDirectory.store();
        return super.performOk();
    }

}
