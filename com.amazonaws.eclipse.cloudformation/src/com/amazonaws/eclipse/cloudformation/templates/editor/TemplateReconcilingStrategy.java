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

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
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

import com.amazonaws.eclipse.cloudformation.templates.TemplateArrayNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    private IDocument document;
    private IProgressMonitor monitor;
    private final ISourceViewer sourceViewer;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonFactory factory = mapper.getJsonFactory();
    private Stack<String> path = new Stack<String>();
    private static final String ROOT_SCHEMA_OBJECT = "ROOT";

    public TemplateReconcilingStrategy(ISourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void setDocument(IDocument document) {
        this.document = document;
    }

    public void reconcile(IRegion partition) {
        reconcile();
    }

    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile();
    }

    private JsonLocation lastLocation;
    private JsonLocation currentLocation;

    /**
     * Fetches the next token from the JsonParser. Also captures the location in
     * the stream before and after fetching the token.
     *
     * @param parser
     *            The JsonParser objects from which the token has to be fetched.
     * @return The JsonToken
     */
    private JsonToken nextToken(JsonParser parser) throws IOException {
        lastLocation = parser.getCurrentLocation();
        JsonToken token = parser.nextToken();
        currentLocation = parser.getCurrentLocation();
        return token;
    }

    /**
     * Returns the last location of the JsonParser.
     */
    private JsonLocation getParserLastLocation(JsonParser parser) {
        if (currentLocation != parser.getCurrentLocation()) {
            getParserCurrentLocation(parser);
        }

        return lastLocation;
    }

    /**
     * Returns the current location of the JsonParser.
     */
    private JsonLocation getParserCurrentLocation(JsonParser parser) {
        JsonLocation oldLocation = currentLocation;
        currentLocation = parser.getCurrentLocation();

        if (oldLocation != currentLocation) lastLocation = oldLocation;

        return currentLocation;
    }

    /**
     * Retrieves a JsonObject from the parser and creates a corresponding
     * TemplateObjectNode. If the Json Object has an array or a child Json
     * object, they are also parsed and associated with the TemplateObjectNode.
     *
     * @param parser
     *            The JsonParser from where the object has to be fetched.
     * @return A TemplateObjectNode of the corresponding JsonObject.
     */
    private TemplateObjectNode parseObject(JsonParser parser)
            throws IOException {

        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_OBJECT)
            throw new IllegalArgumentException(
                    "Current token not an object start token when attempting to parse an object: "
                            + token);

        TemplateObjectNode object = new TemplateObjectNode(
                getParserCurrentLocation(parser));

        do {
            token = nextToken(parser);
            if (token == JsonToken.END_OBJECT)
                break;
            if (token != JsonToken.FIELD_NAME)
                throw new RuntimeException("Unexpected token: " + token);

            String currentField = parser.getText();
            push(currentField);
            token = nextToken(parser);

            if (token == JsonToken.START_OBJECT)
                object.put(currentField, parseObject(parser));
            if (token == JsonToken.START_ARRAY)
                object.put(currentField, parseArray(parser));
            if (token == JsonToken.VALUE_STRING)
                object.put(currentField, parseValue(parser));

            pop();
        } while (true);

        if (token != JsonToken.END_OBJECT)
            throw new RuntimeException(
                    "Current token not an object end token: " + token);
        object.setEndLocation(getParserCurrentLocation(parser));

        return object;
    }

    /**
     * Parses a given value string from the parser.
     *
     * @param parser
     *            The parser from where the value node has to be read.
     * @return A TemplateValueNode object for the parsed value string.
     */
    private TemplateValueNode parseValue(JsonParser parser) throws IOException,
            JsonParseException {
        TemplateValueNode node = new TemplateValueNode(parser.getText());
        node.setStartLocation(getParserLastLocation(parser));
        node.setEndLocation(getParserCurrentLocation(parser));

        return node;
    }

    /**
     * Parses a given array string from the parser.
     *
     * @param parser
     *            The parser from where the array has to be read.
     * @return A TemplateArrayNode object for the parsed array.
     */
    private TemplateArrayNode parseArray(JsonParser parser) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY)
            throw new IllegalArgumentException(
                    "Current token not an array start token when attempting to parse an array: "
                            + token);

        TemplateArrayNode array = new TemplateArrayNode(
                getParserCurrentLocation(parser));

        do {
            token = nextToken(parser);
            if (token == JsonToken.END_ARRAY)
                break;

            if (token == JsonToken.START_OBJECT)
                array.add(parseObject(parser));
            if (token == JsonToken.START_ARRAY)
                array.add(parseArray(parser));
            if (token == JsonToken.VALUE_STRING)
                array.add(parseValue(parser));
        } while (true);

        if (token != JsonToken.END_ARRAY)
            throw new RuntimeException("Current token not an array end token: "
                    + token);
        array.setEndLocation(getParserCurrentLocation(parser));
        return array;
    }

    /**
     * Parses the Json Object from the given JsonParser and returns the root node.
     *
     * @param parser The parser from where the array has to be read.
     * @return The root node of the Json Object.
     */
    private TemplateObjectNode parse(JsonParser parser) throws IOException {
        nextToken(parser);
        push(ROOT_SCHEMA_OBJECT);
        TemplateObjectNode rootNode = parseObject(parser);
        pop();
        return rootNode;
    }

    /**
     * Pushes the given token to the stack.
     * @param token
     */
    private void push(String token){
        path.push(token);
    }

    /**
     * Pops a token from the stack.
     * @return
     */
    private String pop(){
        return path.pop();
    }

    /**
     * Reconciles the Json document extracted from the Json Editor.
     */
    private void reconcile() {
        TemplateDocument templateDocument = (TemplateDocument) this.document;

        try {
            path.clear();
            JsonParser parser = factory.createJsonParser(document.get());

            TemplateObjectNode model = parse(parser);

            templateDocument.setModel(model);

            removeAllAnnotations();
        } catch (JsonParseException e) {
            templateDocument.setPath(Collections.unmodifiableList(path));
            IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
            if (annotationModel != null) {
                Annotation annotation = new Annotation("org.eclipse.ui.workbench.texteditor.error", true, e.getMessage());
                annotationModel.addAnnotation(annotation, new Position((int)e.getLocation().getCharOffset(), 10));
            } else {
                throw new RuntimeException("No AnnotationModel configured");
            }
        } catch (Exception e) {
            // TODO: Add a status annotation for this
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

    public void setProgressMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void initialReconcile() {
        reconcile();
    }
}