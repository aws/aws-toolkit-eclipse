package com.amazonaws.eclipse.opsworks.deploy.wizard.model;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Stack;

public class DeployProjectToOpsworksWizardDataModel {

    public static final String IS_CREATING_NEW_JAVA_APP = "isCreatingNewJavaApp";
    public static final String NEW_JAVA_APP_NAME = "newJavaAppName";
    public static final String ENABLE_SSL = "enableSsl";
    public static final String DEPLOY_COMMENT = "deployComment";
    public static final String CUSTOM_CHEF_JSON = "customChefJson";

    private final IProject project;

    /* Page 1*/
    private Region region;

    private Stack existingStack;

    private boolean isCreatingNewJavaApp;
    private App existingJavaApp;
    private String newJavaAppName;

    /* Page 2*/
    private S3ApplicationSource s3ApplicationSource = new S3ApplicationSource();
    private List<EnvironmentVariable> environmentVariables = new LinkedList<>();
    private List<String> customDomains = new LinkedList<>();

    private boolean enableSsl;
    private SslConfiguration sslConfiguration = new SslConfiguration();

    /* Page 3*/
    private String deployComment;
    private String customChefJson;

    /**
     * @param project
     *            The Eclipse local project that is to be deployed.
     */
    public DeployProjectToOpsworksWizardDataModel(IProject project) {
        this.project = project;
    }

    public IProject getProject() {
        return project;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Stack getExistingStack() {
        return existingStack;
    }

    public void setExistingStack(Stack existingStack) {
        this.existingStack = existingStack;
    }

    public boolean getIsCreatingNewJavaApp() {
        return isCreatingNewJavaApp;
    }

    public void setIsCreatingNewJavaApp(boolean isCreatingNewJavaApp) {
        this.isCreatingNewJavaApp = isCreatingNewJavaApp;
    }

    public App getExistingJavaApp() {
        return existingJavaApp;
    }

    public void setExistingJavaApp(App existingJavaApp) {
        this.existingJavaApp = existingJavaApp;
    }

    public String getNewJavaAppName() {
        return newJavaAppName;
    }

    public void setNewJavaAppName(String newJavaAppName) {
        this.newJavaAppName = newJavaAppName;
    }

    public S3ApplicationSource getS3ApplicationSource() {
        return s3ApplicationSource;
    }

    public void setS3ApplicationSource(S3ApplicationSource s3ApplicationSource) {
        this.s3ApplicationSource = s3ApplicationSource;
    }

    public List<EnvironmentVariable> getEnvironmentVariables() {
        return Collections.unmodifiableList(environmentVariables);
    }

    public void addEnvironmentVariable(EnvironmentVariable var) {
        environmentVariables.add(var);
    }

    public void clearEnvironmentVariable() {
        environmentVariables.clear();
    }


    public List<String> getCustomDomains() {
        return Collections.unmodifiableList(customDomains);
    }

    public void addCustomDomain(String domain) {
        customDomains.add(domain);
    }

    public void clearCustomDomains() {
        customDomains.clear();
    }

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public SslConfiguration getSslConfiguration() {
        return sslConfiguration;
    }

    public void setSslConfiguration(SslConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    public String getDeployComment() {
        return deployComment;
    }

    public void setDeployComment(String deployComment) {
        this.deployComment = deployComment;
    }

    public String getCustomChefJson() {
        return customChefJson;
    }

    public void setCustomChefJson(String customChefJson) {
        this.customChefJson = customChefJson;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
          .append("eclipse-project=" + project.getName())
          .append(", region=" + region)
          .append(", stack=" + existingStack.getName())
          .append(", isCreatingNewJavaApp=" + isCreatingNewJavaApp)
          .append(", existingJavaApp=" + (existingJavaApp == null ? null : existingJavaApp.getName()))
          .append(", newJavaAppName=" + newJavaAppName)
          .append(", s3ApplicationSource=" + s3ApplicationSource)
          .append(", environmentVariables=" + environmentVariables)
          .append(", customDomains=" + customDomains)
          .append(", enableSsl=" + enableSsl)
          .append(", sslConfiguration=" + sslConfiguration)
          .append(", deployComment=" + deployComment)
          .append(", customChefJson=" + customChefJson)
          .append("}")
        ;
        return sb.toString();
    }

    public static class EnvironmentVariable {

        public static final String KEY = "key";
        public static final String VALUE = "value";
        public static final String SECURE = "secure";

        private String key;
        private String value;
        private boolean secure;

        public String getKey() {
            return key;
        }
        public void setKey(String key) {
            this.key = key;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
        public boolean isSecure() {
            return secure;
        }
        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public com.amazonaws.services.opsworks.model.EnvironmentVariable toSdkModel() {
            return new com.amazonaws.services.opsworks.model.EnvironmentVariable()
                    .withKey(key)
                    .withValue(value)
                    .withSecure(secure);
        }

        @Override
        public String toString() {
            return String.format("{ key=%s, value=%s, secure=%s }",
                    key, value, secure);
        }

    }

    public static class SslConfiguration {

        public static final String CERTIFICATE = "certificate";
        public static final String CHAIN = "chain";
        public static final String PRIVATE_KEY = "privateKey";

        private String certificate;
        private String chain;
        private String privateKey;

        public String getCertificate() {
            return certificate;
        }
        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }
        public String getChain() {
            return chain;
        }
        public void setChain(String chain) {
            this.chain = chain;
        }
        public String getPrivateKey() {
            return privateKey;
        }
        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public com.amazonaws.services.opsworks.model.SslConfiguration toSdkModel() {
            return new com.amazonaws.services.opsworks.model.SslConfiguration()
                    .withCertificate(certificate)
                    .withChain(chain)
                    .withPrivateKey(privateKey);
        }

        @Override
        public String toString() {
            return String.format("{ certificate=%s, chain=%s, privateKey=%s }",
                    certificate, chain, privateKey);
        }
    }

}
