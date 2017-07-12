package com.amazonaws.eclipse.opsworks.deploy.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.AppType;
import com.amazonaws.services.opsworks.model.CreateAppRequest;
import com.amazonaws.services.opsworks.model.CreateDeploymentRequest;
import com.amazonaws.services.opsworks.model.CreateDeploymentResult;
import com.amazonaws.services.opsworks.model.DeploymentCommand;
import com.amazonaws.services.opsworks.model.DeploymentCommandName;
import com.amazonaws.services.opsworks.model.DescribeAppsRequest;
import com.amazonaws.services.opsworks.model.EnvironmentVariable;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class DeployUtils {

    public static File repackWarFileToZipFile(File warFile) throws IOException {
        File unpackDir = createTempDirectory();
        File zipFile = File.createTempFile("eclipse-opsworks-content-zip-", null);

        // Unpack the .war file to the temp directory
        unpackWarFile(warFile, unpackDir);

        // Zip it
        ZipUtils.createZipFileOfDirectory(unpackDir, zipFile);

        return zipFile;
    }

    private static void unpackWarFile(File warFile, File targetDir) throws IOException {
        // Just treat the .war file as a zip file
        ZipUtils.unzipFileToDirectory(warFile, targetDir);
    }

    private static File createTempDirectory() throws IOException {
        File tempDir = File.createTempFile("eclipse-opsworks-deployment-content-dir-", null);

        if ( !tempDir.delete() ) {
            throw new RuntimeException("Could not delete temp file: "
                    + tempDir.getAbsolutePath());
        }

        if ( !tempDir.mkdirs() ) {
            throw new RuntimeException("Could not create temp directory: "
                    + tempDir.getAbsolutePath());
        }

        return tempDir;
    }

    /**
     * @return the deployment ID
     */
    public static String runDeployment(
            DeployProjectToOpsworksWizardDataModel dataModel,
            IProgressMonitor progressMonitor) {

        OpsWorksPlugin.getDefault().logInfo(
                "Preparing for deployment: " + dataModel.toString());

        /*
         * (1) Create a new OpsWorks app if necessary
         */
        App targetApp = null;

        if (dataModel.getIsCreatingNewJavaApp()) {

            progressMonitor.subTask(String.format(
                    "Create new Java app [%s]...",
                    dataModel.getNewJavaAppName()));

            OpsWorksPlugin.getDefault().logInfo(
                    "Making CreateApp API call...");
            String endpoint = dataModel.getRegion().getServiceEndpoints()
                    .get(ServiceAbbreviations.OPSWORKS);
            AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                    .getOpsWorksClientByEndpoint(endpoint);

            CreateAppRequest createAppRequest = new CreateAppRequest()
                    .withName(dataModel.getNewJavaAppName())
                    .withStackId(dataModel.getExistingStack().getStackId())
                    .withType(AppType.Java)
                    .withAppSource(dataModel.getS3ApplicationSource().toGenericSource())
                    .withEnableSsl(dataModel.isEnableSsl());
            if (dataModel.getCustomDomains() != null && !dataModel.getCustomDomains().isEmpty()) {
                createAppRequest.setDomains(dataModel.getCustomDomains());
            }
            if (dataModel.getEnvironmentVariables() != null && !dataModel.getEnvironmentVariables().isEmpty()) {
                List<EnvironmentVariable> vars = new LinkedList<>();
                for (com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel.EnvironmentVariable envVar : dataModel
                        .getEnvironmentVariables()) {
                    vars.add(envVar.toSdkModel());
                }
                createAppRequest.setEnvironment(vars);
            }
            if (dataModel.isEnableSsl()) {
                createAppRequest.setSslConfiguration(dataModel.getSslConfiguration().toSdkModel());
            }

            String newAppId = client.createApp(createAppRequest)
                    .getAppId();

            targetApp = client
                    .describeApps(
                            new DescribeAppsRequest().withAppIds(newAppId))
                    .getApps().get(0);

        } else {
            targetApp = dataModel.getExistingJavaApp();
        }

        progressMonitor.worked(30);

        /*
         * (2) Export application archive WAR file
         */

        final IProject project = dataModel.getProject();

        String uuid = UUID.randomUUID().toString();
        String tempDirName = targetApp.getShortname() + "-" + uuid + "-deployment-dir";

        File tempDir = new File(
                new File(System.getProperty("java.io.tmpdir")),
                tempDirName);
        File archiveContentDir = new File(
                tempDir, "archive-content");

        progressMonitor.subTask("Export project to WAR file...");

        OpsWorksPlugin.getDefault().logInfo(
                "Preparing to export project [" + project.getName() +
                "] to a WAR file [" + new File(archiveContentDir, "archive.war").getAbsolutePath() + "].");
        File warFile = WTPWarUtils.exportProjectToWar(
                project, new Path(archiveContentDir.getAbsolutePath()), "archive.war").toFile();
        OpsWorksPlugin.getDefault().logInfo(
                "WAR file created at [" + warFile.getAbsolutePath() + "]");

        progressMonitor.worked(10);

        progressMonitor.subTask("Repackage the war file...");

        /*
         * https://forums.aws.amazon.com/thread.jspa?messageID=557948&tstart=0
         */
        File zipArchive = null;
        try {
            zipArchive = repackWarFileToZipFile(warFile);
        } catch (IOException e) {
            OpsWorksPlugin.getDefault().reportException(
                    "Error when packaging the web application into zip archive.", e);
        }

        progressMonitor.worked(5);

        /*
         * (3) Upload to S3
         */
        String bucketName = dataModel.getS3ApplicationSource().getBucketName();
        String keyName = dataModel.getS3ApplicationSource().getKeyName();
        AmazonS3 s3Client = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucketName);

        progressMonitor.subTask("Upload ZIP file to S3...");

        OpsWorksPlugin.getDefault().logInfo(
                "Uploading zip file to S3 bucket [" + bucketName + "].");

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, keyName, zipArchive);
        if (dataModel.getS3ApplicationSource().isAsPublicHttpArchive()) {
            putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        }
        s3Client.putObject(putObjectRequest);

        OpsWorksPlugin.getDefault().logInfo(
                "Upload succeed. [s3://" + bucketName + "/" + keyName + "]");

        progressMonitor.worked(35);

        /*
         * (4) CreateDeployment
         */

        progressMonitor.subTask("Initiate deployment...");

        OpsWorksPlugin.getDefault().logInfo(
                "Making CreateDeployment API call...");
        String endpoint = dataModel.getRegion().getServiceEndpoints()
                .get(ServiceAbbreviations.OPSWORKS);
        AWSOpsWorks client = AwsToolkitCore.getClientFactory()
                .getOpsWorksClientByEndpoint(endpoint);

        CreateDeploymentRequest createDeploymentRequest = new CreateDeploymentRequest()
                .withStackId(dataModel.getExistingStack().getStackId())
                .withAppId(targetApp.getAppId())
                .withCommand(new DeploymentCommand().withName(DeploymentCommandName.Deploy));
        if (dataModel.getDeployComment() != null && !dataModel.getDeployComment().isEmpty()) {
            createDeploymentRequest.setComment(dataModel.getDeployComment());
        }
        if (dataModel.getCustomChefJson() != null && !dataModel.getCustomChefJson().isEmpty()) {
            createDeploymentRequest.setCustomJson(dataModel.getCustomChefJson());
        }
        CreateDeploymentResult result = client.createDeployment(createDeploymentRequest);

        OpsWorksPlugin.getDefault().logInfo(
                "Deployment submitted. Deployment ID [" + result.getDeploymentId() + "]");

        progressMonitor.worked(20);

        return result.getDeploymentId();
    }

}
