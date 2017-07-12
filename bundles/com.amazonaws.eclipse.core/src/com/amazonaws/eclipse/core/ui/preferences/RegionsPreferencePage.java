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
package com.amazonaws.eclipse.core.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.preferences.PreferenceConstants;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.WebLinkListener;

/**
 * Preference page for AWS region preferences.
 */
public class RegionsPreferencePage
        extends AwsToolkitPreferencePage
        implements IWorkbenchPreferencePage {

    /** Combo box allowing the user to override the default AWS region */
    private Combo regionsCombo;

    /**
     * Constructs a RegionPreferencesPage and sets the title and description.
     */
    public RegionsPreferencePage() {
        super("Region Preferences");
        this.setPreferenceStore(AwsToolkitCore.getDefault().getPreferenceStore());
        this.setDescription("Region Preferences");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        String regionId = (String)regionsCombo.getData(regionsCombo.getText());
        getPreferenceStore().setValue(PreferenceConstants.P_DEFAULT_REGION, regionId);
        return super.performOk();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.LEFT);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        WebLinkListener webLinkListener = new WebLinkListener();
        createRegionSection(composite, webLinkListener);
        createSpacer(composite);

        return composite;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {}

    /**
     * Creates the region section in this preference page.
     *
     * @param parent
     *            The parent composite in which the region section should be
     *            placed.
     * @param webLinkListener
     *            The link listener to use for any Links.
     */
    private void createRegionSection(Composite parent, WebLinkListener webLinkListener) {
        Group regionGroup = newGroup("Regions:", parent);

        String regionsHelpLinkText = "AWS regions allow you to position your AWS resources in different geographical areas, " +
                                 "enabling you to keep your application's data close to your customers, and add redundancy to your system, since " +
                                 "each region is isolated from each other.\n";
        newLink(webLinkListener, regionsHelpLinkText, regionGroup);

        Label label = new Label(regionGroup, SWT.NONE);
        label.setText("Default Region:");

        regionsCombo = new Combo(regionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        regionsCombo.removeAll();

        String currentDefaultRegionId = getPreferenceStore().getString(PreferenceConstants.P_DEFAULT_REGION);
        for (Region region : RegionUtils.getRegions()) {
            regionsCombo.add(region.getName());
            regionsCombo.setData(region.getName(), region.getId());

            if (currentDefaultRegionId.equals(region.getId())) {
                regionsCombo.setText(region.getName());
            }
        }
        if (regionsCombo.getText() == null || regionsCombo.getText().length() == 0) {
            regionsCombo.select(0);
        }

        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        regionsCombo.setLayoutData(gridData);

        tweakLayout((GridLayout)regionGroup.getLayout());
    }
}
