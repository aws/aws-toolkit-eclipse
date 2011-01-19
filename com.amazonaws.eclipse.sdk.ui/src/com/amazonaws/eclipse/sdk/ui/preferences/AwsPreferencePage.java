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
package com.amazonaws.eclipse.sdk.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.sdk.ui.SdkPlugin;
import com.amazonaws.eclipse.sdk.ui.SdkVersionInfoComposite;

/**
 * A preference page for the AWS SDK for Java plugin. It allows users to modify
 * the default SDK version to be added to new Java projects.
 * @author petrescu
 *
 */
public class AwsPreferencePage extends PreferencePage
    implements IWorkbenchPreferencePage {

    public AwsPreferencePage() {
        super("AWS SDK for Java Preferences");
        setPreferenceStore(SdkPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new SdkVersionInfoComposite(parent);
        return composite;
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {}

}
