package com.amazonaws.eclipse.lambda.upload.wizard.model;

import com.amazonaws.services.identitymanagement.model.Role;

public class FunctionConfigPageDataModel {

    public static final String P_DESCRIPTION = "description";
    public static final String P_HANDLER = "handler";
    public static final String P_BUCKET_NAME = "bucketName";
    public static final String P_MEMORY = "memory";
    public static final String P_TIMEOUT = "timeout";

    private String description;
    private String handler;
    private Role role;
    private String bucketName;
    private Long memory;
    private Long timeout;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
}