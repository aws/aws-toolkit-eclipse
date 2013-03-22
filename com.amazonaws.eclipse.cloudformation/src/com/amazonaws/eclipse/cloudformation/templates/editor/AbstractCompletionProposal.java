/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.amazonaws.eclipse.cloudformation.templates.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

class AbstractCompletionProposal implements ICompletionProposal {
    /** The string to be displayed in the completion proposal popup. */
    private String fDisplayString;
    /** The replacement string. */
    private String fReplacementString;
    /** The replacement offset. */
    private int fReplacementOffset;
    /** The replacement length. */
    private int fReplacementLength;
    /** The cursor position after this proposal has been applied. */
    int fCursorPosition;
    /** The image to be displayed in the completion proposal popup. */
    private Image fImage;
    /** The context information of this proposal. */
    private IContextInformation fContextInformation;
    /** The additional info of this proposal. */
    private String fAdditionalProposalInfo;

    /**
     * Creates a new completion proposal based on the provided information. The replacement string is
     * considered being the display string too. All remaining fields are set to <code>null</code>.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     */
    public AbstractCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null);
    }

    /**
     * Creates a new completion proposal. All fields are initialized based on the provided information.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     * @param image the image to display for this proposal
     * @param displayString the string to be displayed for the proposal
     * @param contextInformation the context information associated with this proposal
     * @param additionalProposalInfo the additional information associated with this proposal
     */
    public AbstractCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo) {
        Assert.isNotNull(replacementString);
        Assert.isTrue(replacementOffset >= 0);
        Assert.isTrue(replacementLength >= 0);
        Assert.isTrue(cursorPosition >= 0);

        fReplacementString= replacementString;
        fReplacementOffset= replacementOffset;
        fReplacementLength= replacementLength;
        fCursorPosition= cursorPosition;
        fImage= image;
        fDisplayString= displayString;
        fContextInformation= contextInformation;
        fAdditionalProposalInfo= additionalProposalInfo;
    }

    public void apply(IDocument document) {
        try {
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
        } catch (BadLocationException x) {}
    }

    public Point getSelection(IDocument document) {
        return new Point(fReplacementOffset + fCursorPosition, 0);
    }

    public IContextInformation getContextInformation() {
        return fContextInformation;
    }

    public Image getImage() {
        return fImage;
    }

    public String getDisplayString() {
        if (fDisplayString != null)
            return fDisplayString;
        return fReplacementString;
    }

    public String getAdditionalProposalInfo() {
        return fAdditionalProposalInfo;
    }
}