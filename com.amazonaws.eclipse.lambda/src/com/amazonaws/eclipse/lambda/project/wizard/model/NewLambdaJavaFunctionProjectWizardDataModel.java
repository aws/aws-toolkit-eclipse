package com.amazonaws.eclipse.lambda.project.wizard.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.amazonaws.eclipse.lambda.project.template.data.HandlerClassTemplateData;
import com.amazonaws.eclipse.lambda.project.template.data.HandlerTestClassTemplateData;

public class NewLambdaJavaFunctionProjectWizardDataModel {

    public static final String P_HANDLER_PACKAGE_NAME = "handlerPackageName";
    public static final String P_HANDLER_CLASS_NAME = "handlerClassName";
    public static final String P_PREDEFINED_HANDLER_INPUT_TYPE = "predefinedHandlerInputType";
    public static final String P_CUSTOM_HANDLER_INPUT_TYPE = "customHandlerInputType";
    public static final String P_HANDLER_OUTPUT_TYPE = "handlerOutputType";
    public static final String P_SHOW_README_FILE = "showReadmeFile";

    /* Function handler section */
    private String handlerPackageName;
    private String handlerClassName;
    private String handlerOutputType;

    private PredefinedHandlerInputType predefinedHandlerInputType;
    private String customHandlerInputType;

    /* Show README checkbox */
    private boolean showReadmeFile;

    public HandlerClassTemplateData collectHandlerTemplateData() {

        HandlerClassTemplateData data = new HandlerClassTemplateData();

        data.setPackageName(handlerPackageName);
        data.setHandlerClassName(handlerClassName);
        data.setOutputType(handlerOutputType);

        if (predefinedHandlerInputType != null) {
            data.addAdditionalImport(predefinedHandlerInputType.getFqcn());
            data.setInputType(predefinedHandlerInputType.getClassName());
        } else {
            data.setInputType(customHandlerInputType);
        }

        return data;
    }

    public HandlerTestClassTemplateData collectHandlerTestTemplateData() {

        HandlerTestClassTemplateData data = new HandlerTestClassTemplateData();

        data.setPackageName(handlerPackageName);
        data.setHandlerClassName(handlerClassName);
        data.setHandlerTestClassName(handlerClassName + "Test");
        data.setOutputType(handlerOutputType);

        if (predefinedHandlerInputType != null) {
            data.addAdditionalImport(predefinedHandlerInputType.getFqcn());
            data.setInputType(predefinedHandlerInputType.getClassName());
            data.setInputJsonFileName(predefinedHandlerInputType.getSampleInputJsonFile());
        } else {
            data.setInputType(customHandlerInputType);
        }

        return data;
    }

    public boolean requireSdkDependency() {
        return predefinedHandlerInputType != null && predefinedHandlerInputType.requireSdkDependency();
    }

    public String getHandlerPackageName() {
        return handlerPackageName;
    }

    public void setHandlerPackageName(String handlerPackageName) {
        String oldValue = this.handlerPackageName;
        this.handlerPackageName = handlerPackageName;
        this.pcs.firePropertyChange(P_HANDLER_PACKAGE_NAME, oldValue, handlerPackageName);
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public void setHandlerClassName(String handlerClassName) {
        String oldValue = this.handlerClassName;
        this.handlerClassName = handlerClassName;
        this.pcs.firePropertyChange(P_HANDLER_CLASS_NAME, oldValue, handlerClassName);
    }

    public String getHandlerOutputType() {
        return handlerOutputType;
    }

    public void setHandlerOutputType(String handlerOutputType) {
        String oldValue = this.handlerOutputType;
        this.handlerOutputType = handlerOutputType;
        this.pcs.firePropertyChange(P_HANDLER_OUTPUT_TYPE, oldValue, handlerOutputType);
    }

    public PredefinedHandlerInputType getPredefinedHandlerInputType() {
        return predefinedHandlerInputType;
    }

    public void setPredefinedHandlerInputType(
            PredefinedHandlerInputType predefinedHandlerInputType) {
        PredefinedHandlerInputType oldValue = this.predefinedHandlerInputType;
        this.predefinedHandlerInputType = predefinedHandlerInputType;
        this.pcs.firePropertyChange(P_PREDEFINED_HANDLER_INPUT_TYPE, oldValue, predefinedHandlerInputType);
    }

    public String getCustomHandlerInputType() {
        return customHandlerInputType;
    }

    public void setCustomHandlerInputType(String customHandlerInputType) {
        String oldValue = this.customHandlerInputType;
        this.customHandlerInputType = customHandlerInputType;
        this.pcs.firePropertyChange(P_CUSTOM_HANDLER_INPUT_TYPE, oldValue, customHandlerInputType);
    }

    public boolean isShowReadmeFile() {
        return showReadmeFile;
    }

    public void setShowReadmeFile(boolean showReadmeFile) {
        boolean oldValue = this.showReadmeFile;
        this.showReadmeFile = showReadmeFile;
        this.pcs.firePropertyChange(P_SHOW_README_FILE, oldValue, showReadmeFile);
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
