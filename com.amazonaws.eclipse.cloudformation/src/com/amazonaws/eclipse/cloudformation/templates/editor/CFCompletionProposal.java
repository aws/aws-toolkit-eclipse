/*
 * Copyright 2013 Amazon Technologies, Inc.
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

/**
 * Extension of the standard CompletionProposal provided by Eclipse, which
 * supports adding extra logic, such as additional comma insertion, when the
 * completion is applied.
 */
public class CFCompletionProposal extends AbstractCompletionProposal {
    private int newCommaPosition = -1;

    public CFCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo);
    }

    /**
     * Sets the document position where this completion proposal should apply an
     * additional comma. This is used when a completion adds a new entry to a
     * map or array, and the previous entry needs to have a trailing comma added
     * in order to keep the JSON document parsing.
     *
     * @param offset
     */
    public void setAdditionalCommaPosition(int offset) {
        this.newCommaPosition = offset;
    }

    public void apply(IDocument document) {
        super.apply(document);

        try {
            if (newCommaPosition > -1) {
                document.replace(newCommaPosition, 0, ",");
                super.fCursorPosition += 1;
            }
        } catch (BadLocationException x) {}
    }
}