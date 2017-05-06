package com.amazonaws.eclipse.lambda.blueprint;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.PomFileTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public class BlueprintsTest {

    private CodeTemplateManager manager = CodeTemplateManager.getInstance();
    private LambdaBlueprintTemplateData lambdaBlueprintTemplateData;
    private ServerlessDataModelTemplateData serverlessDataModelTemplateData;
    private ServerlessHandlerTemplateData serverlessHandlerTemplateData;
    private PomFileTemplateData pomFileTemplateData;

    @Before
    public void setUp() {
        lambdaBlueprintTemplateData = mockLambdaBlueprintTemplateData();
        serverlessDataModelTemplateData = mockServerlessDataModelTemplateData();
        serverlessHandlerTemplateData = mockServerlessHandlerTemplateData();
        pomFileTemplateData = mockPomFileTemplateData();
    }

    // Assert all the needed files exist and the templates are valid with the mocked data.
    @Test
    public void testBlueprintManager() {
        assertFilesExist(
            manager.getLambdaBlueprintsConfigFile(),
            manager.getServerlessBlueprintsConfigFile(),
            manager.getServerlessReadmeFile()
        );

        assertTemplatesValid(lambdaBlueprintTemplateData,
            manager.getlambdaProjectReadmeTemplate(),
            manager.getTestContextTemplate(),
            manager.getTestUtilsTemplate()
        );

        assertTemplatesValid(serverlessDataModelTemplateData,
            manager.getServerlessInputClassTemplate(),
            manager.getServerlessOutputClassTemplate()
        );

        assertTemplatesValid(serverlessHandlerTemplateData,
            manager.getServerlessHandlerClassTemplate()
        );
    }

    @Test
    public void testLambdaBlueprints() {
        LambdaBlueprintsConfig config = BlueprintsProvider.provideLambdaBlueprints();
        assertNotNull(config.getDefaultBlueprint());

        Map<String, LambdaBlueprint> blueprints = config.getBlueprints();
        assertNotNull(blueprints);
        // Assert the default blueprint is included in the Blueprint map.
        assertNotNull(blueprints.get(config.getDefaultBlueprint()));

        for (Entry<String, LambdaBlueprint> entry : blueprints.entrySet()) {
            LambdaBlueprint blueprint = entry.getValue();
            assertNotNull(blueprint.getDisplayName());
            assertTemplatesValid(lambdaBlueprintTemplateData,
                manager.getLambdaHandlerTemplate(blueprint),
                manager.getLambdaHandlerTestTemplate(blueprint));
            assertTemplatesValid(pomFileTemplateData,
                manager.getLambdaBlueprintPomTemplate(blueprint));
            if (blueprint.getTestJsonFile() != null) {
                assertFileExists(manager.getLambdaTestJsonFile(blueprint));
            }
        }
    }

    @Test
    public void testServerlessBlueprints() {
        ServerlessBlueprintsConfig config = BlueprintsProvider.provideServerlessBlueprints();
        assertNotNull(config.getDefaultBlueprint());

        Map<String, ServerlessBlueprint> blueprints = config.getBlueprints();
        assertNotNull(blueprints);
        assertNotNull(blueprints.get(config.getDefaultBlueprint()));

        for (Entry<String, ServerlessBlueprint> entry : blueprints.entrySet()) {
            ServerlessBlueprint blueprint = entry.getValue();
            assertNotNull(blueprint.getDisplayName());
            assertFilesExist(manager.getServerlessSamFile(blueprint));
            assertTemplatesValid(pomFileTemplateData, manager.getServerlessPomFile(blueprint));

            Map<String, String> handlers = blueprint.getHandlerTemplatePaths();
            assertNotNull(handlers);
            for (String handlerName : handlers.keySet()) {
                assertTemplatesValid(serverlessHandlerTemplateData,
                    manager.getServerlessHandlerClassTemplate(blueprint, handlerName));
            }
        }
    }

    private LambdaBlueprintTemplateData mockLambdaBlueprintTemplateData() {
        LambdaBlueprintTemplateData data = new LambdaBlueprintTemplateData();
        data.setPackageName("com.foo");
        data.setHandlerClassName("Foo");
        data.setHandlerTestClassName("FooTest");
        data.setInputJsonFileName("foo.json");
        return data;
    }

    private ServerlessDataModelTemplateData mockServerlessDataModelTemplateData() {
        ServerlessDataModelTemplateData serverlessDataModelTemplateData = new ServerlessDataModelTemplateData();
        serverlessDataModelTemplateData.setPackageName("com.foo");
        serverlessDataModelTemplateData.setServerlessInputClassName("ServerlessInput");
        serverlessDataModelTemplateData.setServerlessOutputClassName("ServerlessOutput");
        return serverlessDataModelTemplateData;
    }

    private ServerlessHandlerTemplateData mockServerlessHandlerTemplateData() {
        ServerlessHandlerTemplateData serverlessHandlerTemplateData = new ServerlessHandlerTemplateData();
        serverlessHandlerTemplateData.setPackageName("com.foo");
        serverlessHandlerTemplateData.setClassName("FooHandler");
        serverlessHandlerTemplateData.setInputFqcn("com.foo.Input");
        serverlessHandlerTemplateData.setOutputFqcn("com.foo.Output");
        return serverlessHandlerTemplateData;
    }

    private PomFileTemplateData mockPomFileTemplateData() {
        PomFileTemplateData pomFileTemplateData = new PomFileTemplateData();
        pomFileTemplateData.setGroupId("com.foo");
        pomFileTemplateData.setArtifactId("bar");
        pomFileTemplateData.setVersion("1.0.0");
        pomFileTemplateData.setAwsJavaSdkVersion("1.11.111");
        return pomFileTemplateData;
    }

    private void assertTemplatesValid(Object dataModel, Template... templates) {
        for (Template template : templates) {
            try {
                String content = CodeTemplateManager.processTemplateWithData(template, dataModel);
                assertStringNotEmpty(content);
            } catch (TemplateException | IOException e) {
                fail(template.getName());
            }
        }
    }

    private void assertFilesExist(File... files) {
        for (File file : files) {
            assertFileExists(file);
        }
    }

    private void assertFileExists(File file) {
        assertNotNull(file);
        assertTrue(file.exists());
    }

    private void assertStringNotEmpty(String value) {
        assertNotNull(value);
        assertTrue(value.trim().length() > 0);
    }
}
