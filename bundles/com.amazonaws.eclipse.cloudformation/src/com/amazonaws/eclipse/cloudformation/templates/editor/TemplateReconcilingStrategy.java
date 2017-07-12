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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNodeParser;
import com.fasterxml.jackson.core.JsonParseException;

public class TemplateReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    private IDocument document;
    private IProgressMonitor monitor;
    private final ISourceViewer sourceViewer;

    public TemplateReconcilingStrategy(ISourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    @Override
    public void setDocument(IDocument document) {
        this.document = document;
    }

    @Override
    public void reconcile(IRegion partition) {
        reconcile();
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile();
    }

    /**
     * Reconciles the Json document extracted from the Json Editor.
     */
    private void reconcile() {
        TemplateDocument templateDocument = (TemplateDocument) this.document;
        TemplateNodeParser parser = new TemplateNodeParser();
        parser.parse(templateDocument);
        if (parser.getJsonParseException() != null) {
            Exception e = parser.getJsonParseException();
            if (e instanceof JsonParseException) {
                JsonParseException jpe = (JsonParseException) e;
                IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
                if (annotationModel != null) {
                    Annotation annotation = new Annotation("org.eclipse.ui.workbench.texteditor.error", true, jpe.getMessage());
                    annotationModel.addAnnotation(annotation, new Position((int)jpe.getLocation().getCharOffset(), 10));
                } else {
                    throw new RuntimeException("No AnnotationModel configured");
                }
            }
        } else {
            removeAllAnnotations();
        }

        if (monitor != null) monitor.done();
    }

    /** Clears all annotations from the annotation model. */
    private void removeAllAnnotations() {
        IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
        if (annotationModel == null) return;

        Iterator<?> annotationIterator = annotationModel.getAnnotationIterator();
        while (annotationIterator.hasNext()) {
            Annotation annotation = (Annotation)annotationIterator.next();
            annotationModel.removeAnnotation(annotation);
        }
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void initialReconcile() {
        reconcile();
    }
}