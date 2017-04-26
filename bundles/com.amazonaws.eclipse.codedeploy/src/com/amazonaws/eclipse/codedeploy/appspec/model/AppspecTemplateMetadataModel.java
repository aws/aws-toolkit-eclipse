package com.amazonaws.eclipse.codedeploy.appspec.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.appspec.util.JacksonUtils;
import com.amazonaws.protocol.json.SdkJsonGenerator.JsonGenerationException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class AppspecTemplateMetadataModel {

    private final String metadataVersion;
    private final String templateName;
    private final String templateDescription;
    private final String templateBasedir;
    // Use boxed Boolean so that we can differentiate a non-specified boolean value
    private final Boolean isCustomTemplate;

    private final String warFileExportLocationWithinDeploymentArchive;
    private final boolean useDefaultHttpPortParameter;
    private final boolean useDefaultContextPathParameter;

    private final List<AppspecTemplateParameter> parameters;

    @JsonCreator
    public AppspecTemplateMetadataModel(
            @JsonProperty("metadataVersion") String metadataVersion,
            @JsonProperty("templateName") String templateName,
            @JsonProperty("templateDescription") String templateDescription,
            @JsonProperty("templateBasedir") String templateBasedir,
            @JsonProperty("isCustomTemplate") Boolean isCustomTemplate,
            @JsonProperty("warFileExportLocationWithinDeploymentArchive") String warFileExportLocationWithinDeploymentArchive,
            @JsonProperty("useDefaultHttpPortParameter") boolean useDefaultHttpPortParameter,
            @JsonProperty("useDefaultContextPathParameter") boolean useDefaultContextPathParameter,
            @JsonProperty("parameters") List<AppspecTemplateParameter> parameters) {
        this.metadataVersion = metadataVersion;
        this.templateName = templateName;
        this.templateDescription = templateDescription;
        this.templateBasedir = templateBasedir;
        this.isCustomTemplate = isCustomTemplate;
        this.warFileExportLocationWithinDeploymentArchive = warFileExportLocationWithinDeploymentArchive;
        this.useDefaultHttpPortParameter = useDefaultHttpPortParameter;
        this.useDefaultContextPathParameter = useDefaultContextPathParameter;
        this.parameters = parameters;
    }

    public String getMetadataVersion() {
        return metadataVersion;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateDescription() {
        return templateDescription;
    }

    public String getTemplateBasedir() {
        return templateBasedir;
    }

    @JsonIgnore
    public File getResolvedTemplateBasedir() {
        if (isCustomTemplate) {
            return new File(templateBasedir);

        } else {
            Bundle bundle = CodeDeployPlugin.getDefault().getBundle();
            URL bundleBaseUrl;
            try {
                bundleBaseUrl = FileLocator.resolve(bundle.getEntry("/"));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to resolve root entry in the CodeDeploy plugin bundle", e);
            }

            // Relative to the bundle root
            return new File(bundleBaseUrl.getFile(), templateBasedir);
        }
    }

    public boolean getIsCustomTemplate() {
        return isCustomTemplate;
    }

    public String getWarFileExportLocationWithinDeploymentArchive() {
        return warFileExportLocationWithinDeploymentArchive;
    }

    public boolean isUseDefaultHttpPortParameter() {
        return useDefaultHttpPortParameter;
    }

    public boolean isUseDefaultContextPathParameter() {
        return useDefaultContextPathParameter;
    }

    public List<AppspecTemplateParameter> getParameters() {
        return parameters;
    }

    /*
     * Utility functions for loading or outputing the metadata model.
     */

    public static AppspecTemplateMetadataModel loadFromFile(File metadataFile, boolean validateAfterLoaded) {
        if ( !metadataFile.exists() ) {
            throw new RuntimeException(String.format(
                    "The template metadata file [%s] doesn't exist.", metadataFile.getAbsolutePath()));
        }
        if ( !metadataFile.isFile() ) {
            throw new RuntimeException(String.format(
                    "The specified location [%s] is not a file.", metadataFile.getAbsolutePath()));
        }

        try {
            AppspecTemplateMetadataModel model = JacksonUtils.MAPPER.readValue(metadataFile, AppspecTemplateMetadataModel.class);
            if (validateAfterLoaded) {
                model.validate();
            }
            return model;
        } catch (JsonParseException e) {
            throw new RuntimeException(
                    "Failed to parse metadata file at " + metadataFile.getAbsolutePath(), e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(
                    "Failed to parse metadata file at " + metadataFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read metadata file at " + metadataFile.getAbsolutePath(), e);
        }
    }

    public void writeToFile(File destination) {
        try {
            JacksonUtils.MAPPER.writeValue(destination, this);
        } catch (JsonGenerationException e) {
            throw new RuntimeException(
                    "Failed to serialize metadata model into JSON string.", e);
        } catch (JsonMappingException e) {
            throw new RuntimeException(
                    "Failed to serialize metadata model into JSON string.", e);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write metadata to file " + destination.getAbsolutePath(), e);
        }
    }

    /**
     * @throw IllegalArgumentException if the template model is not valid
     */
    @JsonIgnore
    public void validate() {
        checkRequiredFields();
        checkVersion();
        checkTemplateBaseDir();
        checkTemplateParameters();
    }

    private void checkRequiredFields() {
        if (metadataVersion == null)
            throw new IllegalArgumentException("metadataVersion is not specified.");
        if (templateName == null)
            throw new IllegalArgumentException("templateName is not specified.");
        if (templateDescription == null)
            throw new IllegalArgumentException("templateDescription is not specified.");
        if (templateBasedir == null)
            throw new IllegalArgumentException("templateBasedir is not specified.");
        if (isCustomTemplate == null)
            throw new IllegalArgumentException("isCustomTemplate is not specified.");
        if (warFileExportLocationWithinDeploymentArchive == null)
            throw new IllegalArgumentException("warFileExportLocationWithinDeploymentArchive is not specified.");
    }

    private void checkVersion() {
        if ( !"1.0".equals(metadataVersion) )
            throw new IllegalArgumentException("Unsupported metadata version: " + metadataVersion);
    }

    private void checkTemplateBaseDir() {
        checkTemplateBaseDir(getResolvedTemplateBasedir());
    }

    private void checkTemplateBaseDir(File basedir) {
        if ( !basedir.exists() ) {
            throw new IllegalArgumentException(String.format(
                    "The template base directory[%s] doesn't exist.", basedir.getAbsolutePath()));
        }
        if ( !basedir.isDirectory() ) {
            throw new IllegalArgumentException(String.format(
                    "The specified location[%s] is not a directory.", basedir.getAbsolutePath()));
        }

        File appspecFile = new File(basedir, "appspec.yml");
        if ( !appspecFile.exists() || !appspecFile.isFile() ) {
            throw new IllegalArgumentException(String.format(
                    "The appspec file doesn't exist at the specified location[%s].", appspecFile.getAbsolutePath()));
        }
    }

    private void checkTemplateParameters() {
        if (parameters != null) {
            for (AppspecTemplateParameter parameter : parameters) {
                parameter.validate();
            }
        }
    }

    @Override
    public String toString() {
        try {
            return JacksonUtils.MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to serialize metadata model into JSON string.", e);
        }
    }

}
