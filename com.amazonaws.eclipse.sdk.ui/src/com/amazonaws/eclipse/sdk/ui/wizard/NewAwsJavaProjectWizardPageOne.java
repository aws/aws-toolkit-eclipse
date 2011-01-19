/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.sdk.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.sdk.ui.SdkChangeListener;
import com.amazonaws.eclipse.sdk.ui.SdkInstall;
import com.amazonaws.eclipse.sdk.ui.SdkManager;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.SdkVersionInfoComposite;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;

/**
 * The first page of the AWS New Project Wizard. Allows the user to select:
 * <li> A collection of samples to include in the new project
 * <li> A version of the AWS SDK for Java to automatically add to the build path
 */
class NewAwsJavaProjectWizardPageOne extends NewJavaProjectWizardPageOne {

    private SdkVersionInfoComposite sdkVersionInfoComposite;
    private SdkSamplesComposite sdkSamplesComposite;
    private SdkCredentialsComposite sdkCredentialsComposite;

    private static final String ACCESS_KEYS_URL = "http://aws.amazon.com/security-credentials";

    public NewAwsJavaProjectWizardPageOne() {
        setTitle("Create an AWS Java project");
        setDescription("Create a new AWS Java project in the workspace");
    }

    private GridLayout initGridLayout(GridLayout layout, boolean margins) {
        layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        if (margins) {
            layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        } else {
            layout.marginWidth= 0;
            layout.marginHeight= 0;
        }
        return layout;
    }

    /**
     * Returns a list of Sample projects selected by the user on the wizard page.
     * @return a list of Sample projects selected by the user on the wizard page.
     */
    public List<SdkSample> getSelectedSamples() {
        if (sdkSamplesComposite == null) return null;

        return sdkSamplesComposite.getSelectedSamples();
    }

    /**
     * Returns the SDK version set by the user in the wizard page.
     * @return the SDK version set by the user in the wizard page.
     */
    public SdkInstall getSelectedSdkInstall() {
        return sdkVersionInfoComposite.getCurrentSdk();
    }

    /**
     * Returns the access key set by the user in the wizard page.
     * @return the access key set by the user in the wizard page.
     */
    public String getAccessKey() {
        return sdkCredentialsComposite.accessKey;
    }

    /**
     * Returns the secret key set by the user in the wizard page.
     * @return the secret key set by the user in the wizard page.
     */
    public String getSecretKey() {
        return sdkCredentialsComposite.secretKey;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(final Composite parent) {
        initializeDialogUnits(parent);

        final Composite composite = new Composite(parent, SWT.NULL);
        composite.setFont(parent.getFont());
        composite.setLayout(initGridLayout(new GridLayout(1, false), true));
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

        // create ui elements
        Control nameControl = createNameControl(composite);
        nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        Control layoutControl= createProjectLayoutControl(composite);
        layoutControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control workingSetControl= createWorkingSetControl(composite);
        workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Create advanced drop-down
        final ExpandableComposite dropDown = new ExpandableComposite(composite, ExpandableComposite.TWISTIE);
        dropDown.setText("Advanced Settings");

        final Composite advancedSettingsGroup = new Composite(dropDown, SWT.NONE);
        advancedSettingsGroup.setLayout(initGridLayout(new GridLayout(1, false), true));
        advancedSettingsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Control locationControl= createLocationControl(advancedSettingsGroup);
        locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdkSamplesComposite.listenForSdkChange(sdkVersionInfoComposite);

        Control jreControl= createJRESelectionControl(advancedSettingsGroup);
        jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        dropDown.setClient(advancedSettingsGroup);
        GridData g = new GridData(SWT.FILL, SWT.FILL, true, true);
        g.minimumHeight = 100;
        dropDown.setLayoutData(g);

        dropDown.addExpansionListener(new IExpansionListener() {
            public void expansionStateChanged(ExpansionEvent e) {
                composite.getShell().pack(true);
            }

            public void expansionStateChanging(ExpansionEvent e) { }
        });

        setControl(composite);
    }

    /*
     * The first section displayed in the JDT new Java proj wizard.
     *
     * @see org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne#createLocationControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createLocationControl(Composite composite) {

        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("AWS SDK for Java Version");
        sdkVersionInfoComposite = new SdkVersionInfoComposite(group);

        return sdkVersionInfoComposite;
    }

    /*
     * The third section displayed in the JDT new Java proj wizard.
     *
     * @see org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne#createProjectLayoutControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createProjectLayoutControl(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        GridData g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.grabExcessHorizontalSpace = true;
        group.setLayoutData(g);
        group.setText("AWS SDK for Java Samples");
        sdkSamplesComposite = new SdkSamplesComposite(group);

        return sdkSamplesComposite;
    }

    /*
     * The fourth section displayed in the JDT new Java proj wizard.
     *
     * @see org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne#createWorkingSetControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createWorkingSetControl(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        GridData g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.grabExcessHorizontalSpace = true;
        group.setLayoutData(g);
        group.setText("AWS Credentials");
        sdkCredentialsComposite = new SdkCredentialsComposite(group);

        return sdkCredentialsComposite;
    }

    /*
     * Private Interface
     */

    private class SdkCredentialsComposite extends Composite {
        private Text accessKeyText;
        private Text secretKeyText;
        private Button hideSecretKeyCheckbox;

        protected String accessKey;
        protected String secretKey;

        public SdkCredentialsComposite(Composite parent) {
            super(parent, SWT.NONE);

            createControls();
        }

        private void createControls() {
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            layout.marginWidth = 10;
            layout.marginHeight = 8;
            this.setLayout(layout);

            Label description = new Label(this, SWT.WRAP);
            description.setText(
                    "Optionally enter your AWS account credentials from " + ACCESS_KEYS_URL + " to " +
                    "pre-populate a properties file for the samples to use.");
            description.addListener(SWT.Selection, new WebLinkListener());
            GridData g = new GridData(SWT.FILL, SWT.FILL, true, true);
            g.horizontalSpan = 2;
            g.widthHint = sdkSamplesComposite.getSize().x;
            description.setLayoutData(g);

            Label accessKeyFieldLabel = new Label(this, SWT.NONE);
            accessKeyFieldLabel.setText("Access Key: ");
            accessKeyFieldLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, true));
            accessKeyText = new Text(this, SWT.BORDER);
            g = new GridData(SWT.FILL, SWT.TOP, true, true);
            g.grabExcessHorizontalSpace = true;
            accessKeyText.setLayoutData(g);
            accessKeyText.setText(AwsToolkitCore.getDefault().getAccountInfo().getAccessKey());
            accessKey = accessKeyText.getText();
            accessKeyText.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event e) {
                    accessKey = accessKeyText.getText();
                }
            });

            Label secretKeyFieldLabel = new Label(this, SWT.NONE);
            secretKeyFieldLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
            secretKeyFieldLabel.setText("Secret Key: ");
            secretKeyText = new Text(this, SWT.BORDER);
            secretKeyText.setLayoutData(g);
            secretKeyText.setText(AwsToolkitCore.getDefault().getAccountInfo().getSecretKey());
            secretKeyText.setEchoChar('*');
            secretKey = secretKeyText.getText();
            secretKeyText.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event e) {
                    secretKey = secretKeyText.getText();
                }
            });

            // Just to align the checkbox
            new Label(this, SWT.NONE).setText("");

            hideSecretKeyCheckbox = new Button(this, SWT.CHECK);
            hideSecretKeyCheckbox.setText("Show secret access key");
            hideSecretKeyCheckbox.setSelection(false);
            hideSecretKeyCheckbox.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    updateSecretKeyText();
                }
            });
            g = new GridData(GridData.FILL_HORIZONTAL);
            g.verticalIndent = -6;
            hideSecretKeyCheckbox.setLayoutData(g);
        }

        private void updateSecretKeyText() {
            if (hideSecretKeyCheckbox == null) return;
            if (secretKeyText == null) return;

            if (hideSecretKeyCheckbox.getSelection()) {
                secretKeyText.setEchoChar('\0');
            } else {
                secretKeyText.setEchoChar('*');
            }
        }
    }


    /**
     * Composite displaying the samples available in an SDK.
     */
    private static class SdkSamplesComposite extends Composite implements SdkChangeListener {
        List<Button> buttons = new ArrayList<Button>();
        SdkInstall sdkInstall;

        public SdkSamplesComposite(Composite parent) {
            this(parent, SdkManager.getInstance().getDefaultSdkInstall());
        }

        public SdkSamplesComposite(Composite parent, SdkInstall sdkInstall) {
            super(parent, SWT.NONE);
            this.sdkInstall = sdkInstall;

            createControls();
        }

        private void createControls() {
            for ( Control c : this.getChildren()) {
                c.dispose();
            }

            this.setLayout(new GridLayout());
            List<SdkSample> samples = sdkInstall.getSamples();
            for (SdkSample sample : samples) {
                Button button = new Button(this, SWT.CHECK | SWT.WRAP);
                button.setText(sample.getName());
                button.setData(sample);
                buttons.add(button);
                Label label = new Label(this, SWT.WRAP);
                label.setText(sample.getDescription());
                GridData gridData = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
                gridData.horizontalIndent = 25;
                label.setLayoutData(gridData);
            }
        }

        public void listenForSdkChange(SdkVersionInfoComposite scl) {
            scl.registerSdkVersionChangedListener(this);
        }

        public List<SdkSample> getSelectedSamples() {
            List<SdkSample> selectedSamples = new ArrayList<SdkSample>();

            // Bail out early if the list of buttons doesn't exist yet
            if (buttons == null) return selectedSamples;

            for (Button b : buttons) {
                if (b.isDisposed() || b.getSelection() == false) continue;
                selectedSamples.add((SdkSample)b.getData());
            }
            return selectedSamples;
        }

        public void sdkChanged(SdkInstall newSdk) {
            this.sdkInstall = newSdk;
            this.createControls();

            this.layout(true);
            this.getParent().layout(true);
            this.getParent().getParent().layout(true);
        }
    }

    /**
     * Returns the default class path entries to be added on new projects. By default this is the JRE container as
     * selected by the user.
     *
     * @return returns the default class path entries
     */
    @Override
    public IClasspathEntry[] getDefaultClasspathEntries() {
        IClasspathEntry[] defaultClasspath = super.getDefaultClasspathEntries();
        IClasspathEntry[] newClasspath = new IClasspathEntry[defaultClasspath.length + 1];
        for (int i = 0; i < defaultClasspath.length; i++) { newClasspath[i] = defaultClasspath[i]; }
        newClasspath[defaultClasspath.length] = JavaCore.newContainerEntry(new AwsClasspathContainer(getSelectedSdkInstall()).getPath());
        return newClasspath;
    }

}
