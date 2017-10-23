/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.amazonaws.eclipse.core.model.MavenConfigurationDataModel;
import com.amazonaws.eclipse.lambda.project.template.data.SamFileTemplateData;
import com.amazonaws.eclipse.lambda.project.wizard.model.NewServerlessProjectDataModel;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessDataModelTemplateData;
import com.amazonaws.eclipse.lambda.serverless.template.ServerlessHandlerTemplateData;

import freemarker.template.TemplateException;

/**
 * Test class for {@link #NewServerlessProjectDataModel} to correctly return the template data.
 */
public class NewServerlessProjectDataModelTest {
    private static final String FAKE_GROUP_ID = "com.foo";
    private static final String FAKE_ARTIFACT_ID = "bar";
    private static final String FAKE_VERSION = "1.1.1";
    private static final String FAKE_PACKAGE_NAME = "com.foo.baz";

    @Test
    public void testServerlessProjectDataModel_withBlueprint() {
        ServerlessBlueprintsConfig config = BlueprintsProvider.provideServerlessBlueprints();
        assertNotNull(config.getDefaultBlueprint());

        Map<String, ServerlessBlueprint> blueprints = config.getBlueprints();
        assertNotNull(blueprints);
        assertNotNull(blueprints.get(config.getDefaultBlueprint()));

        for (Entry<String, ServerlessBlueprint> entry : blueprints.entrySet()) {
            ServerlessBlueprint blueprint = entry.getValue();
            NewServerlessProjectDataModel dataModel = mockServerlessProjectDataModel(blueprint.getDisplayName());
            assertServerlessDataModelTemplateData(dataModel);
            assertServerlessSamFileTemplateData(dataModel);
        }
    }

    // Test Lambda handlers generated from the customer provided template file is correct.
    // If no package prefix, we use the Maven package name as the name space, while if a
    // FQCN is provided, we use that instead.
    @Test
    public void testServerlessProjectDataModel_withTemplate() {
        assertServerlessHandlerTemplateData(null, "FooHandler");
        assertServerlessHandlerTemplateData("org.bar", "FooHandler");
    }

    // Assert the handler template data in the serverless template file is correct.
    private void assertServerlessHandlerTemplateData(String packageName, String className) {
        try {
            String fqcn = packageName == null ? className : packageName + "." + className;
            NewServerlessProjectDataModel dataModel = mockServerlessProjectDataModel(mockServerlessTemplateFile(fqcn));
            List<ServerlessHandlerTemplateData> template = dataModel.getServerlessHandlerTemplateData();
            assertEquals(1, template.size());
            assertEquals(template.get(0).getClassName(), className);
            if (packageName == null) {
                assertEquals(template.get(0).getPackageName(), FAKE_PACKAGE_NAME + ".function");
            } else {
                assertEquals(template.get(0).getPackageName(), packageName);
            }
        } catch (IOException | TemplateException e) {
            fail("Failed to assert serverless handler template data");
        }
    }

    // Assert the template data of API Gateway data model for Lambda is correct.
    private void assertServerlessDataModelTemplateData(NewServerlessProjectDataModel dataModel) {
        try {
            ServerlessDataModelTemplateData dataModelTemplateData = dataModel.getServerlessDataModelTemplateData();
            assertEquals(FAKE_PACKAGE_NAME + ".model", dataModelTemplateData.getPackageName());
            assertEquals("ServerlessInput", dataModelTemplateData.getServerlessInputClassName());
            assertEquals("ServerlessOutput", dataModelTemplateData.getServerlessOutputClassName());
        } catch (IOException | TemplateException e) {
            fail("Failed to test data model template data in " + dataModel.getBlueprintName());
        }
    }

    // Assert the SAM file template data in the blueprint is correct.
    private void assertServerlessSamFileTemplateData(NewServerlessProjectDataModel dataModel) {
        SamFileTemplateData samFileTemplateData = dataModel.getServerlessSamTemplateData();
        assertEquals(FAKE_PACKAGE_NAME + ".function", samFileTemplateData.getPackageName());
        assertEquals(FAKE_ARTIFACT_ID, samFileTemplateData.getArtifactId());
        assertEquals(FAKE_VERSION, samFileTemplateData.getVersion());
    }

    // Mock a NewServerlessProjectDataModel that uses an existing blueprint
    private NewServerlessProjectDataModel mockServerlessProjectDataModel(String blueprintName) {
        NewServerlessProjectDataModel dataModel = new NewServerlessProjectDataModel();

        MavenConfigurationDataModel mavenModel = dataModel.getMavenConfigurationDataModel();
        mavenModel.setGroupId(FAKE_GROUP_ID);
        mavenModel.setArtifactId(FAKE_ARTIFACT_ID);
        mavenModel.setVersion(FAKE_VERSION);
        mavenModel.setPackageName(FAKE_PACKAGE_NAME);

        dataModel.setBlueprintName(blueprintName);
        dataModel.setUseBlueprint(true);
        dataModel.setUseServerlessTemplateFile(false);

        return dataModel;
    }

    // Mock a NewServerlessProjectDataModel that uses a serverless template file.
    private NewServerlessProjectDataModel mockServerlessProjectDataModel(File templateFile) {
        NewServerlessProjectDataModel dataModel = new NewServerlessProjectDataModel();

        MavenConfigurationDataModel mavenModel = dataModel.getMavenConfigurationDataModel();
        mavenModel.setGroupId(FAKE_GROUP_ID);
        mavenModel.setArtifactId(FAKE_ARTIFACT_ID);
        mavenModel.setVersion(FAKE_VERSION);
        mavenModel.setPackageName(FAKE_PACKAGE_NAME);

        dataModel.setUseBlueprint(false);
        dataModel.setUseServerlessTemplateFile(true);
        dataModel.getImportFileDataModel().setFilePath(templateFile.getAbsolutePath());

        return dataModel;
    }

    private File mockServerlessTemplateFile(String handlerName) {
        String templateContent = String.format(
"{\n" +
"  \"Resources\": {\n" +
"    \"ServerlessFunction\": {\n" +
"      \"Type\" : \"AWS::Serverless::Function\",\n" +
"      \"Properties\" : {\n" +
"        \"CodeUri\" : \"fakeCodeUri\",\n" +
"        \"Handler\" : \"%s\",\n" +
"        \"Policies\" : [\n" +
"          \"Policy1\", \"Policy2\"\n" +
"        ]\n" +
"      }\n" +
"    }\n" +
"  }\n" +
"}", handlerName);

        try {
            File tempFile = File.createTempFile("serverless", ".template");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(templateContent);
            }
            return tempFile;
        } catch (IOException e) {
            fail("Failed to write serverless template content to a temp file.");
            return null;
        }
    }
}
