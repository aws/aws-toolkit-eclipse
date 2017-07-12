package com.amazonaws.eclipse.codedeploy.deploy.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.model.DeployProjectToCodeDeployWizardDataModel;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.model.BundleType;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.RevisionLocationType;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.amazonaws.services.s3.AmazonS3;

public class DeployUtils {


    /**
     * @return the deployment ID
     */
    public static String createDeployment(
            DeployProjectToCodeDeployWizardDataModel dataModel,
            IProgressMonitor progressMonitor) {

        CodeDeployPlugin.getDefault().logInfo(
                "Preparing for deployment: " + dataModel.toString());

        /*
         * (1) Export application archive WAR file
         */

        final IProject project = dataModel.getProject();

        String uuid = UUID.randomUUID().toString();
        String tempDirName = dataModel.getApplicationName() + "-" + uuid + "-deployment-dir";
        String zipFileName = dataModel.getApplicationName() + "-" + uuid + ".zip";

        File tempDir = new File(
                new File(System.getProperty("java.io.tmpdir")),
                tempDirName);
        File archiveContentDir = new File(
                tempDir, "archive-content");

        progressMonitor.subTask("Export project to WAR file...");

        String warFileRelativePath = dataModel.getTemplateModel()
                .getWarFileExportLocationWithinDeploymentArchive();

        CodeDeployPlugin.getDefault().logInfo(
                "Preparing to export project [" + project.getName() +
                "] to a WAR file [" + new File(archiveContentDir, warFileRelativePath).getAbsolutePath() + "].");
        File warFile = WTPWarUtils.exportProjectToWar(
                project, new Path(archiveContentDir.getAbsolutePath()), warFileRelativePath).toFile();
        CodeDeployPlugin.getDefault().logInfo(
                "WAR file created at [" + warFile.getAbsolutePath() + "]");

        progressMonitor.worked(10);

        progressMonitor.subTask("Add app-spec file and all the deployment event hooks...");

        try {
            addAppSpecFileAndEventHooks(archiveContentDir, dataModel);
        } catch (IOException e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Error when adding app-spec fild and deployment event hooks.", e);
        }

        progressMonitor.worked(5);

        progressMonitor.subTask("Create the ZIP file including all the deployment artifacts...");

        File zipArchive = new File(tempDir, zipFileName);
        CodeDeployPlugin.getDefault().logInfo(
                "Preparing to bundle project artifacts into a zip file [" +
                zipArchive.getAbsolutePath() + "].");
        try {
            ZipUtils.createZipFileOfDirectory(archiveContentDir, zipArchive);
        } catch (IOException e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Error when creating zip archive file for the deployment.", e);
        }
        CodeDeployPlugin.getDefault().logInfo(
                "Zip file created at [" +
                zipArchive.getAbsolutePath() + "].");

        progressMonitor.worked(15);

        /*
         * (2) Upload to S3
         */
        String bucketName = dataModel.getBucketName();
        String keyName = zipArchive.getName();
        AmazonS3 s3Client = AwsToolkitCore.getClientFactory().getS3ClientForBucket(bucketName);

        progressMonitor.subTask("Upload ZIP file to S3...");

        CodeDeployPlugin.getDefault().logInfo(
                "Uploading zip file to S3 bucket [" + bucketName + "].");

        s3Client.putObject(bucketName, keyName, zipArchive);
        CodeDeployPlugin.getDefault().logInfo(
                "Upload succeed. [s3://" + bucketName + "/" + keyName + "]");

        progressMonitor.worked(30);

        /*
         * (3) CreateDeployment
         */

        progressMonitor.subTask("Initiate deployment...");

        CodeDeployPlugin.getDefault().logInfo(
                "Making CreateDeployment API call...");
        String endpoint = dataModel.getRegion().getServiceEndpoints()
                .get(ServiceAbbreviations.CODE_DEPLOY);
        AmazonCodeDeploy client = AwsToolkitCore.getClientFactory()
                .getCodeDeployClientByEndpoint(endpoint);

        CreateDeploymentResult result = client.createDeployment(new CreateDeploymentRequest()
                .withApplicationName(dataModel.getApplicationName())
                .withDeploymentGroupName(dataModel.getDeploymentGroupName())
                .withDeploymentConfigName(dataModel.getDeploymentConfigName())
                .withIgnoreApplicationStopFailures(dataModel.isIgnoreApplicationStopFailures())
                .withRevision(new RevisionLocation()
                      .withRevisionType(RevisionLocationType.S3)
                      .withS3Location(new S3Location()
                              .withBucket(bucketName)
                              .withKey(keyName)
                              .withBundleType(BundleType.Zip)
                      ))
                .withDescription("Deployment created from AWS Eclipse plugin")
                );

        CodeDeployPlugin.getDefault().logInfo(
                "Deployment submitted. Deployment ID [" + result.getDeploymentId() + "]");

        progressMonitor.worked(10);

        return result.getDeploymentId();
    }

    private static void addAppSpecFileAndEventHooks(File targetBaseDir,
            final DeployProjectToCodeDeployWizardDataModel deployDataModel) throws IOException {

        File templateCopySourceRoot = deployDataModel.getTemplateModel()
                .getResolvedTemplateBasedir();

        copyDirectoryWithTransformationHandler(templateCopySourceRoot, targetBaseDir, new FileTransformationHandler() {

            @Override
            public void copyFromFileToFile(File src, File target) throws IOException {
                String srcContent = FileUtils.readFileToString(src);
                String transformedContent = substituteUserConfiguration(
                        srcContent, deployDataModel.getTemplateParameterValues());

                FileUtils.writeStringToFile(target, transformedContent);
            }
        });

    }

    private static String substituteUserConfiguration(String originalContent,
            Map<String, String> paramAnchorTextAndValues) {

        for (Entry<String, String> entry : paramAnchorTextAndValues.entrySet()) {
            String anchorText = entry.getKey();
            String value = entry.getValue();

            originalContent = originalContent.replace(anchorText, value);
        }

        return originalContent;
    }

    private static void copyDirectoryWithTransformationHandler(File srcDir, File destDir, FileTransformationHandler handler) throws IOException {
        if (destDir.exists()) {
            if (destDir.isDirectory() == false) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (destDir.mkdirs() == false) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (destDir.canWrite() == false) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        // recurse
        File[] files = srcDir.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        for (int i = 0; i < files.length; i++) {
            File copiedFile = new File(destDir, files[i].getName());
            if (files[i].isDirectory()) {
                copyDirectoryWithTransformationHandler(files[i], copiedFile, handler);
            } else {
                handler.copyFromFileToFile(files[i], copiedFile);
            }
        }
    }

    private interface FileTransformationHandler {
        void copyFromFileToFile(File src, File target) throws IOException;
    }

}
