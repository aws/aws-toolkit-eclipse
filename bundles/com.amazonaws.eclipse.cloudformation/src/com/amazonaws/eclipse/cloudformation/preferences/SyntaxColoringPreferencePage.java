/*
 * Copyright 2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.cloudformation.preferences;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCheckbox;

import java.io.IOException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.preferences.EditorPreferences.Color;
import com.amazonaws.eclipse.cloudformation.preferences.EditorPreferences.TokenPreference;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateDocument;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateSourceViewerConfiguration;
import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.util.IOUtils;

/**
 *  Preference page for setting CloudFormation template editor highlighting.
 */
public class SyntaxColoringPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {

    private static final String GLOBAL_FONT_PROPERTY_NAME = "org.eclipse.jdt.ui.editors.textfont";
    private final EditorPreferences model = new EditorPreferences();
    // PreferenceStore to be used for the preview viewer.
    private final IPreferenceStore overlayPreferenceStore = new PreferenceStore();

    // UIs in this preference page
    private List tokenList;
    private ColorSelector colorSelector;
    private Button isBoldButton;
    private Button isItalicButton;
    private JavaSourceViewer fPreviewViewer;

    public SyntaxColoringPreferencePage() {
        super("CloudFormation Template Syntax Coloring Preference Page");
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(CloudFormationPlugin.getDefault().getPreferenceStore());
        initModel();
        initOverlayPreferenceStore();
    }

    @Override
    protected Control createContents(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createTokenSelectionSection(composite);
        createStyleSelectionSection(composite);
        createPreviewer(composite);

        onTokenListSelected();

        return composite;
    }

    @Override
    protected void performDefaults() {
        EditorPreferences defaultPreferences = EditorPreferences.getDefaultPreferences();
        hardCopyToModel(defaultPreferences);
        onTokenListSelected();
        EditorPreferencesLoader.loadPreferences(overlayPreferenceStore, model);
        super.performDefaults();
    }

    @Override
    public void performApply() {
        onPerformApply();
        super.performApply();
    }

    @Override
    public boolean performOk() {
        onPerformApply();
        return super.performOk();
    }

    private void createTokenSelectionSection(Composite composite) {
        Composite tokenComposite = new Composite(composite, SWT.NONE);
        tokenComposite.setLayout(new GridLayout());
        tokenComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        new Label(tokenComposite, SWT.NONE).setText("Element:");

        tokenList = new List(tokenComposite, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

        for (TokenPreference tokenPreference : model.getHighlight().values()) {
            tokenList.add(tokenPreference.getDisplayLabel());
            tokenList.setData(tokenPreference.getDisplayLabel(), tokenPreference);
        }
        tokenList.select(0);

        tokenList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onTokenListSelected();
            }
        });
    }

    private void createStyleSelectionSection(Composite composite) {
        Composite styleComposite = new Composite(composite, SWT.NONE);
        styleComposite.setLayout(new GridLayout(2, false));
        styleComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        newLabel("Color: ", styleComposite);
        colorSelector = new ColorSelector(styleComposite);
        isBoldButton = newCheckbox(styleComposite, "Bold", 2);
        isItalicButton = newCheckbox(styleComposite, "Italic", 2);

        colorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                onColorSelectorChanged();
            }
        });

        isBoldButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onIsBoldButtonSelected();
            }
        });

        isItalicButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onIsItalicButtonSelected();
            }
        });
    }

    @SuppressWarnings("restriction")
    private void createPreviewer(Composite parent) {

        GridData data = new GridData();
        data.horizontalSpan = 2;
        newLabel("Preview:", parent).setLayoutData(data);

        IPreferenceStore store= new ChainedPreferenceStore(new IPreferenceStore[] { overlayPreferenceStore, JavaPlugin.getDefault().getCombinedPreferenceStore()});
        fPreviewViewer= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, store);
        TemplateSourceViewerConfiguration configuration = new TemplateSourceViewerConfiguration(store);
        fPreviewViewer.configure(configuration);
        Font font= JFaceResources.getFont(GLOBAL_FONT_PROPERTY_NAME);
        fPreviewViewer.getTextWidget().setFont(font);
        new SourcePreviewerUpdater(fPreviewViewer, configuration, store);
        fPreviewViewer.setEditable(false);
        data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        fPreviewViewer.getControl().setLayoutData(data);

        String content= loadPreviewContentFromFile();
        TemplateDocument document = new TemplateDocument();
        document.set(content);
        fPreviewViewer.setDocument(document);
    }

    private void initModel() {
        IPreferenceStore store = getPreferenceStore();
        model.setHighlight(EditorPreferences.buildHighlightPreferences(store));
    }

    private void initOverlayPreferenceStore() {
        IPreferenceStore store = getPreferenceStore();
        EditorPreferencesLoader.loadPreferences(store, overlayPreferenceStore);
    }

    private TokenPreference getSelectedToken() {
        String key = tokenList.getItems()[tokenList.getSelectionIndex()];
        return (TokenPreference) tokenList.getData(key);
    }

    private void onTokenListSelected() {
        TokenPreference tokenPreference = getSelectedToken();
        colorSelector.setColorValue(new RGB(
                tokenPreference.getColor().getRed(),
                tokenPreference.getColor().getGreen(),
                tokenPreference.getColor().getBlue()));
        isBoldButton.setSelection(tokenPreference.getBold());
        isItalicButton.setSelection(tokenPreference.getItalic());
    }

    private void onColorSelectorChanged() {
        RGB rgb = colorSelector.getColorValue();
        TokenPreference token = getSelectedToken();
        Color color = new Color(rgb.red, rgb.green, rgb.blue);
        token.setColor(color);
        EditorPreferencesLoader.loadTokenPreferences(overlayPreferenceStore, token);
    }

    private void onIsBoldButtonSelected() {
        TokenPreference token = getSelectedToken();
        token.setBold(isBoldButton.getSelection());
        EditorPreferencesLoader.loadTokenPreferences(overlayPreferenceStore, token);
    }

    private void onIsItalicButtonSelected() {
        TokenPreference token = getSelectedToken();
        token.setItalic(isItalicButton.getSelection());
        EditorPreferencesLoader.loadTokenPreferences(overlayPreferenceStore, token);
    }

    private void onPerformApply() {
        EditorPreferencesLoader.loadPreferences(getPreferenceStore(), model);
    }

    private void hardCopyToModel(EditorPreferences from) {
        for (TokenPreference tokenPreference : from.getHighlight().values()) {
            hardCopy(from.getHighlight().get(tokenPreference.getId()),
                    model.getHighlight().get(tokenPreference.getId()));
        }
    }

    private static void hardCopy(TokenPreference from, TokenPreference to) {
        to.setBold(from.getBold());
        to.setItalic(from.getItalic());
        to.setColor(new Color(
                from.getColor().getRed(), from.getColor().getGreen(), from.getColor().getBlue()));
    }

    private static class SourcePreviewerUpdater {

        /**
         * Creates a Java source preview updater for the given viewer, configuration and preference store.
         *
         * @param viewer the viewer
         * @param configuration the configuration
         * @param preferenceStore the preference store
         */
        SourcePreviewerUpdater(final SourceViewer viewer, final TemplateSourceViewerConfiguration configuration, final IPreferenceStore preferenceStore) {
            Assert.isNotNull(viewer);
            Assert.isNotNull(configuration);
            Assert.isNotNull(preferenceStore);
            final IPropertyChangeListener fontChangeListener= new IPropertyChangeListener() {
                /*
                 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
                 */
                public void propertyChange(PropertyChangeEvent event) {
                    if (event.getProperty().equals(GLOBAL_FONT_PROPERTY_NAME)) {
                        Font font= JFaceResources.getFont(GLOBAL_FONT_PROPERTY_NAME);
                        viewer.getTextWidget().setFont(font);
                    }
                }
            };
            final IPropertyChangeListener propertyChangeListener= new IPropertyChangeListener() {
                /*
                 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
                 */
                public void propertyChange(PropertyChangeEvent event) {
                    if (TemplateTokenPreferenceNames.isCloudFormationEditorProperty(event.getProperty())) {
                        configuration.handlePropertyChange(event);
                        viewer.invalidateTextPresentation();
                    }
                }
            };
            viewer.getTextWidget().addDisposeListener(new DisposeListener() {
                /*
                 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
                 */
                public void widgetDisposed(DisposeEvent e) {
                    preferenceStore.removePropertyChangeListener(propertyChangeListener);
                    JFaceResources.getFontRegistry().removeListener(fontChangeListener);
                }
            });
            JFaceResources.getFontRegistry().addListener(fontChangeListener);
            preferenceStore.addPropertyChangeListener(propertyChangeListener);
        }
    }

    private String loadPreviewContentFromFile() {
        String previewDocumentFile = "preview-document-content.json";

        String content =
                "{\n" +
                "  \"AWSTemplateFormatVersion\" : \"2010-09-09\",\n" +
                "  \"Resources\" : {\n" +
                "    \"S3Bucket\": {\n" +
                "      \"Type\" : \"AWS::S3::Bucket\",\n" +
                "      \"Properties\" : {\n" +
                "        \"BucketName\" : \"bucketname\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        try {
            return IOUtils.toString(SyntaxColoringPreferencePage.class.getResourceAsStream(previewDocumentFile));
        } catch (IOException e) {
            return content;
        }
    }

}
