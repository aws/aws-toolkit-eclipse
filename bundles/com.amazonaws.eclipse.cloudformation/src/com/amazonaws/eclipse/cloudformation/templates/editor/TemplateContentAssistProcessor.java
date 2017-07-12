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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import com.amazonaws.eclipse.cloudformation.templates.TemplateNodeParser;
import com.amazonaws.eclipse.cloudformation.templates.TemplateNodePath.PathNode;
import com.amazonaws.eclipse.cloudformation.templates.TemplateObjectNode;
import com.amazonaws.eclipse.cloudformation.templates.schema.v2.AllowedValue;
import com.amazonaws.eclipse.cloudformation.templates.schema.v2.ElementType;
import com.amazonaws.eclipse.cloudformation.templates.schema.v2.TemplateElement;
import com.amazonaws.eclipse.cloudformation.templates.schema.v2.TemplateSchema;
import com.amazonaws.eclipse.cloudformation.templates.schema.v2.TemplateSchemaParser;

public class TemplateContentAssistProcessor implements IContentAssistProcessor {

    private static final TemplateSchema TEMPLATE_SCHEMA = TemplateSchemaParser.getDefaultSchema();

    private ICompletionProposal newFieldCompletionProposal(
            TemplateDocument templateDocument, int offset, String fieldName,
            TemplateElement schemaProperty, String stringToReplace) {

        String insertionText = fieldName + "\" : ";

        int finalCursorPosition = -1;
        ElementType elementType = ElementType.fromValue(schemaProperty.getType());
        if (elementType == null) {
            IStatus status = new Status(IStatus.INFO,
                    CloudFormationPlugin.PLUGIN_ID, "Unhandled property type: "
                            + schemaProperty.getType());
            StatusManager.getManager().handle(status, StatusManager.LOG);
        }

        insertionText += elementType.getInsertionText();
        finalCursorPosition = insertionText.length() + elementType.getCursorOffset();

        if (finalCursorPosition == -1)
            finalCursorPosition = insertionText.length();

        int replacementOffset = offset - stringToReplace.length();

        if ('"' == DocumentUtils.readToNextChar(templateDocument, offset)) {
            stringToReplace += '"';
        }

        CompletionProposal proposal = newCompletionProposal(replacementOffset,
                fieldName, insertionText, finalCursorPosition,
                schemaProperty.getDescription(), stringToReplace);

        return proposal;
    }

    /**
     * Creates a new completion proposal with the given parameters.
     *
     * @param offset
     *            The starting position for the string to be inserted.
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
    private CompletionProposal newCompletionProposal(int offset,
            String label, String insertionText, int finalCursorPosition,
            String description,String stringToReplace) {
        IContextInformation contextInfo = new ContextInformation(label, label);
        return new CompletionProposal(insertionText, offset, stringToReplace.length(),
                finalCursorPosition, null, label, contextInfo, description);
    }

    private void provideAttributeAutoCompletions(
            TemplateDocument document,
            int offset,
            TemplateElement element,
            TemplateNode node,
            List<ICompletionProposal> proposals) {

        String stringToReplace = DocumentUtils.readToPreviousQuote(document, offset);
        if (stringToReplace == null) {
            return;
        }
        if (element.getProperties() != null) {
            List<String> properties = new ArrayList<>(element.getProperties().keySet());
            Collections.sort(properties);

            Set<String> existingFields = new HashSet<>();
            if (node instanceof TemplateObjectNode) {
                TemplateObjectNode objectNode = (TemplateObjectNode) node;
                for (Entry<String, TemplateNode> entry : objectNode.getFields()) {
                    existingFields.add(entry.getKey());
                }
            }

            for (String property : properties) {
                if (existingFields.contains(property)) {
                    continue;
                }
                if (!property.toLowerCase().startsWith(stringToReplace.toLowerCase())) {
                    continue;
                }
                proposals.add(newFieldCompletionProposal(document, offset,
                        property, element.getProperties().get(property), stringToReplace));
            }
        }
    }

    private void provideAllowedValueAutoCompletions(
            TemplateDocument document,
            int offset,
            TemplateElement element,
            List<PathNode> subPaths,
            List<ICompletionProposal> proposals) {
        String stringToReplace = DocumentUtils.readToPreviousQuote(document, offset);
        if (stringToReplace == null) {
            return;
        }
        List<AllowedValue> allowedValues = null;
        if (ElementType.BOOLEAN == ElementType.fromValue(element.getType())) {
            allowedValues = AllowedValue.BOOLEAN_ALLOWED_VALUES;
        } else if (element.getAllowedValues() != null) {
            allowedValues = element.getAllowedValues();
        } else if (subPaths.size() > 2 && subPaths.get(subPaths.size() - 1).getFieldName().equals("Type")) {
            List<PathNode> path = subPaths.subList(0, subPaths.size() - 2);
            TemplateElement templateElement = TEMPLATE_SCHEMA.getTemplateElement(path);
            allowedValues = new ArrayList<>();
            if (templateElement.getChildSchemas() != null) {
                Set<String> keySet = templateElement.getChildSchemas().keySet();
                for (String key : keySet) {
                    allowedValues.add(new AllowedValue(
                            templateElement.getChildSchemas().get(key).getProperties().get("Type").getDescription(), key));
                }
            }
        }

        if (allowedValues != null && !allowedValues.isEmpty()) {
            for (AllowedValue allowedValue : allowedValues) {
                if (!allowedValue.getValue().toLowerCase().startsWith(stringToReplace.toLowerCase())) {
                    continue;
                }
                CompletionProposal proposal = newCompletionProposal(
                        offset - stringToReplace.length(), allowedValue.getValue(), allowedValue.getValue(),
                        allowedValue.getValue().length(), allowedValue.getDisplayLabel(), stringToReplace);
                proposals.add(proposal);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
     * computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        List<ICompletionProposal> proposals = new ArrayList<>();
        TemplateDocument document = (TemplateDocument) viewer.getDocument();
        new TemplateNodeParser().parse(document, offset);
        List<PathNode> subPaths = document.getSubPaths(offset);

        TemplateElement templateElement = TEMPLATE_SCHEMA.getTemplateElement(subPaths);
        TemplateNode templateNode = document.lookupNodeByPath(subPaths);


        // The prefix for the string to be completed
        String stringToReplace = DocumentUtils.readToPreviousQuote(document, offset);
        if (stringToReplace != null) {

            Character previousChar = DocumentUtils.readToPreviousChar(document, offset - stringToReplace.length() - 1);
            Character unmatchedOpenBrace = DocumentUtils.readToPreviousUnmatchedOpenBrace(document, offset - stringToReplace.length() - 1);

            if (previousChar == ':' || unmatchedOpenBrace == '[') {
                provideAllowedValueAutoCompletions(document, offset, templateElement, subPaths, proposals);
            } else if (unmatchedOpenBrace == '{') {
                provideAttributeAutoCompletions(document, offset, templateElement, templateNode, proposals);
            }
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '"' };
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        // TODO Auto-generated method stub
        return null;
    }
}