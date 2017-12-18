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
package com.amazonaws.eclipse.lambda.project.wizard.model;

import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_EVENT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_HOST;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_LAMBDA_IDENTIFIER;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PORT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.model.RegionDataModel;

/**
 * Data model for debugging Sam function using SAM Local.
 */
public class RunSamLocalDataModel {
    public static final int DEFAULT_DEBUG_PORT = 5858;
    public static final String DEFAULT_MAVEN_GOALS = "clean package";
    public static final String DEFAULT_PROFILE = "default";
    public static final String DEFAULT_REGION = "us-east-1";
    public static final int DEFAULT_TIME_OUT = 300;

    public static final String P_DEBUG_PORT = "debugPort";
    public static final String P_SAM_ACTION = "samAction";
    public static final String P_SAM_RUNTIME = "samRuntime";
    public static final String P_MAVEN_GOALS = "mavenGoals";
    public static final String P_CODE_URI = "codeUri";
    public static final String P_TIME_OUT = "timeOut";

    private final RegionDataModel regionDataModel = new RegionDataModel();
    private final ImportFileDataModel workspaceDataModel = new ImportFileDataModel();
    private final ImportFileDataModel templateFileLocationDataModel = new ImportFileDataModel();
    private final ImportFileDataModel envvarFileLocationDataModel = new ImportFileDataModel();

    private AccountInfo account;
    private String mavenGoals = DEFAULT_MAVEN_GOALS;
    private String samRuntime;
    private int debugPort = DEFAULT_DEBUG_PORT;
    private SamAction samAction = SamAction.INVOKE;
    private AttributeMapConvertible actionDataModel;
    private String codeUri;
    private int timeOut = DEFAULT_TIME_OUT;

    public String getMavenGoals() {
        return mavenGoals;
    }

    public void setMavenGoals(String mavenGoals) {
        this.mavenGoals = mavenGoals;
    }

    public String getSamRuntime() {
        return samRuntime;
    }

    public void setSamRuntime(String samRuntime) {
        this.samRuntime = samRuntime;
    }

    public String getCodeUri() {
        return codeUri;
    }

    public void setCodeUri(String codeUri) {
        this.codeUri = codeUri;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public enum SamAction {
        INVOKE("invoke", "Lambda Function"),
        START_API("start-api", "API Gateway"),
        ;

        // Action name must be a valid command line action `sam local ${action}`
        private final String name;
        private final String description;

        private SamAction(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public static List<SamAction> toList() {
            return Arrays.asList(SamAction.values());
        }

        public static SamAction fromValue(String value) {
            for (SamAction samAction : SamAction.values()) {
                if (samAction.getName().equals(value)) {
                    return samAction;
                }
            }
            return null;
        }
    }

    public AccountInfo getAccount() {
        return account;
    }

    public void setAccount(AccountInfo account) {
        this.account = account;
    }

    public RegionDataModel getRegionDataModel() {
        return regionDataModel;
    }

    public ImportFileDataModel getWorkspaceDataModel() {
        return workspaceDataModel;
    }

    public ImportFileDataModel getTemplateFileLocationDataModel() {
        return templateFileLocationDataModel;
    }

    public ImportFileDataModel getEnvvarFileLocationDataModel() {
        return envvarFileLocationDataModel;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public AttributeMapConvertible getActionDataModel() {
        return actionDataModel;
    }

    public void setActionDataModel(AttributeMapConvertible actionDataModel) {
        this.actionDataModel = actionDataModel;
    }

    public SamAction getSamAction() {
        return samAction;
    }

    public void setSamAction(SamAction samAction) {
        this.samAction = samAction;
    }

    public static class SamLocalInvokeFunctionDataModel implements AttributeMapConvertible {
        public static final String P_LAMBDA_IDENTIFIER = "lambdaIdentifier";

        private String lambdaIdentifier;
        private final ImportFileDataModel eventFileLocationDataModel = new ImportFileDataModel();

        public String getLambdaIdentifier() {
            return lambdaIdentifier;
        }

        public void setLambdaIdentifier(String lambdaIdentifier) {
            this.lambdaIdentifier = lambdaIdentifier;
        }

        public ImportFileDataModel getEventFileLocationDataModel() {
            return eventFileLocationDataModel;
        }

        @Override
        public Map<String, String> toAttributeMap() {
            Map<String, String> attributes = new HashMap<>();

            attributes.put(A_EVENT, eventFileLocationDataModel.getFilePath());
            attributes.put(A_LAMBDA_IDENTIFIER, lambdaIdentifier);

            return attributes;
        }
    }

    public static class SamLocalStartApiDataModel implements AttributeMapConvertible {
        public static final int DEFAULT_PORT = 3000;
        public static final String DEFAULT_HOST = "127.0.0.1";

        public static final String P_PORT = "port";
        public static final String P_HOST = "host";

        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;

        public String getHost() {
            return host;
        }
        public void setHost(String host) {
            this.host = host;
        }
        public int getPort() {
            return port;
        }
        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public Map<String, String> toAttributeMap() {
            Map<String, String> attributes = new HashMap<>();
            attributes.put(A_HOST, host);
            attributes.put(A_PORT, String.valueOf(port));
            return attributes;
        }
    }

    public static interface AttributeMapConvertible {
        Map<String, String> toAttributeMap();
    }
}
