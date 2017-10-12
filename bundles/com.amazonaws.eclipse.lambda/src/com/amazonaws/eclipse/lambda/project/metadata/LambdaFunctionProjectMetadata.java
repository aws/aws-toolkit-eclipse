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
package com.amazonaws.eclipse.lambda.project.metadata;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metadata POJO for deploying or invoking Lambda functions. This POJO records the last Lambda function deployment or invoke
 * information which could be reused for the next time of deployment or invoke.
 */
public class LambdaFunctionProjectMetadata {

    private String lastDeploymentHandler;
    private String lastInvokeHandler;

    private final Map<String, LambdaFunctionMetadata> handlerMetadata = new HashMap<>();

    /**
     * Helper methods for getting and setting last deployment metadata according to {@link #lastDeploymentHandler} field.
     */

    /**
     * Whether the chosen Lambda handler is deployed yet or not.
     */
    public boolean isLastInvokedHandlerDeployed() {
        LambdaFunctionMetadata metadata = handlerMetadata.get(lastInvokeHandler);
        return metadata == null ? false : metadata.getDeployment() != null;
    }

    @JsonIgnore
    public Region getLastDeploymentRegion() {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        return lastDeploy == null ? null : RegionUtils.getRegion(lastDeploy.getRegionId());
    }

    public void setLastDeploymentRegion(String regionId) {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        if (lastDeploy != null) {
            lastDeploy.setRegionId(regionId);
        }
    }

    @JsonIgnore
    public String getLastDeploymentFunctionName() {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        return lastDeploy == null ? null : lastDeploy.getAwsLambdaFunctionName();
    }

    public void setLastDeploymentFunctionName(String functionName) {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        if (lastDeploy != null) {
            lastDeploy.setAwsLambdaFunctionName(functionName);
        }
    }

    @JsonIgnore
    public String getLastDeploymentBucketName() {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        return lastDeploy == null ? null : lastDeploy.getAwsS3BucketName();
    }

    public void setLastDeploymentBucketName(String bucketName) {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        if (lastDeploy != null) {
            lastDeploy.setAwsS3BucketName(bucketName);
        }
    }

    @JsonIgnore
    public String getLastDeploymentRoleName() {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        return lastDeploy == null ? null : lastDeploy.getAwsIamRoleName();
    }

    public void setLastDeploymentRoleName(String roleName) {
        LambdaFunctionDeploymentMetadata lastDeploy = getLastDeployment();
        if (lastDeploy != null) {
            lastDeploy.setAwsIamRoleName(roleName);
        }
    }

    /**
     * Create the path to the target {@link LambdaFunctionDeploymentMetadata} in the Pojo if it is null and return it.
     */
    private LambdaFunctionDeploymentMetadata getLastDeployment() {
        if (lastDeploymentHandler != null) {
            LambdaFunctionMetadata functionMetadata = handlerMetadata.get(lastDeploymentHandler);
            if (functionMetadata == null) {
                functionMetadata = new LambdaFunctionMetadata();
                handlerMetadata.put(lastDeploymentHandler, functionMetadata);
            }
            if (functionMetadata.getDeployment() == null) {
                functionMetadata.setDeployment(new LambdaFunctionDeploymentMetadata());
            }
            return functionMetadata.getDeployment();
        }
        return null;
    }

    /**
     * Helper methods for getting and setting last invoke metadata according to {@link #lastInvokeHandler} field.
     */
    @JsonIgnore
    public boolean getLastInvokeSelectJsonInput() {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        return lastInvoke == null ? false : lastInvoke.isSelectJsonInput();
    }

    @JsonIgnore
    public void setLastInvokeSelectJsonInput(boolean selectJsonInput) {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        if (lastInvoke != null) {
            lastInvoke.setSelectJsonInput(selectJsonInput);
        }
    }

    @JsonIgnore
    public String getLastInvokeInput() {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        return lastInvoke == null ? "" : lastInvoke.getInvokeInput();
    }

    @JsonIgnore
    public void setLastInvokeInput(String invokeInput) {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        if (lastInvoke != null) {
            lastInvoke.setInvokeInput(invokeInput);
        }
    }

    @JsonIgnore
    public boolean getLastInvokeShowLiveLog() {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        return lastInvoke == null ? true : lastInvoke.isShowLiveLog();
    }

    @JsonIgnore
    public void setLastInvokeShowLiveLog(boolean showLiveLog) {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        if (lastInvoke != null) {
            lastInvoke.setShowLiveLog(showLiveLog);
        }
    }

    @JsonIgnore
    public boolean getLastInvokeSelectJsonFile() {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        return lastInvoke == null ? false : lastInvoke.isSelectJsonFile();
    }

    @JsonIgnore
    public void setLastInvokeSelectJsonFile(boolean selectJsonFile) {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        if (lastInvoke != null) {
            lastInvoke.setSelectJsonFile(selectJsonFile);
        }
    }

    @JsonIgnore
    public String getLastInvokeJsonFile() {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        return lastInvoke == null ? "" : lastInvoke.getInvokeJsonFile();
    }

    @JsonIgnore
    public void setLastInvokeJsonFile(String invokeJsonFile) {
        LambdaFunctionInvokeMetadata lastInvoke = getLastInvoke();
        if (lastInvoke != null) {
            lastInvoke.setInvokeJsonFile(invokeJsonFile);
        }
    }

    /**
     * Create the path to the target {@link LambdaFunctionInvokeMetadata} in the Pojo if it is null and return it.
     */
    private LambdaFunctionInvokeMetadata getLastInvoke() {
        if (lastInvokeHandler != null) {
            LambdaFunctionMetadata functionMetadata = handlerMetadata.get(lastInvokeHandler);
            if (functionMetadata == null) {
                functionMetadata = new LambdaFunctionMetadata();
                handlerMetadata.put(lastInvokeHandler, functionMetadata);
            }
            if (functionMetadata.getInvoke() == null) {
                functionMetadata.setInvoke(new LambdaFunctionInvokeMetadata());
            }
            return functionMetadata.getInvoke();
        }
        return null;
    }

    public String getLastDeploymentHandler() {
        return lastDeploymentHandler;
    }

    public void setLastDeploymentHandler(String lastDeploymentHandler) {
        this.lastDeploymentHandler = lastDeploymentHandler;
    }

    public Map<String, LambdaFunctionMetadata> getHandlerMetadata() {
        return handlerMetadata;
    }

    public String getLastInvokeHandler() {
        return lastInvokeHandler;
    }

    public void setLastInvokeHandler(String lastInvokeHandler) {
        this.lastInvokeHandler = lastInvokeHandler;
    }

    /**
     * Cached settings for the operations to a Lambda function.
     */
    public static class LambdaFunctionMetadata {
        // Last deployment metadata
        private LambdaFunctionDeploymentMetadata deployment;
        // Last invoke metadata
        private LambdaFunctionInvokeMetadata invoke;

        public LambdaFunctionDeploymentMetadata getDeployment() {
            return deployment;
        }
        public void setDeployment(LambdaFunctionDeploymentMetadata deployment) {
            this.deployment = deployment;
        }
        public LambdaFunctionInvokeMetadata getInvoke() {
            return invoke;
        }
        public void setInvoke(LambdaFunctionInvokeMetadata invoke) {
            this.invoke = invoke;
        }
    }

    /**
     * Data Model for deploying a Lambda function.
     */
    public static class LambdaFunctionDeploymentMetadata {
        private String regionId;
        private String awsLambdaFunctionName;
        private String awsIamRoleName;
        private String awsS3BucketName;
        private int memory;
        private int timeout;

        public String getRegionId() {
            return regionId;
        }
        public void setRegionId(String regionId) {
            this.regionId = regionId;
        }
        public String getAwsLambdaFunctionName() {
            return awsLambdaFunctionName;
        }
        public void setAwsLambdaFunctionName(String awsLambdaFunctionName) {
            this.awsLambdaFunctionName = awsLambdaFunctionName;
        }
        public String getAwsIamRoleName() {
            return awsIamRoleName;
        }
        public void setAwsIamRoleName(String awsIamRoleName) {
            this.awsIamRoleName = awsIamRoleName;
        }
        public String getAwsS3BucketName() {
            return awsS3BucketName;
        }
        public void setAwsS3BucketName(String awsS3BucketName) {
            this.awsS3BucketName = awsS3BucketName;
        }
        public int getMemory() {
            return memory;
        }
        public void setMemory(int memory) {
            this.memory = memory;
        }
        public int getTimeout() {
            return timeout;
        }
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Data model for invoking a Lambda function.
     */
    public static class LambdaFunctionInvokeMetadata {
        // Select a Json file as input
        private boolean selectJsonFile = true;
        private String invokeJsonFile;
        // Select a Json content as input
        private boolean selectJsonInput = false;
        private String invokeInput;
        private boolean showLiveLog = true;

        public String getInvokeJsonFile() {
            return invokeJsonFile == null ? "" : invokeJsonFile;
        }
        public void setInvokeJsonFile(String invokeJsonFile) {
            this.invokeJsonFile = invokeJsonFile;
        }
        public boolean isSelectJsonInput() {
            return selectJsonInput;
        }
        public void setSelectJsonInput(boolean selectJsonInput) {
            this.selectJsonInput = selectJsonInput;
        }
        public String getInvokeInput() {
            return invokeInput == null ? "" : invokeInput;
        }
        public void setInvokeInput(String invokeInput) {
            this.invokeInput = invokeInput;
        }
        public boolean isSelectJsonFile() {
            return selectJsonFile;
        }
        public void setSelectJsonFile(boolean selectJsonFile) {
            this.selectJsonFile = selectJsonFile;
        }
        public boolean isShowLiveLog() {
            return showLiveLog;
        }
        public void setShowLiveLog(boolean showLiveLog) {
            this.showLiveLog = showLiveLog;
        }
    }
}
