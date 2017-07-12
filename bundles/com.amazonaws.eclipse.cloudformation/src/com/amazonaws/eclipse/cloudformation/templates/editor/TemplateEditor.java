/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.preferences.TemplateTokenPreferenceNames;

public class TemplateEditor extends TextEditor {

    //TODO put to preference page.
    public final static String EDITOR_MATCHING_BRACKETS = "com.amazonaws.cloudformation.matchingBrackets";
    public final static String EDITOR_MATCHING_BRACKETS_COLOR= "com.amazonaws.cloudformation.matchingBracketsColor";

    private final class TemplateDocumentProvider extends FileDocumentProvider {
        @Override
        protected IDocument createEmptyDocument() {
            return new TemplateDocument();
        }
    }

    private IDocumentProvider createDocumentProvider(IEditorInput input) {
        if (input instanceof IFileEditorInput) {
            return new TemplateDocumentProvider();
        } else if (input instanceof FileStoreEditorInput) {
            String message = "Unable to open templates from outside the workspace.  Copy it into a workspace project first.";
            CloudFormationPlugin.getDefault().reportException(message, null);
            throw new RuntimeException(message);
        } else {
            return new TemplateDocumentProvider();
        }
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        setDocumentProvider(createDocumentProvider(input));
        super.doSetInput(input);
    }

    @Override
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        super.configureSourceViewerDecorationSupport(support);
        char[] matchChars = {'(', ')', '[', ']', '{', '}'}; //which brackets to match

        ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(matchChars ,
                IDocumentExtension3.DEFAULT_PARTITIONING);
        support.setCharacterPairMatcher(matcher);
        support.setMatchingCharacterPainterPreferenceKeys(
                EDITOR_MATCHING_BRACKETS,
                EDITOR_MATCHING_BRACKETS_COLOR);

        //Enable bracket highlighting in the preference store
        IPreferenceStore store = getPreferenceStore();
        store.setDefault(EDITOR_MATCHING_BRACKETS, true);
        store.setDefault(EDITOR_MATCHING_BRACKETS_COLOR, "128,128,128");
    }

    @Override
    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
        try {
            ISourceViewer sourceViewer = getSourceViewer();
            if (sourceViewer == null) {
                return;
            }

            ((TemplateSourceViewerConfiguration) getSourceViewerConfiguration()).handlePropertyChange(event);

        } finally {
            super.handlePreferenceStoreChanged(event);
        }
    }

    private TemplateContentOutlinePage myOutlinePage;

    public TemplateEditor() {
        setPreferenceStore(CloudFormationPlugin.getDefault().getPreferenceStore());
        setSourceViewerConfiguration(new TemplateSourceViewerConfiguration());
    }

    public TemplateDocument getTemplateDocument() {
        return (TemplateDocument)this.getDocumentProvider().getDocument(getEditorInput());
    }

    /**
     * Returns true if the property change affects text presentation, so that the viewer could invalidate immediately.
     */
    @Override
    protected boolean affectsTextPresentation(PropertyChangeEvent event) {
        return TemplateTokenPreferenceNames.isCloudFormationEditorProperty(event.getProperty());
    }

    @Override
    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
           if (myOutlinePage == null) {
               TemplateDocument document = (TemplateDocument)this.getDocumentProvider().getDocument(getEditorInput());
               if (document != null) {
                   myOutlinePage = new TemplateContentOutlinePage(document);
               }
           }
           return myOutlinePage;
        }
        return super.getAdapter(required);
     }

}
