package com.amazonaws.eclipse.lambda.project.template.data;

import java.util.LinkedList;
import java.util.List;

public class HandlerClassTemplateData extends AbstractHandlerClassTemplateData {

    private List<String> additionalImports;
    private String inputType;
    private String outputType;

    public List<String> getAdditionalImports() {
        return additionalImports;
    }

    public void addAdditionalImport(String additionalImport) {
        if (additionalImports == null) {
            additionalImports = new LinkedList<String>();
        }
        additionalImports.add(additionalImport);
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
