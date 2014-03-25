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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateValueNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateEditor.TemplateDocument;
import com.amazonaws.eclipse.cloudformation.templates.schema.IntrinsicFunction;
import com.amazonaws.eclipse.cloudformation.templates.schema.PseudoParameter;
import com.amazonaws.eclipse.cloudformation.templates.schema.Schema;
import com.amazonaws.eclipse.cloudformation.templates.schema.SchemaProperty;
import com.amazonaws.eclipse.cloudformation.templates.schema.TemplateSchemaRules;

public class TemplateContentAssistProcessor implements IContentAssistProcessor {

    private static final String[] INDENTATION = new String[] {"  ", "    ", "      ", "        ", "          ", "            ", "              "};
    private static final String RESOURCES = "Resources";
    private ITextViewer viewer;

    private TemplateNode lookupNodeByPath(IDocument document, String path) {
        TemplateNode node = ((TemplateDocument)document).getModel();

        if (path.startsWith("ROOT")) path = path.substring("ROOT".length());
        else throw new RuntimeException("Unexpected path encountered");

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (node.isObject()) {
                TemplateObjectNode object = (TemplateObjectNode)node;
                node = object.get(token);
            } else {
                throw new RuntimeException("Unexpected node structure");
            }
        }

        return node;
    }

    private void addFieldCompletionProposals(IDocument document, String path, int offset, List<ICompletionProposal> proposals) {
        TemplateSchemaRules schemaRules = TemplateSchemaRules.getInstance();

        String currentPath = "ROOT";

        if (path.startsWith("ROOT")) path = path.substring("ROOT".length());
        else throw new RuntimeException("Unexpected path encountered");

        // TODO: This is all code just to find the schema for the current position

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        Schema schema = schemaRules.getTopLevelSchema();
        SchemaProperty lastSchemaProperty = null;
        boolean isSchemaLookupProperty = false;
        boolean isDefaultChildSchema = false;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            currentPath += "/" + token;

            SchemaProperty schemaProperty = null;
            if (schema != null) {
                schemaProperty = schema.getProperty(token);
            }

            if (schemaProperty == null && isDefaultChildSchema == false && isSchemaLookupProperty == false) {
                // We don't know anything about this node
                return;
            }

            if (isSchemaLookupProperty) {
                String schemaLookupProperty = lastSchemaProperty.getSchemaLookupProperty();

                TemplateNode node = lookupNodeByPath(document, currentPath);
                if (node.isObject()) {
                    String lookupValue = null;

                    TemplateObjectNode object = (TemplateObjectNode)node;
                    TemplateNode fieldValue = object.get(schemaLookupProperty);
                    if (fieldValue.isValue()) {
                        TemplateValueNode value = (TemplateValueNode)fieldValue;
                        lookupValue = value.getText();

                        schema = lastSchemaProperty.getChildSchema(lookupValue);
                    }
                }
                isSchemaLookupProperty = false;
            } else if (isDefaultChildSchema) {
                schema = lastSchemaProperty.getDefaultChildSchema();
                isDefaultChildSchema = false;
            } else if (schemaProperty.getSchemaLookupProperty() != null) {
                isSchemaLookupProperty = true;
                schema = null;
            } else if (schemaProperty.getDefaultChildSchema() != null) {
                isDefaultChildSchema = true;
                schema = null;
            } else if (schemaProperty.getSchema() != null) {
                schema = schemaProperty.getSchema();
            } else {
                schema = null;
            }

            lastSchemaProperty = schemaProperty;
        }

        if (schema != null) {
            ArrayList<String> properties = new ArrayList<String>(schema.getProperties());
            Collections.sort(properties);

            Set<String> existingFields = new HashSet<String>();
            TemplateNode node = lookupNodeByPath(document, "ROOT" + path);
            if (node.isObject()) {
                TemplateObjectNode objectNode = (TemplateObjectNode)node;
                for (Entry<String, TemplateNode> entry : objectNode.getFields()) {
                    existingFields.add(entry.getKey());
                }
            }

            for (String field : properties) {
                // Don't show completions for fields already present in the document
                if (existingFields.contains(field)) continue;

                proposals.add(newFieldCompletionProposal(offset, field, schema.getProperty(field)));
            }
        }
    }

    private ICompletionProposal newFieldCompletionProposal(int offset, String fieldName, SchemaProperty schemaProperty) {
        TemplateDocument templateDocument = (TemplateDocument)viewer.getDocument();
        char previousChar = ' ';
        try {
            previousChar = templateDocument.getChar(offset - 1);
        } catch (BadLocationException e) {}
        boolean needsQuotes = previousChar != '"';

        String insertionText = fieldName;

        if (needsQuotes) insertionText = "\"" + fieldName;

        insertionText += "\" : ";

        int finalCursorPosition = -1;

        if (schemaProperty.getType().equalsIgnoreCase("string")) {
            insertionText += "\"\"";
            finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("array")) {
            insertionText += "[]";
            finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("named-array")) {
            insertionText += "{}";
            finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("number")) {
                insertionText += "\"\"";
                finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("object")) {
            insertionText += "{}";
            finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("resource")) {
            insertionText += "{}";
            finalCursorPosition = insertionText.length() - 1;
        } else if (schemaProperty.getType().equalsIgnoreCase("json")) {
            insertionText += "{}";
            finalCursorPosition = insertionText.length() - 1;
        } else {
            IStatus status = new Status(IStatus.INFO, CloudFormationPlugin.PLUGIN_ID, "Unhandled property type: " + schemaProperty.getType());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        char nextChar = DocumentUtils.readToNextChar(templateDocument, offset);
        if (nextChar != '}' && nextChar != ',' && nextChar != ']') insertionText += ", ";

        if (finalCursorPosition == -1) finalCursorPosition = insertionText.length();

        CFCompletionProposal proposal = newCompletionProposal(offset, fieldName, insertionText, finalCursorPosition, schemaProperty.getDescription());

        // if previousChar is a type of END_TOKEN
        previousChar = DocumentUtils.readToPreviousChar(templateDocument, offset - 1);
        if (previousChar == '"' || previousChar == '}' || previousChar == ']') {
            proposal.setAdditionalCommaPosition(DocumentUtils.findPreviousCharPosition(templateDocument, offset, previousChar) + 1);
        }
        return proposal;
    }

    private CFCompletionProposal newCompletionProposal(int offset, String text, String description) {
        return newCompletionProposal(offset, text, text, description);
    }

    private CFCompletionProposal newCompletionProposal(int offset, String label, String insertionText, String description) {
        return newCompletionProposal(offset, label, insertionText, insertionText.length(), description);
    }

    private CFCompletionProposal newCompletionProposal(int offset, String label, String insertionText, int finalCursorPosition, String description) {
        IContextInformation contextInfo = new ContextInformation(label, label);
        return new CFCompletionProposal(insertionText, offset, 0, finalCursorPosition, null, label, contextInfo, description);
    }

    private TemplateSchemaRules getTemplateSchema() {
        return TemplateSchemaRules.getInstance();
    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        if (this.viewer == null) this.viewer = viewer;

        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        TemplateDocument document = (TemplateDocument)viewer.getDocument();


        char previousChar = DocumentUtils.readToPreviousChar(document, offset - 1);
        char unmatchedOpenBrace = DocumentUtils.readToPreviousUnmatchedOpenBrace(document, offset - 1);

        if ( unmatchedOpenBrace == '{' ) {
            TemplateNode node = document.findNode(offset);
            if ( node.isObject() ) {
                TemplateObjectNode object = (TemplateObjectNode) node;
                if ( object.getPath().equals("ROOT/Resources/") ) {
                    addResourceTypeCompletionProposals(document, offset, proposals);
                } else {
                    addFieldCompletionProposals(document, node.getPath(), offset, proposals);
                }
            } else {
                addFieldCompletionProposals(document, node.getPath(), offset, proposals);
            }
        } else if ( unmatchedOpenBrace == '[' || previousChar == ':' ) {
            // TODO: Completions for known values for a field

            // TODO: Intrinsic Functions should include "{}"
            addIntrinsicFunctionCompletions(offset, proposals);
            addPseudoParameterCompletions(offset, proposals);
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    private void generateResourceStub(String resourceType, Schema schema, StringBuilder s, int indentationLevel, boolean needsTrailingComa) {
        // don't indent the very first line
        if (s.length() > 0) s.append(INDENTATION[indentationLevel]);

        String field = resourceType.replaceAll(":", "");
        s.append("\"").append(field).append("\"").append(" : {\n");

        Map<String, SchemaProperty> requiredProperties = new HashMap<String, SchemaProperty>();
        for (String propertyName : schema.getProperties()) {
            SchemaProperty property = schema.getProperty(propertyName);
            if (property.isRequired()) requiredProperties.put(propertyName, property);
        }

        int count = 0;
        for (Entry<String, SchemaProperty> entry : requiredProperties.entrySet()) {
            count++;
            String propertyName = entry.getKey();
            SchemaProperty property = entry.getValue();

            if (property.getSchema() != null) {
                generateResourceStub(propertyName, property.getSchema(), s, indentationLevel + 1, (count < requiredProperties.size()));
            } else {
                s.append(INDENTATION[indentationLevel + 1]).append("\"").append(propertyName).append("\" : ");

                if (propertyName.equals("Type")) {
                    s.append("\"" + resourceType + "\"");
                } else if (property.getType().equalsIgnoreCase("Array")) {
                    s.append("[]");
                } else {
                    s.append("\"\"");
                }

                // everytime but the last...
                if (count < requiredProperties.size()) s.append(", ");
                s.append(" \n");
            }
        }
        s.append(INDENTATION[indentationLevel]).append("}");
        if (needsTrailingComa) {
            s.append(", ");
        }
        s.append("\n");
    }

    private void addResourceTypeCompletionProposals(TemplateDocument document, int offset, List<ICompletionProposal> proposals) {
        ArrayList<String> resourceTypeNames = new ArrayList<String>(getTemplateSchema().getResourceTypeNames());
        Collections.sort(resourceTypeNames);

        for (String resourceType : resourceTypeNames) {
            SchemaProperty schemaProperty = getTemplateSchema().getTopLevelSchema().getProperty(RESOURCES);
            Schema schema = schemaProperty.getChildSchema(resourceType);

            boolean needsTrailingComa = true;
            char c = DocumentUtils.readToNextChar(document, offset);
            if (c == '}' || c == ',') needsTrailingComa = false;

            StringBuilder s = new StringBuilder();
            generateResourceStub(resourceType, schema, s, 1, needsTrailingComa);

            IContextInformation contextInfo = new ContextInformation(resourceType, resourceType);
            String insertionText = s.toString();
            int secondQuotePosition = insertionText.indexOf('"', insertionText.indexOf('"') + 1);

            proposals.add(new CompletionProposal(insertionText, offset, 0, secondQuotePosition, null, resourceType, contextInfo, schema.getDescription()));
        }
    }

    private void addPseudoParameterCompletions(int offset, List<ICompletionProposal> proposals) {
        TemplateSchemaRules schemaRules = getTemplateSchema();

        for (PseudoParameter parameter : schemaRules.getPseudoParameters()) {
            proposals.add(newCompletionProposal(offset, parameter.getName(), parameter.getDescription()));
        }
    }

    private void addIntrinsicFunctionCompletions(int offset, List<ICompletionProposal> proposals) {
        TemplateSchemaRules schemaRules = getTemplateSchema();

        for (IntrinsicFunction function : schemaRules.getIntrinsicFunctions()) {
            if (function.getName().equals("Ref")) {
                // text, offset, 0, text.length(), null, text, contextInfo, description)
                String refLiteral = "{ \"Ref\" : \"\" }";
                String additionalProposalInfo = "Reference to a resource";
                IContextInformation contextInfo = new ContextInformation("Ref", additionalProposalInfo);
                proposals.add(new CompletionProposal(refLiteral, offset, 0, 11, null, "Ref",
                        contextInfo, additionalProposalInfo));
            } else {
                proposals.add(newCompletionProposal(offset, function.getName(), function.getDescription()));
            }
        }
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {'"'};
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    public char[] getContextInformationAutoActivationCharacters() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        // TODO Auto-generated method stub
        return null;
    }
}