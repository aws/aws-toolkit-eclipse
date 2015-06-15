package com.amazonaws.eclipse.lambda.project.template;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.lambda.LambdaPlugin;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class CodeTemplateManager {

    private static final String CODE_TEMPLATE_DIR_BASEDIR = "code-template";

    private final Configuration freemarkerCfg;

    private CodeTemplateManager() {
        freemarkerCfg = setupFreemarker();
    }

    public static String processTemplateWithData(Template template,
            Object templateData) throws TemplateException, IOException {
        StringWriter sw = new StringWriter();
        template.process(templateData, sw);
        sw.flush();
        return sw.toString();
    }

    /**
     * Handler class template
     */
    public Template getHandlerClassTemplate() {
        try {
            return freemarkerCfg.getTemplate("handler/handler-class-template.ftl");
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load handler class template.", e);
        }
    }

    /**
     * Handler test code template
     */
    public Template getHandlerTestClassTemplate() {
        try {
            return freemarkerCfg.getTemplate("test-class/handler-test-code-template.ftl");
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load handler test code template.", e);
        }
    }

    /**
     * {@code TestContext} class template.
     */
    public Template getTestContextTemplate() throws IOException {
        return freemarkerCfg.getTemplate("test-class/test-context.ftl");
    }

    /**
     * {@code TestUtils} class template.
     */
    public Template getTestUtilsTemplate() throws IOException {
        return freemarkerCfg.getTemplate("test-class/test-utils.ftl");
    }

    /**
     * Event input json files
     */
    public Template getTestInputJsonFileTemplate(String filename) {
        try {
            return freemarkerCfg.getTemplate("test-resource/" + filename);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load handler test code template.", e);
        }
    }

    public Template getReadmeHtmlFileTemplate() {
        try {
            return freemarkerCfg.getTemplate("README/README.ftl");
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load README.html template.", e);
        }
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
        URL bundleBaseUrl;
        try {
            bundleBaseUrl = FileLocator.resolve(bundle.getEntry("/"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to find plugin bundle root.", e);
        }
        return new File(bundleBaseUrl.getFile(), CODE_TEMPLATE_DIR_BASEDIR);
    }


    private static final CodeTemplateManager INSTANCE = new CodeTemplateManager();
    public static CodeTemplateManager getInstance() {
        return INSTANCE;
    }

}
