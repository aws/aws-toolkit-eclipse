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

import java.util.Arrays;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Indentation strategy reacts to newlines by inserting the appropriate amount
 * of indentation.
 */
final class TemplateAutoEditStrategy implements IAutoEditStrategy {

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org
     * .eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
     */
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        try {
            if ( command.text.startsWith("\r") || command.text.startsWith("\n") ) {
                autoIndent(document, command);
            } else if ( command.text.equals("[") ) {
                insertClosingBrace(document, command, "]");
            } else if ( command.text.equals("{") ) {
                insertClosingBrace(document, command, "}");
            }
        } catch ( Exception e ) {
            AwsToolkitCore.getDefault().logException("Error in auto edit:", e);
        }
    }

    private void insertClosingQuote(IDocument document, DocumentCommand command) throws BadLocationException {
        int lineStart = document.getLineInformationOfOffset(command.offset).getOffset();
        String lineContents = document.get(lineStart, command.offset - lineStart);

        // Type over an existing double quote, not before it (unless to type an escape)
        if ( "\"".equals(document.get(command.offset, 1)) ) {
            if ( !"\\".equals(document.get(command.offset - 1, 1)) ) {
                command.text = "\"";
                command.length = 1;
                return;
            }
        }

        boolean keyStart = false;
        for ( int i = lineContents.length() - 1; i >= 0; i-- ) {
            char c = lineContents.charAt(i);
            if ( c == ':' ) {
                keyStart = true;
                break;
            }
        }

        String insertionText = "\"";

        /*
         * How do we know whether to add a comma or not? What do we see next in
         * the document text? ',' = don't append '}' = don't append ']' = don't
         * append ':' = ??? '{' = append '[' = append if our parent is an object
         * and we're in the field
         */

        if ( keyStart && needsTrailingComma(document, command) )
            insertionText += ", ";
        else if ( !keyStart )
            insertionText += " : ";

        // This seems counterintuitive, but we have to mark the command as not
        // shifting the caret so that it won't get shifted twice. See related
        // code in TextViewer.handleVerifyEvent, which does the shifting itself.
        // The command's own shifting just gets in the way.
        command.shiftsCaret = false;
        command.caretOffset = command.offset + 1;
        command.text += insertionText;
    }

    private void insertClosingBrace(IDocument document, DocumentCommand command, String closingChar) throws BadLocationException {
        int indentLength = findIndentationLevel(document, command.offset);

        char[] indentation = new char[indentLength];
        Arrays.fill(indentation, ' ');

        String newline = System.getProperty("line.separator");
        String insertionText = newline + new String(indentation) + closingChar;
        if ( needsTrailingComma(document, command) )
            insertionText += ", ";
        command.text += insertionText;
        // This seems counterintuitive, but we have to mark the command as not
        // shifting the caret so that it won't get shifted twice. See related
        // code in TextViewer.handleVerifyEvent, which does the shifting itself.
        // The command's own shifting just gets in the way.
        command.shiftsCaret = false;
        command.caretOffset = command.offset + 1;
    }

    // TODO: The content assist processor has some very similar code duplicated for this
    private boolean needsTrailingComma(IDocument document, DocumentCommand command) {
        int position = command.offset + 1;
        try {
            String s = document.get(position, 1);
            while ( document.getLength() > position && Character.isWhitespace(s.charAt(0)) ) {
                s = document.get(position, 1);
                if ( s.equals("}") || s.equals("]") || s.equals(","))
                    return false;
                position++;
            }

            return true;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void autoIndent(IDocument document, DocumentCommand command) {
        try {
            int indentation = findIndentationLevel(document, command.offset);
            char[] insertionText = new char[indentation];
            Arrays.fill(insertionText, ' ');
            command.shiftsCaret = true;
            command.text = command.text + new String(insertionText);
        } catch ( Exception e ) {
            AwsToolkitCore.getDefault().logException("Failed to auto-indent", e);
        }
    }

    /**
     * To determine the indentation of a line, we need to scan backwards
     * until we find a line with some non-whitespace text on it, then use
     * that for our anchor. Indent +2 if there are more open than close
     * braces on this line.
     */
    private int findIndentationLevel(IDocument document, int offset) throws BadLocationException {
        while ( offset > 0 ) {
            IRegion currLineInfo = document.getLineInformationOfOffset(offset);
            String lineContents = document.get(currLineInfo.getOffset(), currLineInfo.getLength());

            boolean nonWhitespaceCharFound = false;
            int numOpenBrackets = 0;
            int indentation = 0;
            for ( int i = 0; i < lineContents.length() && currLineInfo.getOffset() + i < offset; i++ ) {
                char c = lineContents.charAt(i);
                if ( !Character.isWhitespace(c) ) {
                    if ( !nonWhitespaceCharFound ) {
                        indentation = i;
                    }
                    nonWhitespaceCharFound = true;
                    if ( c == '{' || c == '[' )
                        numOpenBrackets++;
                    if ( c == '}' || c == ']' )
                        numOpenBrackets--;
                }
            }

            if ( numOpenBrackets > 0 ) {
                indentation += 2;
            }

            if ( nonWhitespaceCharFound ) {
                return indentation;
            } else {
                // this line didn't have any non-whitespace characters,
                // go to the previous one
                offset = currLineInfo.getOffset() - 1;
            }
        }
        return 0;
    }
}