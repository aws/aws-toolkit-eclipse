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

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_CLASS_NAME;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_INPUT_NAME;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_INPUT_TYPE;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_OUTPUT_NAME;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_PACKAGE_NAME;
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_TYPE;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType.REQUEST_HANDLER;
import static com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType.STREAM_REQUEST_HANDLER;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.amazonaws.eclipse.core.validator.PackageNameValidator;
import com.amazonaws.eclipse.core.widget.ComboComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.UrlConstants;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.wizard.model.LambdaHandlerType;
import com.amazonaws.eclipse.lambda.project.wizard.model.PredefinedHandlerInputType;

import freemarker.template.Template;

@SuppressWarnings("restriction")
public class LambdaFunctionComposite {

    private final WizardPage parentWizard;
    private final Composite parentComposite;
    private final LambdaFunctionDataModel dataModel;
    private final DataBindingContext dataBindingContext;

    private TextComplex packageNameComplex;
    private TextComplex classNameComplex;
    private ComboComplex handlerTypeComplex;
    private ComboComplex inputTypeComplex;
    private TextComplex inputNameComplex;
    private TextComplex outputNameComplex;

    private Link handlerTypeDescriptionLink;
    private JavaSourceViewer sourcePreview;
    private Document sourcePreviewDocument;
    private final Template handlerTemplate;
    private final Template streamHandlerTemplate;

    private Group group;
    private Composite inputComposite;

    public LambdaFunctionComposite(WizardPage parentWizard, Composite parentComposite,
            LambdaFunctionDataModel dataModel, DataBindingContext dataBindingContext) {
        this.parentWizard = parentWizard;
        this.parentComposite = parentComposite;
        this.dataModel = dataModel;
        this.dataBindingContext = dataBindingContext;

        this.handlerTemplate = CodeTemplateManager.getInstance().getHandlerClassTemplate();
        this.streamHandlerTemplate = CodeTemplateManager.getInstance().getStreamHandlderClassTemplate();

        createControl();
    }

    public void createControl() {
        group = newGroup(parentComposite, "Lambda Function Handler");
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
        this.packageNameComplex = TextComplex.builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_PACKAGE_NAME))
                .validator(new PackageNameValidator("Package name must be provided!"))
                .labelValue("Package Name:")
                .defaultValue(dataModel.getPackageName())
                .textColSpan(2)
                .build();
    }

    public void createClassNameControl() {
        this.classNameComplex = TextComplex.builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_CLASS_NAME))
                .validator(new NotEmptyValidator("Please provide a valid class name for the handler"))
                .labelValue("Class Name:")
                .defaultValue(dataModel.getClassName())
                .textColSpan(2)
                .build();
    }

    public void createHandlerTypeControl() {
        this.handlerTypeComplex = ComboComplex.<LambdaHandlerType> builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_TYPE))
                .labelValue("Handler Type")
                .items(LambdaHandlerType.list())
                .defaultItem(LambdaHandlerType.REQUEST_HANDLER)
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onHandlerTypeSelectionChange();
                    }
                })
                .build();

        handlerTypeDescriptionLink = newLink(inputComposite, UrlConstants.webLinkListener, "", 1);
        setItalicFont(handlerTypeDescriptionLink);
    }

    public void createInputTypeControl() {
        this.inputTypeComplex = ComboComplex.<PredefinedHandlerInputType> builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_INPUT_TYPE))
                .labelValue("Input Type:")
                .items(PredefinedHandlerInputType.list())
                .defaultItem(PredefinedHandlerInputType.S3_EVENT)
                .selectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        onPredefinedHandlerInputTypeComboSelectionChange();
                    }
                })
                .build();

        this.inputNameComplex = TextComplex.builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_INPUT_NAME))
                .validator(new NotEmptyValidator("Please provide a valid input type"))
                .createLabel(false)
                .defaultValue(dataModel.getInputName())
                .build();
    }

    public void createOutputTypeControl() {
        this.outputNameComplex = TextComplex.builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoObservables.observeValue(dataModel, P_OUTPUT_NAME))
                .validator(new NotEmptyValidator("Please provide a valid output type"))
                .labelValue("Output Type:")
                .defaultValue(dataModel.getOutputName())
                .textColSpan(2)
                .build();
    }

    public void createSeparator() {
        Label separator = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    public TextComplex getPackageNameComplex() {
        return packageNameComplex;
    }

    private void onHandlerTypeSelectionChange() {
        final String handlerType = handlerTypeComplex.getCombo().getText();
        final Object handlerTypeData = handlerTypeComplex.getCombo().getData(handlerType);

        if (STREAM_REQUEST_HANDLER == handlerTypeData) {
            inputNameComplex.setEnabled(false);
            outputNameComplex.setEnabled(false);
            inputTypeComplex.getCombo().setEnabled(false);
            handlerTypeDescriptionLink.setText(createHandlerTypeDescriptionLink(STREAM_REQUEST_HANDLER));

        } else if (REQUEST_HANDLER == handlerTypeData) {

            String selectedText = inputTypeComplex.getCombo().getText();
            Object selectedData = inputTypeComplex.getCombo().getData(selectedText);

            inputNameComplex.setEnabled(selectedData == PredefinedHandlerInputType.CUSTOM);
            outputNameComplex.setEnabled(true);
            inputTypeComplex.getCombo().setEnabled(true);
            handlerTypeDescriptionLink.setText(createHandlerTypeDescriptionLink(REQUEST_HANDLER));

        } else {
            LambdaHandlerType lambdaHandlerType = (LambdaHandlerType)handlerTypeData;
            MessageDialog.openInformation(parentWizard.getShell(),
                    "Unsupported handler type combo selection.",
                    "The handler type " + handlerType + " is not yet supported in the toolkit! For more information, see "
                            + lambdaHandlerType.getDocUrl() + ".");
            handlerTypeComplex.getCombo().select(0);
            onHandlerTypeSelectionChange();
        }
    }

    /** Return the descriptive words for the specific Lambda Handler type. */
    private String createHandlerTypeDescriptionLink(LambdaHandlerType handlerType) {
        return "<a href=\"" + handlerType.getDocUrl() + "\">Learn more</a> about handlers.";
    }

    private void onPredefinedHandlerInputTypeComboSelectionChange() {
        String selectedText = inputTypeComplex.getCombo().getText();
        Object selectedData = inputTypeComplex.getCombo().getData(selectedText);

        inputNameComplex.setEnabled(PredefinedHandlerInputType.CUSTOM == selectedData);
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

        dataModel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                onPropertyChange();
            }
        });
    }

    private void onPropertyChange() {
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

    public void initialize() {
        onHandlerTypeSelectionChange();
        onPredefinedHandlerInputTypeComboSelectionChange();
        onPropertyChange();
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
