package com.amazonaws.eclipse.lambda.project.template.data;


public class HandlerTestClassTemplateData extends HandlerClassTemplateData {

    private String handlerTestClassName;
    private String inputJsonFileName;

    public String getHandlerTestClassName() {
        return handlerTestClassName;
    }

    public void setHandlerTestClassName(String handlerTestClassName) {
        this.handlerTestClassName = handlerTestClassName;
    }

    public void setInputJsonFileName(String inputJsonFileName) {
        this.inputJsonFileName = inputJsonFileName;
    }

    public String getInputJsonFileName() {
        return inputJsonFileName;
    }
}
