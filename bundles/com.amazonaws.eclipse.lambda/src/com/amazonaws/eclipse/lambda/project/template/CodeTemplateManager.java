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
package com.amazonaws.eclipse.lambda.project.template;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprint;
import com.amazonaws.eclipse.lambda.blueprint.ServerlessBlueprint;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Class that manages Freemarker code template and Json configuration files.
 * Use a get*Template method to get a certain Freemarker template.
 * Use a get*File method to get a certain File.
 */
public class CodeTemplateManager {

    private static final String CODE_TEMPLATE_BASE_DIR = "code-template";
    private static final String LAMBDA_BLUEPRINTS_BASE_DIR = "lambda";
    private static final String SERVERLESS_BLUEPRINTS_BASE_DIR = "serverless";

    private static final String LAMBDA_BLUEPRINTS_CONFIG_PATH = String.format("%s/%s", LAMBDA_BLUEPRINTS_BASE_DIR, "blueprints.json");
    private static final String SERVERLESS_BLUEPRINTS_CONFIG_PATH = String.format("%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, "blueprints.json");

    private static final String LAMBDA_BLUEPRINT_HANDLER_NAME = "handler.java.ftl";
    private static final String LAMBDA_BLUEPRINT_HANDLER_TEST_NAME = "handler-test.java.ftl";
    private static final String LAMBDA_BLUEPRINT_POM_NAME = "pom.xml.ftl";

    private static final String LAMBDA_README_FILE_PATH = String.format("%s/%s", LAMBDA_BLUEPRINTS_BASE_DIR, "README.ftl");
    private static final String SERVERLESS_README_FILE_PATH = String.format("%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, "README.html");
    private static final String SERVERLESS_INPUT_MODEL_PATH = String.format("%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, "serverless-input.ftl");
    private static final String SERVERLESS_OUTPUT_MODEL_PATH = String.format("%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, "serverless-output.ftl");
    private static final String SERVERLESS_HANDLER_CLASS_PATH = String.format("%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, "serverless-handler.ftl");

    public static final String SAM_FILE_NAME = "serverless.template";
    public static final String SERVERLESS_BLUEPRINT_SAM_NAME = "serverless.template.ftl";
    private static final String SERVERLESS_BLUEPRINT_POM_NAME = "pom.xml.ftl";

    private static final String TEST_RESOURCES_BASE_DIR = "test-resource";
    private static final String TEST_CLASSES_BASE_DIR = "test-class";

    private static final String TEST_CONTEXT_FILE_PATH = String.format("%s/%s", TEST_CLASSES_BASE_DIR, "test-context.ftl");
    private static final String TEST_UTILS_FILE_PATH = String.format("%s/%s", TEST_CLASSES_BASE_DIR, "test-utils.ftl");

    private final Configuration freemarkerCfg;

    private static final CodeTemplateManager INSTANCE = new CodeTemplateManager();

    private CodeTemplateManager() {
        freemarkerCfg = setupFreemarker();
    }

    public static CodeTemplateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Helper method to return the populated content rendered by Freemarker to the given
     * template and template data.
     */
    public static String processTemplateWithData(Template template, Object templateData)
            throws TemplateException, IOException {
        try (StringWriter stringWriter = new StringWriter()) {
            template.process(templateData, stringWriter);
            stringWriter.flush();
            return stringWriter.toString();
        }
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData}
     */
    public Template getLambdaHandlerTemplate(LambdaBlueprint blueprint) {
        return blueprint == null ? null : getTemplate(String.format("%s/%s/%s",
                LAMBDA_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(), LAMBDA_BLUEPRINT_HANDLER_NAME));
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData}
     */
    public Template getLambdaHandlerTestTemplate(LambdaBlueprint blueprint) {
        return blueprint == null ? null : getTemplate(String.format("%s/%s/%s",
                LAMBDA_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(), LAMBDA_BLUEPRINT_HANDLER_TEST_NAME));
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData}
     */
    public Template getLambdaBlueprintPomTemplate(LambdaBlueprint blueprint) {
        return blueprint == null ? null : getTemplate(String.format("%s/%s/%s",
                LAMBDA_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(), LAMBDA_BLUEPRINT_POM_NAME));
    }

    // Test JSON file could be null for some blueprints
    public File getLambdaTestJsonFile(LambdaBlueprint blueprint) {
        return blueprint == null || blueprint.getTestJsonFile() == null ? null :
            getFile(String.format("%s/%s", TEST_RESOURCES_BASE_DIR, blueprint.getTestJsonFile()));
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData}
     */
    public Template getTestContextTemplate() {
        return getTemplate(TEST_CONTEXT_FILE_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData}
     */
    public Template getTestUtilsTemplate() {
        return getTemplate(TEST_UTILS_FILE_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData}
     */
    public Template getlambdaProjectReadmeTemplate() {
        return getTemplate(LAMBDA_README_FILE_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData}
     */
    public Template getServerlessInputClassTemplate() {
        return getTemplate(SERVERLESS_INPUT_MODEL_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData}
     */
    public Template getServerlessOutputClassTemplate() {
        return getTemplate(SERVERLESS_OUTPUT_MODEL_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData}
     */
    public Template getServerlessHandlerClassTemplate() {
        return getTemplate(SERVERLESS_HANDLER_CLASS_PATH);
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData}
     */
    public Template getServerlessHandlerClassTemplate(ServerlessBlueprint blueprint, String handlerName) {
        String templatePath = blueprint.getHandlerTemplatePaths().get(handlerName);
        return getTemplate(String.format("%s/%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(), templatePath));
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.SamFileTemplateData}
     */
    public Template getServerlessSamTemplate(ServerlessBlueprint blueprint) {
        return getTemplate(String.format("%s/%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(),
                SERVERLESS_BLUEPRINT_SAM_NAME));
    }

    /**
     * Template data model {@link #com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData}
     */
    public Template getServerlessPomFile(ServerlessBlueprint blueprint) {
        return getTemplate(String.format("%s/%s/%s", SERVERLESS_BLUEPRINTS_BASE_DIR, blueprint.getBaseDir(), SERVERLESS_BLUEPRINT_POM_NAME));
    }

    public File getServerlessBlueprintsConfigFile() {
        return getFile(SERVERLESS_BLUEPRINTS_CONFIG_PATH);
    }

    public File getLambdaBlueprintsConfigFile() {
        return getFile(LAMBDA_BLUEPRINTS_CONFIG_PATH);
    }

    public File getServerlessReadmeFile() {
        return getFile(SERVERLESS_README_FILE_PATH);
    }

    private Configuration setupFreemarker() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_21);
        try {
            cfg.setDirectoryForTemplateLoading(getCodeTemplateBasedir());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to setup freemarker template directory.", e);
        }
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        return cfg;
    }

    private File getCodeTemplateBasedir() {
        Bundle bundle = LambdaPlugin.getDefault().getBundle();
        File file = null;
        try {
            URL bundleBaseUrl = FileLocator.toFileURL(bundle.getEntry(CODE_TEMPLATE_BASE_DIR));
            URI bundleBaseUri = new URI(bundleBaseUrl.getProtocol(), bundleBaseUrl.getPath(), null);
            file = new File(bundleBaseUri);
        } catch (IOException e) {
            throw new RuntimeException("Failed to find plugin bundle root.", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find plugin bundle root.", e);
        }
        return file;
    }

    private File getFile(String path) {
        return new File(getCodeTemplateBasedir(), path);
    }

    private Template getTemplate(String templatePath) {
        try {
            return freemarkerCfg.getTemplate(templatePath);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load Freemarker template from " + templatePath, e);
        }
    }
}
