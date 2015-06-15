package com.amazonaws.eclipse.lambda.upload.wizard.model;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;

public class UploadFunctionWizardDataModel {

    public static final String P_REGION = "region";
    public static final String P_IS_CREATING_NEW_FUNCTION = "creatingNewFunction";
    public static final String P_NEW_FUNCTION_NAME = "newFunctionName";

    private final IProject project;
    private final List<String> requestHandlerImplementerClasses;

    /* Page 1*/
    private Region region;
    private boolean isCreatingNewFunction;
    private FunctionConfiguration existingFunction;
    private String newFunctionName;

    /* Page 2 */
    private FunctionConfigPageDataModel functionConfigPageDataModel;

    public CreateFunctionRequest toCreateFunctionRequest() {
        return new CreateFunctionRequest()
                .withFunctionName(newFunctionName)
                .withRuntime(ServiceApiUtils.JAVA_8)
                .withDescription(functionConfigPageDataModel.getDescription())
                .withHandler(functionConfigPageDataModel.getHandler())
                .withRole(functionConfigPageDataModel.getRole().getArn())
                .withMemorySize(functionConfigPageDataModel.getMemory().intValue())
                .withTimeout(functionConfigPageDataModel.getTimeout().intValue());
    }

    public UpdateFunctionConfigurationRequest toUpdateFunctionConfigRequest() {
        return new UpdateFunctionConfigurationRequest()
                .withFunctionName(existingFunction.getFunctionName())
                .withDescription(functionConfigPageDataModel.getDescription())
                .withHandler(functionConfigPageDataModel.getHandler())
                .withRole(functionConfigPageDataModel.getRole().getArn())
                .withMemorySize(functionConfigPageDataModel.getMemory().intValue())
                .withTimeout(functionConfigPageDataModel.getTimeout().intValue());
    }

    /**
     * @param project
     *            the project being uploaded
     * @param requestHandlerImplementerClasses
     *            a non-empty list of FQCNs of the classes within the project
     *            that implement the RequestHandler interface.
     */
    public UploadFunctionWizardDataModel(IProject project,
            List<String> requestHandlerImplementerClasses) {

        this.project = project;

        if (requestHandlerImplementerClasses.isEmpty()) {
            throw new IllegalArgumentException(
                    "requestHandlerImplementerClasses must not be empty.");
        }
        this.requestHandlerImplementerClasses = Collections
                .unmodifiableList(requestHandlerImplementerClasses);

        this.functionConfigPageDataModel = new FunctionConfigPageDataModel();
    }

    public IProject getProject() {
        return project;
    }

    public List<String> getRequestHandlerImplementerClasses() {
        return requestHandlerImplementerClasses;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public boolean isCreatingNewFunction() {
        return isCreatingNewFunction;
    }

    public void setCreatingNewFunction(boolean isCreatingNewFunction) {
        this.isCreatingNewFunction = isCreatingNewFunction;
    }

    public FunctionConfiguration getExistingFunction() {
        return existingFunction;
    }

    public void setExistingFunction(FunctionConfiguration existingFunction) {
        this.existingFunction = existingFunction;
    }

    public String getNewFunctionName() {
        return newFunctionName;
    }

    public void setNewFunctionName(String newFunctionName) {
        this.newFunctionName = newFunctionName;
    }

    public FunctionConfigPageDataModel getFunctionConfigPageDataModel() {
        return functionConfigPageDataModel;
    }

    public void setFunctionConfigPageDataModel(
            FunctionConfigPageDataModel functionConfigPageDataModel) {
        this.functionConfigPageDataModel = functionConfigPageDataModel;
    }

}
