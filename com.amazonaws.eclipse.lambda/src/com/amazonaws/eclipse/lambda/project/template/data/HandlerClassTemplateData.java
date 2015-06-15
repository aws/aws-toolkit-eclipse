package com.amazonaws.eclipse.lambda.project.template.data;

import java.util.LinkedList;
import java.util.List;

public class HandlerClassTemplateData {

    private String packageName;
    private List<String> additionalImports;
    private String handlerClassName;
    private String inputType;
    private String outputType;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getAdditionalImports() {
        return additionalImports;
    }

    public void addAdditionalImport(String additionalImport) {
        if (additionalImports == null) {
            additionalImports = new LinkedList<String>();
        }
        additionalImports.add(additionalImport);
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public void setHandlerClassName(String className) {
        this.handlerClassName = className;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

}
