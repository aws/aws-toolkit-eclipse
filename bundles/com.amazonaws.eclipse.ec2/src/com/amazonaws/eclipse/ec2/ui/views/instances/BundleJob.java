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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.ec2.AmiToolsVersion;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.RemoteCommandUtils;
import com.amazonaws.eclipse.ec2.ShellCommandException;
import com.amazonaws.eclipse.ec2.ShellCommandResults;
import com.amazonaws.eclipse.ec2.ui.ShellCommandErrorDialog;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RegisterImageRequest;

/**
 * Eclipse Job encapsulating the logic to bundle a specified instance as a new AMI.
 */
class BundleJob extends Job {
    private final Instance instance;
    private final String bundleName;
    private final String s3Bucket;

    private final AccountInfo accountInfo;

    /** Shared utilities for executing remote commands */
    private final static RemoteCommandUtils remoteCommandUtils = new RemoteCommandUtils();

    /** Shared logger */
    private static final Logger logger = Logger.getLogger(BundleJob.class.getName());

    /** The minimum required version of the EC2 AMI Tools */
    private static final AmiToolsVersion requiredVersion = new AmiToolsVersion(1, 3, 31780);


    /**
     * Creates a new Bundle Job to bundle the specified instances and store the
     * data in the specified S3 bucket with the specified bundle name.
     *
     * @param instance
     *            The instance to bundle into an AMI.
     * @param s3Bucket
     *            The S3 bucket to store the bundled image in.
     * @param bundleName
     *            The name of the AMI manifest.
     */
    public BundleJob(Instance instance, String s3Bucket, String bundleName) {
        super("Bundling instance " + instance.getInstanceId());

        this.instance = instance;
        this.bundleName = bundleName;
        this.s3Bucket = s3Bucket;

        this.accountInfo = AwsToolkitCore.getDefault().getAccountInfo();
    }


    /*
     * Job Interface
     */

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Bundling", 110);

        try {
            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            checkRequirements();

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            transferKeys(monitor);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            bundleVolume(monitor);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            deleteKeys(monitor);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            uploadBundle(monitor);

            if (monitor.isCanceled()) return Status.CANCEL_STATUS;
            final String amiName = registerBundle(monitor);
            final String message = "Successfully created AMI " + amiName;

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_INFORMATION | SWT.OK);
                    messageBox.setMessage(message);
                    messageBox.setText("AMI Bundling Complete");
                    messageBox.open();
                }
            });

            Status status = new Status(Status.INFO, Ec2Plugin.PLUGIN_ID, message);
            StatusManager.getManager().handle(status, StatusManager.LOG);

            logger.info("Successfully created AMI: " + amiName);
        } catch (final ShellCommandException sce) {

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    new ShellCommandErrorDialog(sce).open();
                }
            });

            /*
             * We return a warning status instead of an error status since the
             * warning status doesn't cause another dialog to pop up (since
             * we're already displaying our own).
             */
            return new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID,
                    "Unable to bundle instance: " + sce.getMessage(), sce);
        } catch (Exception e) {
            e.printStackTrace();
            return new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                    "Unable to bundle instance: " + e.getMessage(), e);
        }

        return Status.OK_STATUS;
    }


    /*
     * Private Interface
     */

    /**
     * Checks requirements such as minimum required AMI tools version on the
     * remote host before the bundling process begins to try and detect anything
     * that will cause problems.
     *
     * @throws Exception
     *             If this method detected a known problem.
     */
    private void checkRequirements() throws Exception {
        /*
         * We seen bundling fail for older AMI tools versions that can't
         * automatically figure out the proper architecture
         * (ex: ec2-ami-tools-version 1.3-20041), so we want to warn the user
         * early if we know there's going to be a problem.
         */

         /*
          * TODO: We should also do more error checking in general,
          *       such as S3 bucket validation.
          */

        List<ShellCommandResults> results =
            remoteCommandUtils.executeRemoteCommand("ec2-ami-tools-version", instance);

        // Find the output from the first successful attempt
        String output = null;
        for (ShellCommandResults result : results) {
            if (result.exitCode == 0) {
                output = result.output;
                break;
            }
        }

        // If we can't find the output from a successful run, just bail out
        if (output == null) {
            return;
        }

        AmiToolsVersion version = null;
        try {
            version = new AmiToolsVersion(output);
        } catch (ParseException pe) {
            /*
             * If we can't parse the AMI tools version, we want to bail out
             * instead of stopping the bundling operation. The version number
             * format could have changed, for example, and we wouldn't want to
             * break everybody if that happened.
             */
            return;
        }

        if (requiredVersion.isGreaterThan(version)) {
            throw new Exception("The version of the EC2 AMI Tools on the remote host is too old " +
                    "(" + version.toString() + ").  " +
                    "The AWS Toolkit for Eclipse requires version " + requiredVersion.toString() +
                    " or greater.  \n\nFor information on updating the EC2 AMI Tools on your " +
                    "instance, consult the EC2 Developer Guide available from: http://aws.amazon.com/documentation ");
        }
    }

    /**
     * Registers the uploaded bundle as an EC2 AMI.
     *
     * @param monitor The progress monitor for this job.
     * @return The ID of the AMI created when registering the bundle.
     */
    private String registerBundle(IProgressMonitor monitor) {
        monitor.subTask("Registering bundle");

        AmazonEC2 ec2 = Ec2Plugin.getDefault().getDefaultEC2Client();

        RegisterImageRequest request = new RegisterImageRequest();
        request.setImageLocation(s3Bucket + "/" + bundleName + ".manifest.xml");
        String amiName = ec2.registerImage(request).getImageId();

        monitor.worked(5);
        return amiName;
    }

    /**
     * Uploads the bundled volume to S3.
     *
     * @param monitor The progress monitor for this job.
     * @throws IOException
     * @throws InterruptedException
     */
    private void uploadBundle(IProgressMonitor monitor) throws IOException,
            InterruptedException {
        monitor.subTask("Uploading bundle");

        String manifestFile = "/mnt/" + bundleName + ".manifest.xml";

        String uploadCommand = "ec2-upload-bundle "
                + " --bucket " + s3Bucket
                + " --manifest " + manifestFile
                + " --access-key " + accountInfo.getAccessKey()
                + " --secret-key " + accountInfo.getSecretKey();

        Region region = RegionUtils.getCurrentRegion();
        String regionId = region.getId().toLowerCase();

        /*
         * We need to make sure that the bundle is uploaded to the correct S3
         * location depending on what region we're going to register the AMI in.
         */
        if (regionId.startsWith("eu")) {
            uploadCommand += " --location EU";
        } else if (regionId.startsWith("us")) {
            uploadCommand += " --location US";
        }

        remoteCommandUtils
                .executeRemoteCommand(uploadCommand, instance);

        monitor.worked(35);
    }

    /**
     * Connects to the EC2 instance to bundle the volume.
     *
     * @param monitor The progress monitor for this job.
     * @throws IOException
     * @throws InterruptedException
     */
    private void bundleVolume(IProgressMonitor monitor) throws IOException,
            InterruptedException {
        monitor.subTask("Bundling volume");

        File privateKeyFile = new File(accountInfo.getEc2PrivateKeyFile());
        String privateKeyBaseFileName = privateKeyFile.getName();

        File certificateFile = new File(accountInfo.getEc2CertificateFile());
        String certificateBaseFileName = certificateFile.getName();

        String bundleCommand = "ec2-bundle-vol --destination /mnt "
                + " --privateKey /mnt/" + privateKeyBaseFileName
                + " --cert /mnt/" + certificateBaseFileName
                + " --user " + accountInfo.getUserId()
                + " --batch --prefix " + bundleName;

        remoteCommandUtils.executeRemoteCommand(bundleCommand, instance);

        monitor.worked(50);
    }

    /**
     * Securely transfers the user's key and certificate to the EC2 instance so that
     * the bundle can be signed.
     *
     * @param monitor The progress monitor for this job.
     * @throws IOException
     * @throws InterruptedException
     */
    private void transferKeys(IProgressMonitor monitor) throws IOException,
            InterruptedException {
        monitor.subTask("Transfering keys");

        File privateKeyFile = new File(accountInfo.getEc2PrivateKeyFile());
        String privateKeyBaseFileName = privateKeyFile.getName();

        File certificateFile = new File(accountInfo.getEc2CertificateFile());
        String certificateBaseFileName = certificateFile.getName();

        remoteCommandUtils.copyRemoteFile(accountInfo.getEc2CertificateFile(), "/mnt/" + certificateBaseFileName, instance);
        monitor.worked(5);

        remoteCommandUtils.copyRemoteFile(accountInfo.getEc2PrivateKeyFile(), "/mnt/" + privateKeyBaseFileName, instance);
        monitor.worked(5);
    }

    /**
     * Deletes the user's key and certificate that were previously transfered.
     *
     * @param monitor
     *            The progress monitor for this job.
     * @throws IOException
     *             If any problems were encountered deleting the EC2 key and
     *             cert.
     */
    private void deleteKeys(IProgressMonitor monitor) {
        monitor.subTask("Deleting keys");

        File privateKeyFile = new File(accountInfo.getEc2PrivateKeyFile());
        String privateKeyBaseFileName = privateKeyFile.getName();

        File certificateFile = new File(accountInfo.getEc2CertificateFile());
        String certificateBaseFileName = certificateFile.getName();

        try {
            remoteCommandUtils.executeRemoteCommand("rm /mnt/" + certificateBaseFileName, instance);
        } catch (IOException e) {
            Status status = new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID,
                    "Unable to remove EC2 certificate: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
        monitor.worked(5);

        try {
            remoteCommandUtils.executeRemoteCommand("rm /mnt/" + privateKeyBaseFileName, instance);
        } catch (IOException e) {
            Status status = new Status(IStatus.WARNING, Ec2Plugin.PLUGIN_ID,
                    "Unable to remove EC2 private key: " + e.getMessage(), e);
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }
        monitor.worked(5);
    }
}
