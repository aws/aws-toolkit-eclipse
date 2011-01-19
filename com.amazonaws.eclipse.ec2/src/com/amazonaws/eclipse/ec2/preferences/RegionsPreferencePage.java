/*
 * Copyright 2008-2011 Amazon Technologies, Inc.
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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.eclipse.ec2.Ec2ClientFactory;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.Region;

/**
 * Preference page for AWS region preferences.
 */
public class RegionsPreferencePage
		extends AwsToolkitPreferencePage
		implements IWorkbenchPreferencePage {

	/** Combo box allowing the user to override the default EC2 region */
	private Combo ec2RegionsCombo;

	/**
	 * Constructs a RegionPreferencesPage and sets the title and description.
	 */
	public RegionsPreferencePage() {
		super("Region Preferences");
		this.setPreferenceStore(Ec2Plugin.getDefault().getPreferenceStore());
		this.setDescription("Region Preferences");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		// Save the region and region endpoint
		String selectedEc2Region = ec2RegionsCombo.getText();
		String selectedEc2Endpoint = (String)ec2RegionsCombo.getData(selectedEc2Region);
		getPreferenceStore().setValue(PreferenceConstants.P_EC2_REGION_NAME, selectedEc2Region);
		getPreferenceStore().setValue(PreferenceConstants.P_EC2_REGION_ENDPOINT, selectedEc2Endpoint);

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
	public void init(IWorkbench workbench) {
	}

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

		String regionsFeatureGuideUrl = "http://developer.amazonwebservices.com/connect/entry.jspa?externalID=1927";
		String regionsHelpLinkText = "Amazon EC2 regions allow you run EC2 instances in multiple geographically distinct regions.\n" +
		"<a href=\"" + regionsFeatureGuideUrl + "\">More information on EC2 regions</a>.";
		this.newLink(webLinkListener, regionsHelpLinkText, regionGroup);

		createSpacer(regionGroup);

		Label label = new Label(regionGroup, SWT.NONE);
		label.setText("EC2 Region:");

		ec2RegionsCombo = new Combo(regionGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		ec2RegionsCombo.removeAll();
		String selectedRegionName = getPreferenceStore().getString(PreferenceConstants.P_EC2_REGION_NAME);
		String selectedRegionEndpoint = getPreferenceStore().getString(PreferenceConstants.P_EC2_REGION_ENDPOINT);
		ec2RegionsCombo.add(selectedRegionName);
		ec2RegionsCombo.setData(selectedRegionName, selectedRegionEndpoint);
		ec2RegionsCombo.select(0);

		GridData gridData = new GridData();
		gridData.horizontalSpan = 2;
		ec2RegionsCombo.setLayoutData(gridData);

		describeRegions();

		tweakLayout((GridLayout)regionGroup.getLayout());
	}

	/**
	 * Queries EC2 for a list of available regions and updates the combo
	 * selection box with the choices.
	 */
	private void describeRegions() {
		new DescribeRegionsThread().start();
	}

	/**
	 * Simple thread to wrap the EC2 service call to request a list of EC2
	 * regions and update the display with the results.
	 */
	private class DescribeRegionsThread extends Thread {

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			Ec2ClientFactory clientFactory = new Ec2ClientFactory();
			AmazonEC2 client = clientFactory.getAwsClient();

			try {
				DescribeRegionsResult response = client.describeRegions(new DescribeRegionsRequest());
				final List<Region> regions = response.getRegions();

				final String selectedRegion = Ec2Plugin.getDefault().getPreferenceStore().getString(
						PreferenceConstants.P_EC2_REGION_NAME);

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						ec2RegionsCombo.removeAll();
						for (Region region : regions) {
							ec2RegionsCombo.add(region.getRegionName());
							ec2RegionsCombo.setData(region.getRegionName(), region.getEndpoint());
						}
						ec2RegionsCombo.pack();
						ec2RegionsCombo.getParent().layout();

						for (int i = 0; i < ec2RegionsCombo.getItems().length; i++) {
							if (ec2RegionsCombo.getItem(i).equals(selectedRegion)) {
								ec2RegionsCombo.select(i);
								break;
							}
						}
					}
				});

			} catch (AmazonClientException e) {
				Status status = new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID,
						"Unable to list EC2 regions: " + e.getMessage(), e);
				StatusManager.getManager().handle(status, StatusManager.LOG);
			}
		}
	}

}
