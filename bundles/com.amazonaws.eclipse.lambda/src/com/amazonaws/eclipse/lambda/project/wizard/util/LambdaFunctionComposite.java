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
import static com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel.P_INPUT_TYPE;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.SimpleJavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.SWT;
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

import com.amazonaws.eclipse.core.widget.ComboComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.lambda.LambdaConstants;
import com.amazonaws.eclipse.lambda.blueprint.BlueprintsProvider;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprint;
import com.amazonaws.eclipse.lambda.blueprint.LambdaBlueprintsConfig;
import com.amazonaws.eclipse.lambda.model.LambdaFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.template.CodeTemplateManager;
import com.amazonaws.eclipse.lambda.project.template.data.LambdaBlueprintTemplateData;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@SuppressWarnings("restriction")
public class LambdaFunctionComposite {

    private final Composite parentComposite;
    private final LambdaFunctionDataModel dataModel;
    private final LambdaBlueprintsConfig lambdaBlueprintConfig;
    private final DataBindingContext dataBindingContext;

    private TextComplex packageNameComplex;
    private TextComplex classNameComplex;
    private ComboComplex inputTypeComplex;

    private Link handlerTypeDescriptionLink;
    private JavaSourceViewer sourcePreview;
    private Document sourcePreviewDocument;

    private Group group;
    private Composite inputComposite;

    public LambdaFunctionComposite(Composite parentComposite,
            LambdaFunctionDataModel dataModel, DataBindingContext dataBindingContext) {
        this.parentComposite = parentComposite;
        this.dataModel = dataModel;
        this.dataBindingContext = dataBindingContext;
        this.lambdaBlueprintConfig = BlueprintsProvider.provideLambdaBlueprints();

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
        setItalicFont(newLink(group, LambdaConstants.webLinkListener,
                description + " <a href=\"" +
                LambdaConstants.LAMBDA_JAVA_HANDLER_DOC_URL +
                "\">Learn more</a> about Lambda Java function handler.",
                1));

        inputComposite = new Composite(group, SWT.NONE);
        inputComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout inputCompLayout = new GridLayout(3, true);
        inputCompLayout.marginHeight = 10;
        inputCompLayout.marginWidth = 10;
        inputComposite.setLayout(inputCompLayout);
    }

    public void createClassNameControl() {
        this.classNameComplex = TextComplex.builder(inputComposite, dataBindingContext,
                    PojoProperties.value(P_CLASS_NAME).observe(dataModel))
                .addValidator(new NotEmptyValidator("Please provide a valid class name for the handler"))
                .labelValue("Class Name:")
                .defaultValue(dataModel.getClassName())
                .textColSpan(2)
                .build();
    }

    public void createInputTypeControl() {
        this.inputTypeComplex = ComboComplex.<LambdaBlueprint> builder()
                .composite(inputComposite)
                .dataBindingContext(dataBindingContext)
                .pojoObservableValue(PojoProperties.value(LambdaFunctionDataModel.class, P_INPUT_TYPE).observe(dataModel))
                .labelValue("Input Type:")
                .items(lambdaBlueprintConfig.getBlueprints().values())
                .defaultItemName(dataModel.getInputType())
                .comboColSpan(2)
                .build();

        handlerTypeDescriptionLink = newLink(inputComposite, LambdaConstants.webLinkListener,
                dataModel.getSelectedBlueprint().getDescription(), 3);
        setItalicFont(handlerTypeDescriptionLink);
    }

    public void createSeparator() {
        Label separator = new Label(group, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    public TextComplex getPackageNameComplex() {
        return packageNameComplex;
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
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                onPropertyChange();
            }
        });
    }

    private void onPropertyChange() {
        Template template = CodeTemplateManager.getInstance().getLambdaHandlerTemplate(
                dataModel.getSelectedBlueprint());
        LambdaBlueprintTemplateData freeMarkerDataModel = dataModel.collectLambdaBlueprintTemplateData();

        if (template == null || freeMarkerDataModel == null) {
            return;
        }

        handlerTypeDescriptionLink.setText(dataModel.getSelectedBlueprint().getDescription());
        String source;
        try {
            source = CodeTemplateManager.processTemplateWithData(template, freeMarkerDataModel);
        } catch (TemplateException | IOException e) {
            throw new RuntimeException("Failed to generate handler source preview", e);
        }

        if (sourcePreviewDocument != null) {
            sourcePreviewDocument.set(source);
        }
        if (sourcePreview != null) {
            sourcePreview.getTextWidget().setRedraw(true);
        }
    }

    public void initialize() {
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
