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
package com.amazonaws.eclipse.sdk.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Composite displaying the SDK version information.
 */
public class SdkVersionInfoComposite extends Composite implements SdkChangeListener {
    private Label locationLabel;
    private final Combo versions;
    private final List<JavaSdkInstall> sdkInstalls;

    private JavaSdkInstall chosenSdk;

    public void registerSdkVersionChangedListener(final SdkChangeListener sdkListener) {
        versions.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                sdkListener.sdkChanged(sdkInstalls.get(((Combo) e.widget).getSelectionIndex()));
            }

            public void widgetSelected(SelectionEvent e) {
                sdkListener.sdkChanged(sdkInstalls.get(((Combo) e.widget).getSelectionIndex()));
            }
        });
    }

    public SdkVersionInfoComposite(Composite parent, JavaSdkInstall chosenSdk) {
        super(parent, SWT.NONE);

        this.setLayout(new GridLayout());

        JavaSdkManager sdkManager = JavaSdkManager.getInstance();

        versions = new Combo(this, SWT.READ_ONLY);
        locationLabel = new Label(this, SWT.NONE);
        locationLabel.setText("Location:");
        this.chosenSdk = chosenSdk;

        registerSdkVersionChangedListener(this);

        sdkInstalls = sdkManager.getSdkInstalls();
        for (int i = 0; i < sdkInstalls.size(); ++i) {
            versions.add("AWS SDK for Java " + sdkInstalls.get(i).getVersion());
            if (sdkInstalls.get(i).getVersion().equals(chosenSdk.getVersion())) {
                versions.select(i);
                this.sdkChanged(sdkInstalls.get(i));
            }
        }
    }

    public SdkVersionInfoComposite(Composite parent) {
        this(parent, (JavaSdkInstall)JavaSdkManager.getInstance().getDefaultSdkInstall());
    }

    public void sdkChanged(JavaSdkInstall sdkInstall) {
        this.chosenSdk = sdkInstall;

        locationLabel.dispose();

        this.setLayout(new GridLayout());
        locationLabel = new Label(this, SWT.WRAP);
        locationLabel.setText("Location: " + sdkInstall.getRootDirectory().toString());

        this.layout(true);
        this.getParent().layout(true);
        this.getParent().getParent().layout(true);
    }

    public JavaSdkInstall getCurrentSdk() {
        return chosenSdk;
    }
}

