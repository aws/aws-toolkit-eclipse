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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
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

    /** The parsed schema rules for the Amazon CloudFormation template. */
    private static final TemplateSchemaRules schemaRules = TemplateSchemaRules
            .getInstance();

    /** The path from the root to where the Resource JSON objects are present. */
    private static final String resourcesPath = "ROOT/Resources/";

    /**
     * The name of the field specifying the type of the Resource in the JSON
     * object.
     */
    private static final String RESOURCE_TYPE = "Type";

    /** The name of the Json Object specifying the list of resources. */
    private static final String RESOURCES = "Resources";

    /** The root object of the Json document. */
    private static final String ROOT = "ROOT/";

    private static final String EMPTY_STRING = "";

    /**
     * Identifies the node in the document for the given path and returns it.
     *
     * @param document
     *            The document associated with the editor.
     * @param path
     *            The path from the Root to the node.
     */
    private TemplateNode lookupNodeByPath(TemplateDocument document, String path) {
        TemplateNode node = ((TemplateDocument) document).getModel();

        if (path.startsWith("ROOT"))
            path = path.substring("ROOT".length());
        else
            throw new RuntimeException("Unexpected path encountered");

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (node != null && node.isObject()) {
                TemplateObjectNode object = (TemplateObjectNode) node;
                node = object.get(token);
            } else {
                throw new RuntimeException("Unexpected node structure");
            }
        }
        return node;
    }

    /**
     * Generates a proposal for the given field.
     *
     * @param templateDocument
     *            The document associated with the editor.
     * @param offset
     *            The current cursor position.
     * @param fieldName
     *            The name of the field.
     * @param schemaProperty
     *            The schema property associated with the field.
     */
    private ICompletionProposal newFieldCompletionProposal(
            TemplateDocument templateDocument, int offset, String fieldName,
            SchemaProperty schemaProperty, String stringToReplace) {
        char previousChar = ' ';
        try {
            previousChar = templateDocument.getChar(offset - 1);
        } catch (BadLocationException e) {
        }
        boolean needsQuotes = previousChar != '"';

        String insertionText = fieldName;

        if (needsQuotes){
            insertionText = "\"" + fieldName;
            stringToReplace = "\"" + stringToReplace;
        }

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
            IStatus status = new Status(IStatus.INFO,
                    CloudFormationPlugin.PLUGIN_ID, "Unhandled property type: "
                            + schemaProperty.getType());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        char nextChar = DocumentUtils.readToNextChar(templateDocument, offset);
        if (nextChar != '}' && nextChar != ',' && nextChar != ']')
            insertionText += ", ";

        if (finalCursorPosition == -1)
            finalCursorPosition = insertionText.length();

        CFCompletionProposal proposal = newCompletionProposal(offset,
                fieldName, insertionText, finalCursorPosition,
                schemaProperty.getDescription(),stringToReplace);

        // if previousChar is a type of END_TOKEN
        previousChar = DocumentUtils.readToPreviousChar(templateDocument,
                offset - 1);
        if (previousChar == '"' || previousChar == '}' || previousChar == ']') {
            proposal.setAdditionalCommaPosition(DocumentUtils
                    .findPreviousCharPosition(templateDocument, offset,
                            previousChar) + 1);
        }
        return proposal;
    }

    /**
     * Creates a new completion proposal with the given parameters.
     *
     * @param offset
     *            The current cursor position.
     * @param label
     *            The label to displayed in the the proposal.
     * @param insertionText
     *            The text to be inserted when a proposal is selected.
     * @param finalCursorPosition
     *            The final cursor position after inserting the proposal.
     * @param description
     *            The description for the proposal to be displayed in the tool
     *            tip.
     * @param stringToReplace The string to be replaced when a proposal is selected.
     */
    private CFCompletionProposal newCompletionProposal(int offset,
            String label, String insertionText, int finalCursorPosition,
            String description,String stringToReplace) {
        IContextInformation contextInfo = new ContextInformation(label, label);
        return new CFCompletionProposal(insertionText, offset-stringToReplace.length(), stringToReplace.length(),
                finalCursorPosition, null, label, contextInfo, description);
    }

    /**
     * Provides the auto completions for all the root level objects.
     *
     * @param offset
     *            The current cursor position
     * @param document
     *            The document associated with the editor.
     * @param path
     *            Path from the root of the JSON object to the current cursor
     *            position.
     * @param proposals
     *            List containing the proposals.
     */
    private void provideRootLevelAutoCompletions(int offset,
            TemplateDocument document, String path,
            List<ICompletionProposal> proposals) {
        if (!(path.startsWith(ROOT))) {
            throw new RuntimeException("Unexpected path encountered");
        }

        TemplateSchemaRules schemaRules = TemplateSchemaRules.getInstance();

        String currentPath = "ROOT";

        if (path.startsWith("ROOT"))
            path = path.substring("ROOT".length());
        else
            throw new RuntimeException("Unexpected path encountered");

        // TODO: This is all code just to find the schema for the current
        // position

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        String stringToReplace = DocumentUtils.readToPreviousQuote(document, offset);

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

            if (schemaProperty == null && isDefaultChildSchema == false
                    && isSchemaLookupProperty == false) {
                // We don't know anything about this node
                return;
            }

            if (isSchemaLookupProperty) {
                String schemaLookupProperty = lastSchemaProperty
                        .getSchemaLookupProperty();

                TemplateNode node = lookupNodeByPath(document, currentPath);
                if (node.isObject()) {
                    String lookupValue = null;

                    TemplateObjectNode object = (TemplateObjectNode) node;
                    TemplateNode fieldValue = object.get(schemaLookupProperty);
                    if (fieldValue.isValue()) {
                        TemplateValueNode value = (TemplateValueNode) fieldValue;
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
            ArrayList<String> properties = new ArrayList<String>(
                    schema.getProperties());
            Collections.sort(properties);

            Set<String> existingFields = new HashSet<String>();
            TemplateNode node = lookupNodeByPath(document, "ROOT" + path);
            if (node.isObject()) {
                TemplateObjectNode objectNode = (TemplateObjectNode) node;
                for (Entry<String, TemplateNode> entry : objectNode.getFields()) {
                    existingFields.add(entry.getKey());
                }
            }

            for (String field : properties) {
                // Don't show completions for fields already present in the
                // document
                if (existingFields.contains(field))
                    continue;

                proposals.add(newFieldCompletionProposal(document, offset,
                        field, schema.getProperty(field),stringToReplace));
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
     * computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int offset) {

        List<ICompletionProposal> proposals = new ArrayList<ICompletionProposal>();
        TemplateDocument document = (TemplateDocument) viewer.getDocument();

        StringBuilder path = new StringBuilder();
        List<String> subPaths = document.getPath();
        if (subPaths != null && !subPaths.isEmpty()) {
            for (String subPath : subPaths) {
                path.append(subPath + "/");
            }
        } else {
            TemplateNode node = document.findNode(offset);
            path.append(node.getPath());
        }

        char previousChar = DocumentUtils.readToPreviousChar(document,
                offset - 1);
        char unmatchedOpenBrace = DocumentUtils
                .readToPreviousUnmatchedOpenBrace(document, offset - 1);

        if (unmatchedOpenBrace == '{') {
            if (path.toString().startsWith(resourcesPath)) {
                provideResourceTypes(offset, document, path.toString(),
                        proposals);
            } else if (path.toString().startsWith(ROOT)) {
                provideRootLevelAutoCompletions(offset, document,
                        path.toString(), proposals);
            }
        } else if (unmatchedOpenBrace == '[' || previousChar == ':') {
            // TODO: Completions for known values for a field

            // TODO: Intrinsic Functions should include "{}"
            addIntrinsicFunctionCompletions(offset, proposals);
            addPseudoParameterCompletions(offset, proposals);
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    /**
     * Creates a new auto completion proposal for the given field.
     *
     * @param offset
     *            The current position of the cursor.
     * @param autoCompleteField
     *            The value that is to be shown in the auto complete drop box.
     * @param insertionText
     *            The actual value to be inserted when an option is selected.
     * @param fieldDescription
     *            The description for the auto complete option to be shown in
     *            tool tip.
     * @param stringToReplace
     *            The string to replace in the editor.
     * @return A completion proposal with all the options added.
     */
    private ICompletionProposal createCompletionProposal(int offset,
            String autoCompleteField, String insertionText,
            String fieldDescription, String stringToReplace) {

        return new CFCompletionProposal(insertionText, offset
                - stringToReplace.length(), stringToReplace.length(),
                insertionText.length(), null, autoCompleteField,
                new ContextInformation(autoCompleteField, fieldDescription),
                fieldDescription);
    }

    /**
     * Identifies the set of resources to be shown for auto completion.
     *
     * @param offset
     *            The current cursor position.
     * @param document
     *            The document associated with the editor.
     * @param path
     *            Path from the root of the JSON object to the current cursor
     *            position.
     * @param proposals
     *            List of auto completion proposals.
     */
    private void provideResourceTypes(int offset, TemplateDocument document,
            String path, List<ICompletionProposal> proposals) {

        if (!(path.startsWith(resourcesPath))) {
            throw new RuntimeException("Unexpected path encountered");
        }

        String stringToReplace = DocumentUtils.readToPreviousQuote(document,
                offset);
        char nextChar = DocumentUtils.readToNextChar(document, offset);

        boolean needsQuotes = nextChar != '"';

        path = path.substring(resourcesPath.length());

        Set<String> resourceTypes = schemaRules.getResourceTypeNames();

        StringTokenizer tokenizer = new StringTokenizer(path, "/");

        String nextToken;
        if (tokenizer.countTokens() == 2) {
            // Skipping the resource name
            tokenizer.nextToken();
            // Fetching the next Token.
            nextToken = tokenizer.nextToken();

            if (nextToken.equals(RESOURCE_TYPE)) {
                addListToProposals(offset,
                        new ArrayList<String>(resourceTypes), proposals,
                        stringToReplace, needsQuotes, null);
            }
        }
    }

    /**
     * Creates a completion proposal for the given set and adds it to the list.
     *
     * @param offset
     *            The current cursor position.
     * @param values
     *            The values for which proposals are to be created.
     * @param proposals
     *            The list that contains the proposals.
     * @param stringToReplace
     *            The string to be replaced when a proposal is selected.
     * @param needsQuotes
     *            A boolean value indicating if a double quote needs to be added
     *            while insertion.
     */
    private void addListToProposals(int offset, List<String> values,
            List<ICompletionProposal> proposals, String stringToReplace,
            boolean needsQuotes, List<String> existingFields) {
        Collections.sort(values);
        SchemaProperty schemaProperty = schemaRules.getTopLevelSchema()
                .getProperty(RESOURCES);
        Schema childSchema;
        String insertionText;
        for (String value : values) {
            if (value.startsWith(stringToReplace)) {
                insertionText = value;
                childSchema = schemaProperty.getChildSchema(value);
                if (needsQuotes)
                    insertionText = value + "\"";
                if (existingFields != null && existingFields.contains(value))
                    continue;
                proposals.add(createCompletionProposal(offset, value,
                        insertionText, childSchema.getDescription(),
                        stringToReplace));
            }
        }
    }

    /**
     * Adds the pseudo parameters from the cloud formation schema to the list of
     * proposals.
     *
     * @param offset
     *            The current cursor position.
     * @param proposals
     *            List containing the proposals.
     */
    private void addPseudoParameterCompletions(int offset,
            List<ICompletionProposal> proposals) {
        for (PseudoParameter parameter : schemaRules.getPseudoParameters()) {
            proposals.add(createCompletionProposal(offset, parameter.getName(),
                    parameter.getName(), parameter.getDescription(),
                    EMPTY_STRING));
        }
    }

    /**
     * Adds the intrinsic functions from the cloud formation schema to the list
     * of proposals.
     *
     * @param offset
     *            The current cursor position.
     * @param proposals
     *            List containing the proposals.
     */
    private void addIntrinsicFunctionCompletions(int offset,
            List<ICompletionProposal> proposals) {
        for (IntrinsicFunction function : schemaRules.getIntrinsicFuntions()) {
            if (function.getName().equals("Ref")) {
                // text, offset, 0, text.length(), null, text, contextInfo,
                // description)
                String refLiteral = "{ \"Ref\" : \"\" }";
                String additionalProposalInfo = "Reference to a resource";
                IContextInformation contextInfo = new ContextInformation("Ref",
                        additionalProposalInfo);
                proposals.add(new CompletionProposal(refLiteral, offset, 0, 11,
                        null, "Ref", contextInfo, additionalProposalInfo));
            } else {
                proposals.add(createCompletionProposal(offset,
                        function.getName(), function.getName(),
                        function.getDescription(), EMPTY_STRING));
            }
        }
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '"' };
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
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