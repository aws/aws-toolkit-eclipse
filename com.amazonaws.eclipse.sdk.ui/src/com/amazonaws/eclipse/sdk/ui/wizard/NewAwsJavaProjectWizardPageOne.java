/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.sdk.ui.JavaSdkInstall;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;
import com.amazonaws.eclipse.sdk.ui.JavaSdkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkChangeListener;
import com.amazonaws.eclipse.sdk.ui.SdkSample;
import com.amazonaws.eclipse.sdk.ui.SdkVersionInfoComposite;
import com.amazonaws.eclipse.sdk.ui.classpath.AwsClasspathContainer;

/**
 * The first page of the AWS New Project Wizard. Allows the user to select:
 * <li> Account credentials
 * <li> A collection of samples to include in the new project
 * <li> A version of the AWS SDK for Java to automatically add to the build path
 */
class NewAwsJavaProjectWizardPageOne extends NewJavaProjectWizardPageOne {

    private SdkVersionInfoComposite sdkVersionInfoComposite;
    private SdkSamplesComposite sdkSamplesComposite;
    private AccountSelectionComposite accountSelectionComposite;

    public NewAwsJavaProjectWizardPageOne() {
        setTitle("Create an AWS Java project");
        setDescription("Create a new AWS Java project in the workspace");
    }

    private GridLayout initGridLayout(GridLayout layout, boolean margins) {
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        if ( margins ) {
            layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
            layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        } else {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        return layout;
    }

    /**
     * Returns a list of Sample projects selected by the user on the wizard page.
     * @return a list of Sample projects selected by the user on the wizard page.
     */
    public List<SdkSample> getSelectedSamples() {
        if (sdkSamplesComposite == null) {
            return null;
        }

        return sdkSamplesComposite.getSelectedSamples();
    }

    /**
     * Returns the SDK version set by the user in the wizard page.
     * @return the SDK version set by the user in the wizard page.
     */
    public JavaSdkInstall getSelectedSdkInstall() {
        return sdkVersionInfoComposite.getCurrentSdk();
    }

    /**
     * Returns the access key set by the user in the wizard page.
     * @return the access key set by the user in the wizard page.
     */
    public String getAccessKey() {
        return AwsToolkitCore.getDefault().getAccountInfo(accountSelectionComposite.getSelectedAccountId()).getAccessKey();
    }

    /**
     * Returns the secret key set by the user in the wizard page.
     * @return the secret key set by the user in the wizard page.
     */
    public String getSecretKey() {
        return AwsToolkitCore.getDefault().getAccountInfo(accountSelectionComposite.getSelectedAccountId()).getSecretKey();
    }

    private ScrolledComposite scrolledComp;
    private ControlAdapter resizeListener;

    @Override
    public void createControl(final Composite parent) {

        initializeDialogUnits(parent);

        scrolledComp = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolledComp.setExpandHorizontal(true);
        scrolledComp.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComp);

        final Composite composite = new Composite(scrolledComp, SWT.NULL);

        composite.setFont(parent.getFont());
        composite.setLayout(initGridLayout(new GridLayout(1, false), true));
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        scrolledComp.setContent(composite);

        resizeListener = new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = scrolledComp.getClientArea();
                scrolledComp.setMinSize(composite.computeSize(r.width, SWT.DEFAULT));
            }
        };
        scrolledComp.addControlListener(resizeListener);

        Control nameControl = createNameControl(composite);
        nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameControl.addListener(SWT.Modify, new Listener() {
            public void handleEvent(Event event) {
                if ( !canFlipToNextPage() ) {
                    setPageComplete(false);
                }
            }
        });

        Control accountSelectionControl = createAccountSelectionComposite(composite);
        accountSelectionControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Check to see if we have an SDK. If we don't, we need to wait before
        // continuing
        JavaSdkManager sdkManager = JavaSdkManager.getInstance();
        synchronized ( sdkManager ) {
            JavaSdkInstall defaultSDKInstall = sdkManager.getDefaultSdkInstall();
            if ( defaultSDKInstall == null ) {
                setPageComplete(false);

                Job installationJob = sdkManager.getInstallationJob();
                if ( installationJob == null ) {
                    JavaSdkPlugin
                            .getDefault()
                            .getLog()
                            .log(new Status(IStatus.ERROR, JavaSdkPlugin.PLUGIN_ID,
                                    "Unable to check status of AWS SDK for Java download"));
                    return;
                }

                final Composite pleaseWait = new Composite(composite, SWT.None);
                pleaseWait.setLayout(initGridLayout(new GridLayout(1, false), true));
                pleaseWait.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                Label label = new Label(pleaseWait, SWT.None);
                label.setText("The AWS SDK for Java is currently downloading.  Please wait while it completes.");
                label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                ProgressBar progressBar = new ProgressBar(pleaseWait, SWT.INDETERMINATE);
                progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                installationJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                pleaseWait.dispose();
                                createSDKOptionsControls(composite);
                                composite.getParent().layout();
                                composite.getShell().pack(true);
                                composite.getParent().redraw();
                                setPageComplete(true);
                            }
                        });
                    }
                });

            } else {
                createSDKOptionsControls(composite);
            }
        }

        setControl(scrolledComp);
    }

    /*
     * This is kind of hacky, but the parent class doesn't give us any other way
     * to hook into the validation state. We need to do this to make sure that
     * the customer can't progress to the next page before the SDK has been
     * bootstrapped.
     */
    @Override
    public void setPageComplete(boolean complete) {
        super.setPageComplete(complete && sdkVersionInfoComposite != null);
    }

    private void createSDKOptionsControls(final Composite composite) {

        Control samplesComposite = createSamplesComposite(composite);
        samplesComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Create advanced drop-down
        final ExpandableComposite dropDown = new ExpandableComposite(composite, ExpandableComposite.TWISTIE);
        dropDown.setText("Advanced Settings");

        final Composite advancedSettingsGroup = new Composite(dropDown, SWT.NONE);
        advancedSettingsGroup.setLayout(initGridLayout(new GridLayout(1, false), true));
        advancedSettingsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createSdkVersionComposite(advancedSettingsGroup);
        sdkVersionInfoComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sdkSamplesComposite.listenForSdkChange(sdkVersionInfoComposite);

        Control jreControl= createJRESelectionControl(advancedSettingsGroup);
        jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        dropDown.setClient(advancedSettingsGroup);
        GridData g = new GridData(SWT.FILL, SWT.FILL, true, true);
        g.minimumHeight = 100;
        dropDown.setLayoutData(g);

        dropDown.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                resizeListener.controlResized(null);
            }
        });
    }

    protected Control createSdkVersionComposite(Composite composite) {

        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("AWS SDK for Java Version");
        sdkVersionInfoComposite = new SdkVersionInfoComposite(group);

        return sdkVersionInfoComposite;
    }

    protected Control createSamplesComposite(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        GridData g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.grabExcessHorizontalSpace = true;
        group.setLayoutData(g);
        group.setText("AWS SDK for Java Samples");

        sdkSamplesComposite = new SdkSamplesComposite(group, JavaSdkManager.getInstance().getDefaultSdkInstall());

        return sdkSamplesComposite;
    }

    protected Control createAccountSelectionComposite(Composite composite) {
        Group group = new Group(composite, SWT.NONE);
        group.setLayout(new GridLayout());
        GridData g = new GridData(SWT.FILL, SWT.TOP, true, false);
        g.grabExcessHorizontalSpace = true;
        group.setLayoutData(g);
        group.setText("AWS Credentials");
        accountSelectionComposite = new AccountSelectionComposite(group, SWT.None);

        return accountSelectionComposite;
    }

    /**
     * Composite displaying the samples available in an SDK.
     */
    private static class SdkSamplesComposite extends Composite implements SdkChangeListener {
        List<Button> buttons = new ArrayList<Button>();
        JavaSdkInstall sdkInstall;

        public SdkSamplesComposite(Composite parent, JavaSdkInstall sdkInstall) {
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
                if (sample.getName() == null
                 || sample.getDescription() == null) {
                    // Sanity check - skip samples without names and
                    // descriptions.
                    continue;
                }

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
            if (buttons == null) {
                return selectedSamples;
            }

            for (Button b : buttons) {
                if (b.isDisposed() || b.getSelection() == false) {
                    continue;
                }
                selectedSamples.add((SdkSample)b.getData());
            }
            return selectedSamples;
        }

        public void sdkChanged(JavaSdkInstall newSdk) {
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
