package com.amazonaws.eclipse.lambda.project.template.data;

public class AbstractHandlerClassTemplateData {
    private String packageName;
    private String handlerClassName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public void setHandlerClassName(String handlerClassName) {
        this.handlerClassName = handlerClassName;
    }
}
