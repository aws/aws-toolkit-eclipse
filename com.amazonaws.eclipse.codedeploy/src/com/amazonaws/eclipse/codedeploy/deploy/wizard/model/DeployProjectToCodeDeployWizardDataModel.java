package com.amazonaws.eclipse.codedeploy.deploy.wizard.model;

import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateMetadataModel;
import com.amazonaws.eclipse.core.regions.Region;

public class DeployProjectToCodeDeployWizardDataModel {

    public static final String REGION_PROPERTY = "region";
    public static final String APPLICATION_NAME_PROPERTY = "applicationName";
    public static final String DEPLOYMENT_GROUP_NAME_PROPERTY = "deploymentGroupName";
    public static final String DEPLOYMENT_CONFIG_NAME_PROPERTY = "deploymentConfigName";
    public static final String IGNORE_APPLICATION_STOP_FAILURES_PROPERTY = "ignoreApplicationStopFailures";
    public static final String BUCKET_NAME_PROPERTY = "bucketName";

    private final IProject project;

    /* Page 1 */
    private Region region;
    private String applicationName;
    private String deploymentGroupName;

    /* Page 2 */
    private String deploymentConfigName;
    private boolean ignoreApplicationStopFailures;
    private String bucketName;

    /* Page 3 */
    private AppspecTemplateMetadataModel templateModel;
    private Map<String, String> templateParameterValues;

    /**
     * @param project
     *            The Eclipse local project that is to be deployed.
     */
    public DeployProjectToCodeDeployWizardDataModel(IProject project) {
        this.project = project;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDeploymentGroupName() {
        return deploymentGroupName;
    }

    public void setDeploymentGroupName(String deploymentGroupName) {
        this.deploymentGroupName = deploymentGroupName;
    }

    public String getDeploymentConfigName() {
        return deploymentConfigName;
    }

    public void setDeploymentConfigName(String deploymentConfigName) {
        this.deploymentConfigName = deploymentConfigName;
    }

    public boolean isIgnoreApplicationStopFailures() {
        return ignoreApplicationStopFailures;
    }

    public void setIgnoreApplicationStopFailures(
            boolean ignoreApplicationStopFailures) {
        this.ignoreApplicationStopFailures = ignoreApplicationStopFailures;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public IProject getProject() {
        return project;
    }

    public AppspecTemplateMetadataModel getTemplateModel() {
        return templateModel;
    }

    public void setTemplateModel(AppspecTemplateMetadataModel templateModel) {
        this.templateModel = templateModel;
    }

    public Map<String, String> getTemplateParameterValues() {
        return templateParameterValues;
    }

    public void setTemplateParameterValues(
            Map<String, String> templateParameterValues) {
        this.templateParameterValues = templateParameterValues;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("Eclipse.Project=" + project.getName())
          .append(", Region=" + region)
          .append(", Bucket=" + bucketName)
          .append(", Application=" + applicationName)
          .append(", DeploymentGroup=" + deploymentGroupName)
          .append(", DeploymentConfig=" + deploymentConfigName)
          .append(", IgnoreApplicationStopFailures=" + ignoreApplicationStopFailures)
          .append(", AppspecTemplateName=" + templateModel.getTemplateName())
          .append(", AppspecTemplateParams=" + templateParameterValues)
          .append("}")
          ;
        return sb.toString();
    }
}
