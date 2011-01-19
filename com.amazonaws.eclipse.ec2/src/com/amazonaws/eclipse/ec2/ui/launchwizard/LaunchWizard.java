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
package com.amazonaws.eclipse.ec2.ui.launchwizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2InstanceLauncher;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.views.instances.InstanceView;

/**
 * Wizard for launching EC2 instances.
 */
public class LaunchWizard extends Wizard {

    /** Wizard page for selecting the AMI to launch */
    private AmiSelectionWizardPage amiSelectionWizardPage;

    /** Wizard page for selecting launch options */
    private LaunchWizardPage launchOptionsWizardPage;

    /** The EC2 AMI being launched by this wizard */
    private Image image;

    /**
     * Creates a new launch wizard. Since no AMI has been specified in this
     * constructor form, the wizard will include an extra page at the beginning
     * to allow the user to select an AMI.
     */
    public LaunchWizard() {
        this(null);
    }

	/**
	 * Creates a new launch wizard to launch the specified AMI.
	 *
	 * @param image
	 *            The AMI this launch wizard will launch.
	 */
	public LaunchWizard(Image image) {
		this.image = image;

		if (image == null) {
		    amiSelectionWizardPage = new AmiSelectionWizardPage();
		    this.addPage(amiSelectionWizardPage);
		}

		launchOptionsWizardPage = new LaunchWizardPage(image);

		this.addPage(launchOptionsWizardPage);
		this.setNeedsProgressMonitor(true);
		this.setWindowTitle("Launch Amazon EC2 Instances");

        /*
         * TODO: Grab a better image for the wizard...
         */
		this.setDefaultPageImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
	}

    /**
     * Returns the Amazon Machine Image this launch wizard is launching. This
     * could be an image the user selected from the AMI browser, or it could be
     * an image the user selected within in the launch wizard.
     *
     * @return The Amazon Machine Image this launch wizard is launching.
     */
	public Image getImageToLaunch() {
	    if (image != null) return image;

	    return amiSelectionWizardPage.getSelectedAmi();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {

		/*
		 * TODO: performFinish executes in the UI thread. It might be nice to
		 * run this in a separate thread in case of network issues, but it's
		 * probably not the most critical piece to get out of the UI thread.
		 */

		String keyPairName = launchOptionsWizardPage.getKeyPairName();

		Ec2InstanceLauncher launcher = new Ec2InstanceLauncher(getImageToLaunch().getImageId(), keyPairName);
		launcher.setNumberOfInstances(launchOptionsWizardPage.getNumberOfInstances());
		launcher.setAvailabilityZone(launchOptionsWizardPage.getAvailabilityZone());
		launcher.setInstanceType(launchOptionsWizardPage.getInstanceTypeId());
		launcher.setUserData(launchOptionsWizardPage.getUserData());
		launcher.setSecurityGroup(launchOptionsWizardPage.getSecurityGroup());

		try {
			launcher.launch();
			activateInstanceView();
		} catch (Exception e) {
			String message = "Unable to launch instances: " + e.getMessage();
			Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID, message, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);

			MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.OK);
			messageBox.setMessage(message);
			messageBox.setText("Launch Error");
			messageBox.open();

			return false;
		}

		return true;
	}

	private void activateInstanceView() {
		try {
			IViewPart viewPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
									.findView("com.amazonaws.eclipse.ec2.ui.views.InstanceView");
			if (viewPart != null) {
				InstanceView instanceView = (InstanceView)viewPart;
				instanceView.refreshData();
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(instanceView);
			}
		} catch (Exception e) {
			Status status = new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID,
					"Unable to activate instance view: " + e.getMessage(), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
		}
	}

}
