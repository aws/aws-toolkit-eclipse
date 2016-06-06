package com.amazonaws.eclipse.lambda.project.wizard.model;

import com.amazonaws.eclipse.lambda.UrlConstants;

/**
 * This enum indicates all the Request Handler types provisioned by AWS Lambda. The Handler Type combo box
 * in the "Create a new AWS Lambda Java Project" UI will take use of these values.
 */
public enum LambdaHandlerType {
    // The order of the handler types matters that the first handler type in this enum will be chosen
    // as the default selection for the lambda handler combo.
    REQUEST_HANDLER("Request Handler", UrlConstants.LAMBDA_REQUEST_HANDLER_DOC_URL),
    STREAM_REQUEST_HANDLER("Stream Request Handler", UrlConstants.LAMBDA_STREAM_REQUEST_HANDLER_DOC_URL)
    ;

    private final String name;
    private final String docUrl;

    LambdaHandlerType(String name, String docUrl) {
        this.name = name;
        this.docUrl = docUrl;
    }

    public String getName() {
        return name;
    }

    public String getDocUrl() {
        return docUrl;
    }

}
