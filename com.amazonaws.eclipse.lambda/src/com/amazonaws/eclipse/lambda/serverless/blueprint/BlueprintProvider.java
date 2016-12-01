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
package com.amazonaws.eclipse.lambda.serverless.blueprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BlueprintProvider {
    
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private static BlueprintProvider INSTANCE;
    private final BlueprintsConfig blueprintsConfig;
    
    private BlueprintProvider() throws JsonParseException, JsonMappingException, IOException {
        blueprintsConfig = MAPPER.readValue(CodeTemplateManager.getInstance().getBlueprintConfigFile(),
                BlueprintsConfig.class);
    }

    public List<String> getBlueprintNames() {
        List<String> names = new ArrayList<String>();
        for (String name : blueprintsConfig.getBlueprints().keySet()) {
            names.add(name);
        }
        return names;
    }
    
    public Map<String, String> getBlueprintDescriptions() {
        Map<String, String> descriptions = new HashMap<String, String>();
        for (Map.Entry<String, Blueprint> entry : blueprintsConfig.getBlueprints().entrySet()) {
            descriptions.put(entry.getKey(), entry.getValue().getDescription());
        }
        return descriptions;
    }
    
    public Blueprint getBlueprint(String blueprintName) {
        return blueprintsConfig.getBlueprints().get(blueprintName);
    }
    
    public static BlueprintProvider getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new BlueprintProvider();
            } catch (IOException e) {
                LambdaPlugin.getDefault().reportException("Cannot instantiate Blueprint Provider.", e);
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }
}