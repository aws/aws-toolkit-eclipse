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

import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;


public final class TemplateSourceViewerConfiguration extends SourceViewerConfiguration {

    private final TemplateScanner templateScanner;

    public TemplateSourceViewerConfiguration() {
        this(CloudFormationPlugin.getDefault().getPreferenceStore());
    }

    public TemplateSourceViewerConfiguration(IPreferenceStore store) {
        templateScanner = new TemplateScanner(store);
    }

    public void handlePropertyChange(PropertyChangeEvent event) {
        templateScanner.resetTokens();
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

        PresentationReconciler reconciler = new PresentationReconciler();
        DefaultDamagerRepairer damagerRepairer = new DefaultDamagerRepairer(templateScanner);

        reconciler.setDamager(damagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(damagerRepairer, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

        return reconciler;
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        TemplateReconcilingStrategy reconcilingStrategy = new TemplateReconcilingStrategy(sourceViewer);
        MonoReconciler reconciler = new MonoReconciler(reconcilingStrategy, false);
        reconciler.install(sourceViewer);
        return reconciler;
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new IAnnotationHover() {
            @Override
            public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
                IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
                Iterator<?> iterator = annotationModel.getAnnotationIterator();

                try {
                    IRegion lineInfo = sourceViewer.getDocument().getLineInformation(lineNumber);

                    while (iterator.hasNext()) {
                        Annotation annotation = (Annotation)iterator.next();
                        Position position = annotationModel.getPosition(annotation);
                        if (position.offset >= lineInfo.getOffset() &&
                            position.offset <= (lineInfo.getOffset() + lineInfo.getLength())) {
                            return annotation.getText();
                        }
                    }
                } catch (BadLocationException e) { }

                return null;
            }
        };
    }

    // Overriding to turn on text wrapping
    @Override
    public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, SWT.WRAP, null);
            }
        };
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        final ContentAssistant assistant = new ContentAssistant();

        assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        assistant.setContentAssistProcessor(new TemplateContentAssistProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
        assistant.setAutoActivationDelay(0);
        assistant.enableAutoActivation(true);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        assistant.enablePrefixCompletion(true);
        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
        assistant.addCompletionListener(new ICompletionListener() {
            @Override
            public void assistSessionStarted(ContentAssistEvent event) {
                assistant.showContextInformation();
            }

            @Override
            public void assistSessionEnded(ContentAssistEvent event) {}

            @Override
            public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {}
        });

        assistant.setProposalSelectorBackground(
            Display.getDefault().
            getSystemColor(SWT.COLOR_WHITE));

        return assistant;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
     */
    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return new IAutoEditStrategy[] { new TemplateAutoEditStrategy() };
    }
}