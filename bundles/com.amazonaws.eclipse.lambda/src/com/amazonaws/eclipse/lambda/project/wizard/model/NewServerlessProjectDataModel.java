/*
* Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.maven.MavenFactory;
import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.model.ProjectNameDataModel;
import com.amazonaws.eclipse.lambda.blueprint.BlueprintsProvider;
import com.amazonaws.eclipse.lambda.blueprint.ServerlessBlueprint;
import com.amazonaws.eclipse.lambda.blueprint.ServerlessBlueprintsConfig;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.SamFileTemplateData;
import com.amazonaws.eclipse.lambda.serverless.NameUtils;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import freemarker.template.TemplateException;

/**
 * Serverless project creation data model. It also manages all other models throughout this stage, such as
 * ServerlessModel, freemarker template models.
 */
public class NewServerlessProjectDataModel {

    /* These constants must be the same to the Pojo property names */
    public static final String P_BLUEPRINT_NAME = "blueprintName";
    public static final String P_USE_BLUEPRINT = "useBlueprint";
    public static final String P_USE_SERVERLESS_TEMPLATE_FILE = "useServerlessTemplateFile";

    private static final ServerlessBlueprintsConfig BLUEPRINTS_CONFIG = BlueprintsProvider.provideServerlessBlueprints();

    /* Function handler section */
    private final ProjectNameDataModel projectNameDataModel = new ProjectNameDataModel();
    private final MavenConfigurationDataModel mavenConfigurationDataModel = new MavenConfigurationDataModel();
    private final ImportFileDataModel importFileDataModel = new ImportFileDataModel();

    private ServerlessBlueprint selectedBlueprint = BLUEPRINTS_CONFIG.getBlueprints()
            .get(BLUEPRINTS_CONFIG.getDefaultBlueprint());
    private String blueprintName = selectedBlueprint.getDisplayName();
    private boolean useBlueprint = true;
    private boolean useServerlessTemplateFile = false;

    private ServerlessModel serverlessModel;

    public ProjectNameDataModel getProjectNameDataModel() {
        return projectNameDataModel;
    }
    public MavenConfigurationDataModel getMavenConfigurationDataModel() {
        return mavenConfigurationDataModel;
    }
    public ImportFileDataModel getImportFileDataModel() {
        return importFileDataModel;
    }

    public String getBlueprintName() {
        return blueprintName;
    }
    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
        this.selectedBlueprint = getServerlessBlueprint(blueprintName);
    }

    public ServerlessBlueprint getSelectedBlueprint() {
        return selectedBlueprint;
    }

    public boolean isUseBlueprint() {
        return useBlueprint;
    }
    public void setUseBlueprint(boolean useBlueprint) {
        this.useBlueprint = useBlueprint;
    }
    public boolean isUseServerlessTemplateFile() {
        return useServerlessTemplateFile;
    }
    public void setUseServerlessTemplateFile(boolean useServerlessTemplateFile) {
        this.useServerlessTemplateFile = useServerlessTemplateFile;
    }
    public ServerlessDataModelTemplateData getServerlessDataModelTemplateData()
            throws JsonParseException, JsonMappingException, IOException, TemplateException {
        buildServerlessModel();
        ServerlessDataModelTemplateData data = new ServerlessDataModelTemplateData();
        data.setPackageName(NameUtils.toModelPackageName(mavenConfigurationDataModel.getPackageName()));
        data.setServerlessInputClassName(NameUtils.SERVERLESS_INPUT_CLASS_NAME);
        data.setServerlessOutputClassName(NameUtils.SERVERLESS_OUTPUT_CLASS_NAME);
        return data;
    }

    public List<ServerlessHandlerTemplateData> getServerlessHandlerTemplateData()
            throws JsonParseException, JsonMappingException, IOException, TemplateException {
        buildServerlessModel();
        List<ServerlessHandlerTemplateData> handlers = new ArrayList<>();
        for (ServerlessFunction function : serverlessModel.getServerlessFunctions().values()) {
            ServerlessHandlerTemplateData template = new ServerlessHandlerTemplateData();
            String handlerName = function.getHandler();
            int lastDotIndex = handlerName.lastIndexOf(".");
            if (lastDotIndex < 0) {
                template.setPackageName(NameUtils.toHandlerPackageName(mavenConfigurationDataModel.getPackageName()));
            } else {
                template.setPackageName(handlerName.substring(0, lastDotIndex));
            }
            template.setClassName(NameUtils.toHandlerClassName(handlerName));
            template.setInputFqcn(NameUtils.toServerlessInputModelFqcn(mavenConfigurationDataModel.getPackageName()));
            template.setOutputFqcn(NameUtils.toServerlessOutputModelFqcn(mavenConfigurationDataModel.getPackageName()));
            handlers.add(template);
        }
        return handlers;
    }

    public PomFileTemplateData getServerlessPomTemplateData() {
        PomFileTemplateData data = new PomFileTemplateData();
        data.setGroupId(mavenConfigurationDataModel.getGroupId());
        data.setArtifactId(mavenConfigurationDataModel.getArtifactId());
        data.setVersion(mavenConfigurationDataModel.getVersion());
        data.setAwsJavaSdkVersion(MavenFactory.getLatestJavaSdkVersion());
        return data;
    }

    public SamFileTemplateData getServerlessSamTemplateData() {
        SamFileTemplateData data = new SamFileTemplateData();
        data.setPackageName(NameUtils.toHandlerPackageName(mavenConfigurationDataModel.getPackageName()));
        data.setArtifactId(mavenConfigurationDataModel.getArtifactId());
        data.setVersion(mavenConfigurationDataModel.getVersion());
        return data;
    }

    private void buildServerlessModel() throws JsonParseException, JsonMappingException, IOException, TemplateException {
        if (serverlessModel != null) return;
        if (isUseBlueprint()) {
            ServerlessBlueprint blueprint = getSelectedBlueprint();
            String content = CodeTemplateManager.processTemplateWithData(
                    CodeTemplateManager.getInstance().getServerlessSamTemplate(blueprint), getServerlessSamTemplateData());
            serverlessModel = Serverless.loadFromContent(content);
        } else {
            serverlessModel = Serverless.load(importFileDataModel.getFilePath());
        }
    }

    /**
     * Return the ServerlessBlueprint by display name.
     */
    private static ServerlessBlueprint getServerlessBlueprint(String displayName) {
        for (ServerlessBlueprint blueprint : BLUEPRINTS_CONFIG.getBlueprints().values()) {
            if (blueprint.getDisplayName().equals(displayName)) {
                return blueprint;
            }
        }
        return null;
    }
}
