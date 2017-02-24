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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.core.model.ProjectNameDataModel;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.serverless.NameUtils;
import com.amazonaws.eclipse.lambda.serverless.Serverless;
import com.amazonaws.eclipse.lambda.serverless.blueprint.Blueprint;
import com.amazonaws.eclipse.lambda.serverless.blueprint.BlueprintProvider;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessFunction;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessInputTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessOutputTemplateData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Serverless project creation data model. It also manages all other models throughout this stage, such as
 * ServerlessModel, freemarker template models.
 */
public class NewServerlessProjectDataModel {

    /* These constants must be the same to the Pojo property names */
    public static final String P_PACKAGE_PREFIX = "packagePrefix";
    public static final String P_BLUEPRINT_NAME = "blueprintName";
    public static final String P_USE_BLUEPRINT = "useBlueprint";
    public static final String P_USE_SERVERLESS_TEMPLATE_FILE = "useServerlessTemplateFile";

    /* Function handler section */
    private final ProjectNameDataModel projectNameDataModel = new ProjectNameDataModel();
    private final MavenConfigurationDataModel mavenConfigurationDataModel = new MavenConfigurationDataModel();
    private final ImportFileDataModel importFileDataModel = new ImportFileDataModel();
    private String packagePrefix;
    private String blueprintName;
    private boolean useBlueprint = true;
    private boolean useServerlessTemplateFile = false;
    private boolean needLambdaProxyIntegrationModel = true;

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
    public String getPackagePrefix() {
        return packagePrefix;
    }
    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public String getBlueprintName() {
        return blueprintName;
    }
    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
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
    public boolean isNeedLambdaProxyIntegrationModel() {
        return needLambdaProxyIntegrationModel;
    }
    private void setNeedLambdaProxyIntegrationModel(
            boolean needLambdaProxyIntegrationModel) {
        this.needLambdaProxyIntegrationModel = needLambdaProxyIntegrationModel;
    }
    public ServerlessInputTemplateData getServerlessInputTemplateData()
            throws JsonParseException, JsonMappingException, IOException {
        buildServerlessModel();
        ServerlessInputTemplateData data = new ServerlessInputTemplateData();
        data.setPackageName(NameUtils.toModelPackageName(packagePrefix));
        data.setClassName(NameUtils.SERVERLESS_INPUT_CLASS_NAME);
        return data;
    }

    public ServerlessOutputTemplateData getServerlessOutputTemplateData()
            throws JsonParseException, JsonMappingException, IOException {
        buildServerlessModel();
        ServerlessOutputTemplateData data = new ServerlessOutputTemplateData();
        data.setPackageName(NameUtils.toModelPackageName(packagePrefix));
        data.setClassName(NameUtils.SERVERLESS_OUTPUT_CLASS_NAME);
        return data;
    }

    public List<ServerlessHandlerTemplateData> getServerlessHandlerTemplateData()
            throws JsonParseException, JsonMappingException, IOException {
        buildServerlessModel();
        List<ServerlessHandlerTemplateData> handlers = new ArrayList<ServerlessHandlerTemplateData>();
        for (ServerlessFunction function : serverlessModel.getServerlessFunctions().values()) {
            ServerlessHandlerTemplateData template = new ServerlessHandlerTemplateData();
            template.setPackageName(NameUtils.toHandlerPackageName(packagePrefix));
            template.setClassName(NameUtils.toHandlerClassName(function.getHandler()));
            template.setInputFqcn(NameUtils.toServerlessInputModelFqcn(packagePrefix));
            template.setOutputFqcn(NameUtils.toServerlessOutputModelFqcn(packagePrefix));
            handlers.add(template);
        }
        return handlers;
    }

    public void buildServerlessModel() throws JsonParseException, JsonMappingException, IOException {
        if (serverlessModel != null) return;
        if (isUseBlueprint()) {
            Blueprint blueprint = BlueprintProvider.getInstance().getBlueprint(
                    getBlueprintName());
            File serverlessFile = new File(CodeTemplateManager.getInstance()
                    .getCodeTemplateBasedir(), blueprint.getTemplatePath());
            serverlessModel = Serverless.load(serverlessFile);
            importFileDataModel.setFilePath(serverlessFile.getAbsolutePath());
            setNeedLambdaProxyIntegrationModel(blueprint.isNeedLambdaProxyIntegrationModel());
        } else {
            serverlessModel = Serverless.load(importFileDataModel.getFilePath());
        }
    }

    public boolean requireSdkDependency() {
        return true;
    }

}
