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
package com.amazonaws.eclipse.lambda.project.wizard.util;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCheckbox;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType.REQUEST_HANDLER;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType.STREAM_REQUEST_HANDLER;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_CUSTOM_HANDLER_INPUT_TYPE;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_HANDLER_CLASS_NAME;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_HANDLER_OUTPUT_TYPE;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_HANDLER_PACKAGE_NAME;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_HANDLER_TYPE;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel.P_SHOW_README_FILE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.UrlConstants;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaFunctionWizardDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.PredefinedHandlerInputType;
import com.amazonaws.eclipse.lambda.project.wizard.page.validator.ValidPackageNameValidator;
import com.amazonaws.eclipse.sdk.ui.JavaSdkManager;

import freemarker.template.Template;

@SuppressWarnings("restriction")
public class LambdaFunctionGroup {

    private WizardPage parentWizard;

    private final LambdaFunctionWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* Function handler section */
    private Text handlerPackageText;
    private ControlDecoration handlerPackageTextDecoration;
    private ISWTObservableValue handlerPackageTextObservable;

    private Text handlerClassText;
    private ControlDecoration handlerClassTextDecoration;
    private ISWTObservableValue handlerClassTextObservable;

    private Combo handlerTypeCombo;
    private Link handlerTypeDescriptionLink;
    private ISWTObservableValue handlerTypeComboObservable;

    private Combo predefinedHandlerInputCombo;

    private Text customHandlerInputTypeText;
    private ControlDecoration customHandlerInputTypeTextDecoration;
    private ISWTObservableValue customHandlerInputTypeTextObservable;
    private IObservableValue enableCustomHandlerInputTypeValidation = new WritableValue();

    private IObservableValue enableSdkInstalledValidation = new WritableValue();
    private IObservableValue sdkInstalledObservable = new WritableValue();

    private Text handlerOutputTypeText;
    private ControlDecoration handlerOutputTypeTextDecoration;
    private ISWTObservableValue handlerOutputTypeTextObservable;

    private JavaSourceViewer sourcePreview;
    private Document sourcePreviewDocument;
    private final Template handlerTemplate;
    private final Template streamHandlerTemplate;

    private static String CUSTOM_INPUT_TYPE_COMBO_TEXT = "Custom";
    private static Object CUSTOM_INPUT_TYPE_COMBO_DATA = new Object();

    /* Check box to opt-out showing README.html */
    private Button showReadmeFileCheckbox;
    private ISWTObservableValue showReadmeFileCheckboxObservable;

    private Composite parentComposite;
    private Group group;
    private Composite inputComposite;

    public LambdaFunctionGroup(final WizardPage parentWizard, final LambdaFunctionWizardDataModel dataModel) {
        this.parentWizard = parentWizard;
        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        this.handlerTemplate = CodeTemplateManager.getInstance().getHandlerClassTemplate();
        this.streamHandlerTemplate = CodeTemplateManager.getInstance().getStreamHandlderClassTemplate();
    }

    // This method is supposed to be called once and before creating the controls
    public void init(Composite composite) {
        parentComposite = composite;
        group = newGroup(composite, "Lambda Function Handler");
        GridLayout groupLayout = new GridLayout(1, true);
        groupLayout.marginWidth = 15;
        group.setLayout(groupLayout);

        String description =
                "Each Lambda function must specify a handler class " +
                "which the service will use as the entry point to begin execution.";
        setItalicFont(newLink(group, UrlConstants.webLinkListener,
                description + " <a href=\"" +
                UrlConstants.LAMBDA_EXECUTION_ROLE_DOC_URL +
                "\">Learn more</a> about Lambda Java function handler.",
                1));

        inputComposite = new Composite(group, SWT.NONE);
        inputComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout inputCompLayout = new GridLayout(3, true);
        inputCompLayout.marginHeight = 10;
        inputCompLayout.marginWidth = 10;
        inputComposite.setLayout(inputCompLayout);
    }

    public void createPackageNameControl() {
        // create package name control
        newFillingLabel(inputComposite, "Package Name:");
        handlerPackageText = newText(inputComposite, "", 2);
        handlerPackageTextDecoration = newControlDecoration(handlerPackageText, "");

        // bind package name control to data model
        handlerPackageTextObservable = SWTObservables
                .observeText(handlerPackageText, SWT.Modify);
        bindingContext.bindValue(handlerPackageTextObservable,
                PojoObservables.observeValue(dataModel, P_HANDLER_PACKAGE_NAME));

        // bind validation of package name
        ChainValidator<String> handlerPackageValidator = new ChainValidator<String>(
                handlerPackageTextObservable,
                new ValidPackageNameValidator("Please provide a valid package name for the handler class"));
        bindingContext.addValidationStatusProvider(handlerPackageValidator);
        new DecorationChangeListener(handlerPackageTextDecoration,
                handlerPackageValidator.getValidationStatus());
    }

    public void createClassNameControl() {
        // create Lambda function handler class name control
        newFillingLabel(inputComposite, "Class Name:");
        handlerClassText = newText(inputComposite, "", 2);
        handlerClassTextDecoration = newControlDecoration(handlerClassText, "");

        // bind Lambda function handler class name control to data model
        handlerClassTextObservable = SWTObservables
                .observeText(handlerClassText, SWT.Modify);
        bindingContext.bindValue(handlerClassTextObservable,
                PojoObservables.observeValue(dataModel, P_HANDLER_CLASS_NAME));

        // bind validation of class name
        ChainValidator<String> handlerClassValidator = new ChainValidator<String>(
                handlerClassTextObservable,
                new NotEmptyValidator("Please provide a valid class name for the handler"));
        bindingContext.addValidationStatusProvider(handlerClassValidator);
        new DecorationChangeListener(handlerClassTextDecoration,
                handlerClassValidator.getValidationStatus());
    }

    public void createLambdaHandlerControl() {
        // create handler type control
        newFillingLabel(inputComposite, "Handler Type");
        handlerTypeCombo = createHandlerTypeCombo(inputComposite, 1);
        handlerTypeDescriptionLink = newLink(inputComposite, UrlConstants.webLinkListener, "", 1);
        setItalicFont(handlerTypeDescriptionLink);

        // create input type control
        newFillingLabel(inputComposite, "Input Type:");
        predefinedHandlerInputCombo = createPredefinedHandlerInputTypeCombo(inputComposite, 1);
        customHandlerInputTypeText = newText(inputComposite, "", 1);
        customHandlerInputTypeTextDecoration = newControlDecoration(customHandlerInputTypeText, "");

        // create output type control
        newFillingLabel(inputComposite, "Output Type:");
        handlerOutputTypeText = newText(inputComposite, "", 2);
        handlerOutputTypeTextDecoration = newControlDecoration(handlerOutputTypeText, "");

        // bind handler type control to data model
        handlerTypeComboObservable = SWTObservables
                .observeText(handlerTypeCombo);
        bindingContext.bindValue(handlerTypeComboObservable,
                PojoObservables.observeValue(dataModel, P_HANDLER_TYPE));

        // bind input type control to data model
        customHandlerInputTypeTextObservable = SWTObservables
                .observeText(customHandlerInputTypeText, SWT.Modify);
        bindingContext.bindValue(customHandlerInputTypeTextObservable,
                PojoObservables.observeValue(dataModel, P_CUSTOM_HANDLER_INPUT_TYPE));

        // bind output type control to data model
        handlerOutputTypeTextObservable = SWTObservables
                .observeText(handlerOutputTypeText, SWT.Modify);
        bindingContext.bindValue(handlerOutputTypeTextObservable,
                PojoObservables.observeValue(dataModel, P_HANDLER_OUTPUT_TYPE));

        // bind validation of input type
        ChainValidator<String> customHandlerInputChainValidator = new ChainValidator<String>(
                customHandlerInputTypeTextObservable,
                enableCustomHandlerInputTypeValidation, //enabler
                new NotEmptyValidator("Please provide a valid input type"));
        bindingContext.addValidationStatusProvider(customHandlerInputChainValidator);
        new DecorationChangeListener(customHandlerInputTypeTextDecoration,
                customHandlerInputChainValidator.getValidationStatus());

        // bind validation of output type
        ChainValidator<String> handlerOutputChainValidator = new ChainValidator<String>(
                handlerOutputTypeTextObservable,
                new NotEmptyValidator("Please provide a valid output type"));
        bindingContext.addValidationStatusProvider(handlerOutputChainValidator);
        new DecorationChangeListener(handlerOutputTypeTextDecoration,
                handlerOutputChainValidator.getValidationStatus());

        // bind validation of SDK installed
        ChainValidator<Boolean> sdkInstalledValidator = new ChainValidator<Boolean>(
                sdkInstalledObservable,
                enableSdkInstalledValidation, // enabler
                new BooleanValidator(
                        "The selected input type requires the AWS Java SDK dependency. " +
                        "Please install the SDK first " +
                        "(Window -> Preference -> AWS Toolkit -> AWS SDK for Java) " +
                        "and then retry."));
        bindingContext.addValidationStatusProvider(sdkInstalledValidator);
    }

    public void createSeparator() {
        Label separator = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private Combo createHandlerTypeCombo(Composite composite, int colspan) {
        final Combo combo = newCombo(composite, 1);

        for (LambdaHandlerType handlerType : LambdaHandlerType.values()) {
            combo.add(handlerType.getName());
            combo.setData(handlerType.getName(), handlerType);
        }

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onHandlerTypeSelectionChange();
            }
        });
        return combo;
    }

    private void onHandlerTypeSelectionChange() {
        final String handlerType = handlerTypeCombo.getText();
        final Object handlerTypeData = handlerTypeCombo.getData(handlerType);

        if (STREAM_REQUEST_HANDLER == handlerTypeData) {
            customHandlerInputTypeText.setEnabled(false);
            enableCustomHandlerInputTypeValidation.setValue(false);

            handlerOutputTypeText.setEnabled(false);
            predefinedHandlerInputCombo.setEnabled(false);

            handlerTypeDescriptionLink.setText(createHandlerTypeDescriptionLink(STREAM_REQUEST_HANDLER));

        } else if (REQUEST_HANDLER == handlerTypeData) {

            String selectedText = predefinedHandlerInputCombo.getText();
            Object selectedData = predefinedHandlerInputCombo.getData(selectedText);

            boolean customHandlerInputTypeTextEnabled = selectedData == CUSTOM_INPUT_TYPE_COMBO_DATA;
            customHandlerInputTypeText.setEnabled(customHandlerInputTypeTextEnabled);
            enableCustomHandlerInputTypeValidation.setValue(customHandlerInputTypeTextEnabled);

            handlerOutputTypeText.setEnabled(true);
            predefinedHandlerInputCombo.setEnabled(true);

            handlerTypeDescriptionLink.setText(createHandlerTypeDescriptionLink(REQUEST_HANDLER));

        } else {
            LambdaHandlerType lambdaHandlerType = (LambdaHandlerType)handlerTypeData;
            MessageDialog.openInformation(parentWizard.getShell(),
                    "Unsupported handler type combo selection.",
                    "The handler type " + handlerType + " is not yet supported in the toolkit! For more information, see "
                            + lambdaHandlerType.getDocUrl() + ".");
            handlerTypeCombo.select(0);
            onHandlerTypeSelectionChange();
        }
    }

    /** Return the descriptive words for the specific Lambda Handler type. */
    private String createHandlerTypeDescriptionLink(LambdaHandlerType handlerType) {
        return "<a href=\"" + handlerType.getDocUrl() + "\">Learn more</a> about handlers.";
    }

    private Combo createPredefinedHandlerInputTypeCombo(Composite composite, int colspan) {

        final Combo combo = newCombo(composite, 1);

        for (PredefinedHandlerInputType type : PredefinedHandlerInputType.values()) {
            combo.add(type.getDisplayName());
            combo.setData(type.getDisplayName(), type);
        }

        combo.add(CUSTOM_INPUT_TYPE_COMBO_TEXT);
        combo.setData(CUSTOM_INPUT_TYPE_COMBO_TEXT, CUSTOM_INPUT_TYPE_COMBO_DATA);

        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onPredefinedHandlerInputTypeComboSelectionChange();
            }
        });

        return combo;
    }

    private void onPredefinedHandlerInputTypeComboSelectionChange() {
        String selectedText = predefinedHandlerInputCombo.getText();
        Object selectedData = predefinedHandlerInputCombo.getData(selectedText);

        if (selectedData == CUSTOM_INPUT_TYPE_COMBO_DATA) {
            customHandlerInputTypeText.setEnabled(true);
            enableCustomHandlerInputTypeValidation.setValue(true);
            dataModel.setPredefinedHandlerInputType(null);

            enableSdkInstalledValidation.setValue(false);

        } else if (selectedData instanceof PredefinedHandlerInputType) {
            customHandlerInputTypeText.setEnabled(false);
            enableCustomHandlerInputTypeValidation.setValue(false);

            PredefinedHandlerInputType type = (PredefinedHandlerInputType)selectedData;
            dataModel.setPredefinedHandlerInputType(type);

            enableSdkInstalledValidation.setValue(type.requireSdkDependency());

        } else {
            LambdaPlugin.getDefault().warn("Unknown combo selection " + selectedText, null);
        }

    }

    public void createHandlerSourcePreview() {

        newFillingLabel(group, "Preview:", 1);

        sourcePreviewDocument = new Document("");
        IPreferenceStore javaPluginPrefStore = JavaPlugin.getDefault()
                .getCombinedPreferenceStore();

        sourcePreview = new JavaSourceViewer(group, null, null, false,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, javaPluginPrefStore);
        sourcePreview.setEditable(false);
        sourcePreview.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));

        // Setting up Java Syntax Highlight
        JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
        tools.setupJavaDocumentPartitioner(sourcePreviewDocument, IJavaPartitions.JAVA_PARTITIONING);
        SimpleJavaSourceViewerConfiguration sourceConfig = new SimpleJavaSourceViewerConfiguration(
                tools.getColorManager(), javaPluginPrefStore, null,
                IJavaPartitions.JAVA_PARTITIONING, true);
        sourcePreview.configure(sourceConfig);

        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 1;
        gridData.heightHint = 50;
        sourcePreview.getTextWidget().setLayoutData(gridData);
        sourcePreview.setDocument(sourcePreviewDocument);

        linkPreviewWithHandlerConfigInput();
    }

    private void linkPreviewWithHandlerConfigInput() {

        dataModel.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                Template template = dataModel.isUseStreamHandler() ? streamHandlerTemplate : handlerTemplate;
                Object freeMarkerDataModel = dataModel.isUseStreamHandler() ?
                        dataModel.collectStreamHandlerTemplateData() :
                        dataModel.collectHandlerTemplateData();

                StringWriter sw = new StringWriter();
                try {
                    template.process(freeMarkerDataModel, sw);
                    sw.flush();
                } catch (Exception e) {
                    LambdaPlugin.getDefault().reportException(
                            "Failed to generate handler source preview", e);
                }
                try {
                    sw.close();
                } catch (IOException ignored) {
                }

                String source = sw.toString();
                if (sourcePreviewDocument != null) {
                    sourcePreviewDocument.set(source);
                }
                if (sourcePreview != null) {
                    sourcePreview.getTextWidget().setRedraw(true);
                }
            }
        });
    }

    public void createShowReadmeFileCheckBox() {
        showReadmeFileCheckbox = newCheckbox(parentComposite, "Show README guide after creating the project", 1);

        showReadmeFileCheckboxObservable = SWTObservables.observeSelection(showReadmeFileCheckbox);
        bindingContext.bindValue(showReadmeFileCheckboxObservable,
                PojoObservables.observeValue(dataModel, P_SHOW_README_FILE));
    }

    public void initializeValidators(IChangeListener listener) {
        aggregateValidationStatus.addChangeListener(listener);
    }

    public void runHandlerValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

    public void initializeDefaults() {
        if (handlerPackageTextObservable != null) {
            handlerPackageTextObservable.setValue("");
        }
        if (handlerClassTextObservable != null) {
            handlerClassTextObservable.setValue("LambdaFunctionHandler");
        }
        if (customHandlerInputTypeTextObservable != null) {
            customHandlerInputTypeTextObservable.setValue("Object");
        }
        if (handlerOutputTypeTextObservable != null) {
            handlerOutputTypeTextObservable.setValue("Object");
        }

        if (handlerTypeCombo != null) {
            handlerTypeCombo.select(0);
            onHandlerTypeSelectionChange();
        }

        if (predefinedHandlerInputCombo != null) {
            predefinedHandlerInputCombo.select(0);
            onPredefinedHandlerInputTypeComboSelectionChange();
        }

        if (sdkInstalledObservable != null) sdkInstalledObservable.setValue(checkSdkInstalled());

        if (showReadmeFileCheckboxObservable != null) {
            showReadmeFileCheckboxObservable.setValue(
                LambdaPlugin.getDefault().getPreferenceStore()
                        .getBoolean(LambdaPlugin.PREF_K_SHOW_README_AFTER_CREATE_NEW_PROJECT));
        }
    }

    private boolean checkSdkInstalled() {
        return JavaSdkManager.getInstance().getDefaultSdkInstall() != null;
    }

    public IStatus getHandlerInfoValidationStatus() {
        if (aggregateValidationStatus == null) {
            return null;
        }

        Object value = aggregateValidationStatus.getValue();
        if (! (value instanceof IStatus)) return null;
        return (IStatus)value;
    }

    private Font italicFont;

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    public void dispose() {
        if (italicFont != null) {
            italicFont.dispose();
        }
    }
}
