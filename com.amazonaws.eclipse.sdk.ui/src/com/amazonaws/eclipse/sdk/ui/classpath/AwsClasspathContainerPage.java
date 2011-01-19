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
package com.amazonaws.eclipse.sdk.ui.classpath;

import java.io.IOException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.sdk.ui.SdkInstall;
import com.amazonaws.eclipse.sdk.ui.SdkProjectMetadata;
import com.amazonaws.eclipse.sdk.ui.SdkVersionInfoComposite;

/**
 * A Wizard page to modify the version of the AWS SDK for Java being used by
 * a particular project.
 */
public class AwsClasspathContainerPage extends WizardPage 
        implements IClasspathContainerPage, IClasspathContainerPageExtension {

	private IJavaProject project;
	private SdkVersionInfoComposite sdkVersionInfoComposite;

	public AwsClasspathContainerPage() {
		super("AWS SDK for Java");
		
		setDescription("Add the AWS SDK for Java to your project's classpath.");
		setTitle("AWS SDK for Java");
		
		ImageRegistry imageRegistry = AwsToolkitCore.getDefault().getImageRegistry();
        setImageDescriptor(imageRegistry.getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
	    SdkInstall currentSdk = null;
	    try {
	        SdkProjectMetadata sdkProjectMetadataFile = new SdkProjectMetadata(project.getProject());

            currentSdk = new SdkInstall(sdkProjectMetadataFile.getSdkInstallRootForProject());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (currentSdk.getRootDirectory() == null) {
            sdkVersionInfoComposite = new SdkVersionInfoComposite(parent);
        } else {
            sdkVersionInfoComposite = new SdkVersionInfoComposite(parent, currentSdk);
        }
		this.setControl(sdkVersionInfoComposite);
	}
	
	public boolean finish() {
	    try {
	        SdkProjectMetadata sdkProjectMetadataFile = new SdkProjectMetadata(project.getProject());
	        AwsSdkClasspathUtils.removeAwsSdkFromProjectClasspath(
	                project, new SdkInstall(sdkProjectMetadataFile.getSdkInstallRootForProject()));
            sdkVersionInfoComposite.getCurrentSdk().writeMetadataToProject(project);
	        AwsSdkClasspathUtils.addAwsSdkToProjectClasspath(
	                project, sdkVersionInfoComposite.getCurrentSdk());
	    } catch (IOException e) {
	        e.printStackTrace();
	        return false;
	    }
		
		return true;
	}

	/**
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension#initialize(org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathEntry[])
	 */
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
	    this.project = project;
	}
	
	/**
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#getSelection()
	 */
	public IClasspathEntry getSelection() {
	    return null;
	}

	/**
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#setSelection(org.eclipse.jdt.core.IClasspathEntry)
	 */
	public void setSelection(IClasspathEntry containerEntry) {}

}
