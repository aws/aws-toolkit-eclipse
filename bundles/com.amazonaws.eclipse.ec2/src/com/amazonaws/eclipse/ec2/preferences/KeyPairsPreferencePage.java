/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;

/**
 * Preference page displaying key pairs for a user's account.
 */
public class KeyPairsPreferencePage extends AwsToolkitPreferencePage implements
        IWorkbenchPreferencePage {

    public KeyPairsPreferencePage() {
        super("Amazon EC2 Key Pairs");
        this.setPreferenceStore(Ec2Plugin.getDefault().getPreferenceStore());
        setDescription("Key Pair Management");

        noDefaultAndApplyButton();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Group group = newGroup("Key Pairs:", composite);

        newLabel("Key pairs allow you to securely log into your EC2 instances.", group);

        KeyPairComposite keyPairSelectionTable = new KeyPairComposite(group);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = 150;
        keyPairSelectionTable.setLayoutData(gridData);

        return composite;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performHelp()
     */
    @Override
    public void performHelp() {
        String keyPairHelpResource = "/" + Ec2Plugin.PLUGIN_ID + "/html/concepts/keyPairs.html";
        PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(keyPairHelpResource);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {}

}
