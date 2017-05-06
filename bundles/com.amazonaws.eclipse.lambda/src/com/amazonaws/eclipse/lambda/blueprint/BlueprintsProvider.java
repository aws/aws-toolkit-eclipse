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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class that parses Serverless and Lambda blueprints config files.
 */
public class BlueprintsProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static volatile ServerlessBlueprintsConfig serverlessBlueprintsConfig;
    private static volatile LambdaBlueprintsConfig lambdaBlueprintsConfig;

    public static ServerlessBlueprintsConfig provideServerlessBlueprints() {
        if (serverlessBlueprintsConfig == null) {
            try {
                serverlessBlueprintsConfig = MAPPER.readValue(
                        CodeTemplateManager.getInstance().getServerlessBlueprintsConfigFile(),
                        ServerlessBlueprintsConfig.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return serverlessBlueprintsConfig;
    }

    public static List<String> getServerlessBlueprintDisplayNames() {
        ServerlessBlueprintsConfig config = provideServerlessBlueprints();
        List<String> displayNames = new ArrayList<>();
        for (ServerlessBlueprint blueprint : config.getBlueprints().values()) {
            displayNames.add(blueprint.getDisplayName());
        }
        return displayNames;
    }

    public static LambdaBlueprintsConfig provideLambdaBlueprints() {
        if (lambdaBlueprintsConfig == null) {
            try {
                lambdaBlueprintsConfig = MAPPER.readValue(
                        CodeTemplateManager.getInstance().getLambdaBlueprintsConfigFile(),
                        LambdaBlueprintsConfig.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return lambdaBlueprintsConfig;
    }
}
