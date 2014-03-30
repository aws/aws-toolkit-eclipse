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
import java.util.Iterator;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonStreamContext;
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
import com.amazonaws.eclipse.cloudformation.templates.TemplateNamedObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;

public class TemplateReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    private IDocument document;
    private IProgressMonitor monitor;
    private final ISourceViewer sourceViewer;


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

    private JsonToken nextToken(JsonParser parser) throws IOException {
        lastLocation = parser.getCurrentLocation();
        JsonToken token = parser.nextToken();
        currentLocation = parser.getCurrentLocation();
        return token;
    }
    
    private JsonLocation getParserLastLocation(JsonParser parser) {
        if (currentLocation != parser.getCurrentLocation()) {
            getParserCurrentLocation(parser);
        }
        
        return lastLocation;
    }
    
    private JsonLocation getParserCurrentLocation(JsonParser parser) {
        JsonLocation oldLocation = currentLocation;
        currentLocation = parser.getCurrentLocation();
        
        if (oldLocation != currentLocation) lastLocation = oldLocation;
        
        return currentLocation;
    }
    
    private TemplateObjectNode parseObject(JsonParser parser) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_OBJECT) throw new IllegalArgumentException("Current token not an object start token when attempting to parse an object: " + token);

        // TODO: find a better way of identifying the root level node of parameters, mappings, resources, etc.
        boolean createNamedObject = isParsingNamedObject(parser);

        JsonLocation startLocation = getParserCurrentLocation(parser);
        TemplateObjectNode object = (createNamedObject ? new TemplateNamedObjectNode(startLocation)
                : new TemplateObjectNode(startLocation));

        do {
            token = nextToken(parser);
            if (token == JsonToken.END_OBJECT) break;
            
            if (token != JsonToken.FIELD_NAME) throw new RuntimeException("Unexpected token: " + token);
            String currentField = parser.getText();

            token = nextToken(parser);
            if (token == JsonToken.START_OBJECT) {
                TemplateObjectNode parsedObject = parseObject(parser);
                object.put(currentField, parsedObject);
                if (parsedObject instanceof TemplateNamedObjectNode) {
                    TemplateNamedObjectNode namedObject = TemplateNamedObjectNode.class.cast(parsedObject);
                    namedObject.setName(currentField);
                }
            }
            if (token == JsonToken.START_ARRAY)  object.put(currentField, parseArray(parser));
            if (token == JsonToken.VALUE_STRING) object.put(currentField, parseValue(parser));
        } while (true);

        if (token != JsonToken.END_OBJECT) throw new RuntimeException("Current token not an object end token: " + token);
        object.setEndLocation(getParserCurrentLocation(parser));

        return object;
    }

    /*
     * Hack to check the depth from the root
     */
    private boolean isParsingNamedObject(JsonParser parser) {
        JsonStreamContext greatGrandParent = greatGrandParent(parser);
        return greatGrandParent != null && greatGrandParent.inRoot();
    }

    private JsonStreamContext greatGrandParent(JsonParser parser) {
        JsonStreamContext parent = parser.getParsingContext().getParent();
        JsonStreamContext grandParent = parentIfNotNull(parent);
        JsonStreamContext greatGrandParent = parentIfNotNull(grandParent);
        return greatGrandParent;
    }

    private JsonStreamContext parentIfNotNull(JsonStreamContext parent) {
        return parent == null ? null : parent.getParent();
    }

    private TemplateValueNode parseValue(JsonParser parser) throws IOException, JsonParseException {
        TemplateValueNode node = new TemplateValueNode(parser.getText());
        node.setStartLocation(getParserLastLocation(parser));
        node.setEndLocation(getParserCurrentLocation(parser));
        
        return node;
    }
    
    private void prettyPrint(TemplateNode node) {
        if (node.isObject()) {
            for (Entry<String, TemplateNode> entry : ((TemplateObjectNode)node).getFields()) {
                prettyPrint(entry.getValue());
            }
        } else if (node.isArray()) {
            TemplateArrayNode array = (TemplateArrayNode)node;
            for (TemplateNode member : array.getMembers()) {
                prettyPrint(member);
            }
        } else if (node.isValue()) {
            TemplateValueNode value = (TemplateValueNode)node;
            System.out.println(value.getText() + 
                " (" + value.getStartLocation().getCharOffset() +
                " - " + value.getEndLocation().getCharOffset() + ")");
        }
    }
    
    private TemplateArrayNode parseArray(JsonParser parser) throws IOException {
        JsonToken token = parser.getCurrentToken();
        if (token != JsonToken.START_ARRAY) throw new IllegalArgumentException("Current token not an array start token when attempting to parse an array: " + token);
        
        TemplateArrayNode array = new TemplateArrayNode(getParserCurrentLocation(parser));
        
        do {
            token = nextToken(parser);
            if (token == JsonToken.END_ARRAY) break;
            
            if (token == JsonToken.START_OBJECT) array.add(parseObject(parser));
            if (token == JsonToken.START_ARRAY)  array.add(parseArray(parser));
            if (token == JsonToken.VALUE_STRING) array.add(parseValue(parser));
        } while (true);

        if (token != JsonToken.END_ARRAY) throw new RuntimeException("Current token not an array end token: " + token);
        array.setEndLocation(getParserCurrentLocation(parser));
        
        return array;
    }
    
    private void reconcile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonParser parser = mapper.getJsonFactory().createJsonParser(document.get());
            // TODO: Have parseObject take care of this? 
            nextToken(parser);
            TemplateObjectNode model = parseObject(parser);

            TemplateDocument document = (TemplateDocument) this.document;
            document.setModel(model);

            removeAllAnnotations();
        } catch (JsonParseException e) {
            e.printStackTrace();

            IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
            if (annotationModel != null) {
                Annotation annotation = new Annotation("org.eclipse.ui.workbench.texteditor.error", true, e.getMessage());
                annotationModel.addAnnotation(annotation, new Position((int)e.getLocation().getCharOffset(), 10));
            } else {
                throw new RuntimeException("No AnnotationModel configured");
            }
        } catch (Exception e) {
            // TODO: Add a status annotation for this
            e.printStackTrace();
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