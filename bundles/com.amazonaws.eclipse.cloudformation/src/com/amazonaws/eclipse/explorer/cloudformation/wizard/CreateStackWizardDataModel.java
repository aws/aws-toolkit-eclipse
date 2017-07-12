/*
 * Copyright 2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import static com.amazonaws.eclipse.cloudformation.CloudFormationConstants.MAX_ALLOWED_TAG_AMOUNT;

import java.util.ArrayList;
import java.util.Collection;

import com.amazonaws.eclipse.cloudformation.model.ParametersDataModel;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel;
import com.amazonaws.eclipse.core.model.KeyValueSetDataModel.Pair;

/**
 * Data model for creating a new stack
 */
public class CreateStackWizardDataModel {

    private String stackName;
    private String templateUrl;
    private String templateFile;
    private Boolean useTemplateFile;
    private Boolean useTemplateUrl;
    private String snsTopicArn;
    private Boolean notifyWithSNS = false;
    private Integer timeoutMinutes;
    private Boolean rollbackOnFailure = true;
    private Collection<String> requiredCapabilities;
    // for use only with file templates, to avoid processing the file twice
    private String templateBody;
    private boolean usePreselectedTemplateFile;
    private final KeyValueSetDataModel tagModel = new KeyValueSetDataModel(MAX_ALLOWED_TAG_AMOUNT, new ArrayList<Pair>(MAX_ALLOWED_TAG_AMOUNT));
    private Mode mode = Mode.Create;

    private final ParametersDataModel parametersDataModel = new ParametersDataModel();

    public static enum Mode {
        Create, Update, EstimateCost
    };

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
    }

    public String getTemplateUrl() {
        return templateUrl;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public Boolean getUseTemplateFile() {
        return useTemplateFile;
    }

    public void setUseTemplateFile(Boolean useTemplateFile) {
        this.useTemplateFile = useTemplateFile;
    }

    public Boolean getUseTemplateUrl() {
        return useTemplateUrl;
    }

    public void setUseTemplateUrl(Boolean useTemplateUrl) {
        this.useTemplateUrl = useTemplateUrl;
    }

    public String getSnsTopicArn() {
        return snsTopicArn;
    }

    public void setSnsTopicArn(String snsTopicArn) {
        this.snsTopicArn = snsTopicArn;
    }

    public Boolean getNotifyWithSNS() {
        return notifyWithSNS;
    }

    public void setNotifyWithSNS(Boolean notifyWithSNS) {
        this.notifyWithSNS = notifyWithSNS;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Boolean getRollbackOnFailure() {
        return rollbackOnFailure;
    }

    public void setRollbackOnFailure(Boolean rollbackOnFailure) {
        this.rollbackOnFailure = rollbackOnFailure;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    public Collection<String> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(Collection<String> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities;
    }

    public boolean isUsePreselectedTemplateFile() {
        return usePreselectedTemplateFile;
    }

    public void setUsePreselectedTemplateFile(boolean usePreselectedTemplateFile) {
        this.usePreselectedTemplateFile = usePreselectedTemplateFile;
    }

    public KeyValueSetDataModel getTagModel() {
        return tagModel;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public ParametersDataModel getParametersDataModel() {
        return parametersDataModel;
    }

}
