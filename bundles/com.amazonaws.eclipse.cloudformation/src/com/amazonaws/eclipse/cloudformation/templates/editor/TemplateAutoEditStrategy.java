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
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;

/**
 * Indentation strategy reacts to newlines or open characters by inserting the appropriate amount
 * of indentation and moving the caret to the appropriate location. We apply the auto-edit strategy
 * in the following two situations:
 *
 * 1. Newline character
 *     // See {@link IndentType} for more information.
 *   - Double indent
 *   - Open indent
 *   - Close indent
 *   - Jump out
 * 2. Open character - includes {, [, (, ", '
 */
final class TemplateAutoEditStrategy implements IAutoEditStrategy {

    // TODO to move this to preference page setting
    static final int INDENTATION_LENGTH = 2;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org
     * .eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
     */
    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        try {
            if (isNewlineCommand(command.text)) {
                IndentContext context = getCurrentIndentContext(document, command);
                if (context == null) {
                    return;
                }
                switch (context.indentType) {
                case JUMP_OUT:
                    command.text = "";
                    command.caretOffset = command.offset + context.indentValue;
                    break;
                case DOUBLE_INDENT:
                    command.text += sameCharSequence(' ', context.indentValue);
                    int length = command.text.length();
                    command.text += LINE_SEPARATOR + sameCharSequence(' ', Math.max(context.indentValue - INDENTATION_LENGTH, 0));
                    command.shiftsCaret = false;
                    command.caretOffset = command.offset + length;
                    break;
                case OPEN_INDENT:
                case CLOSE_INDENT:
                default:
                    command.text += sameCharSequence(' ', context.indentValue);
                    command.shiftsCaret = true;
                    break;
                }
            } else if (PairType.isOpenCharacter(command.text)) {
                insertClosingCharacter(document, command, PairType.fromValue(command.text).closeChar);
            }
        } catch (Exception e) {
            CloudFormationPlugin.getDefault().logError("Error in auto edit:", e);
        }
    }

    private void insertClosingCharacter(IDocument document, DocumentCommand command, String closingChar) throws BadLocationException {
        String insertionText = closingChar;
        if (needsTrailingComma(document, command)) {
            insertionText += ",";
        }
        command.text += insertionText;
        command.shiftsCaret = false;
        command.caretOffset = command.offset + 1;
    }

    /**
     * Return true if the next non-whitespace character is an open character, i.e {, [, (, ", '
     */
    private boolean needsTrailingComma(IDocument document, DocumentCommand command) throws BadLocationException {
        PairType pairType = PairType.fromValue(command.text);
        // If the pair is single quote, or parentheses which are not Json token, we don't append the trailing comma.
        if (pairType == null || !pairType.isJsonToken) {
            return false;
        }
        int position = command.offset;
        while (document.getLength() > position) {
            String nextChar = document.get(position, 1);
            if (PairType.isOpenCharacter(nextChar) && PairType.fromValue(nextChar).isJsonToken) {
                return true;
            } else if (!Character.isWhitespace(nextChar.charAt(0))) {
                return false;
            }
            ++position;
        }
        return false;
    }


    private static String sameCharSequence(char character, int length) {
        char[] sequence = new char[length];
        Arrays.fill(sequence, character);
        return new String(sequence);
    }

    private static boolean isNewlineCommand(String characters) {
        return characters.startsWith("\r") || characters.startsWith("\n");
    }

    private static IndentContext getCurrentIndentContext(IDocument document, DocumentCommand command) throws BadLocationException {
        int offset = command.offset;
        while (offset > 0) {
            IRegion region = document.getLineInformationOfOffset(offset);
            String lineContents = document.get(region.getOffset(), region.getLength());
            IndentContext context = parseOneLine(lineContents, offset - region.getOffset());
            if (context == null) {
                offset = region.getOffset() - 1;
            } else {
                return context;
            }
        }

        return null;
    }

    /**
     * Parse the current line of the document and return an IndentContext for further action.
     *
     * @param line - The current line of the document.
     * @param offset - The offset of the caret.
     * @return - The IndentContext to be used for further action.
     */
    static IndentContext parseOneLine(String line, int offset) {
        int indent = leadingWhitespaceLength(line, 0, offset);
        if (indent >= offset) {
            return null;
        }
        Integer openIndex = parseLineLeftToRight(line, indent, offset - indent);
        Integer closeIndex = parseLineRightToLeft(line, offset, line.length() - offset);
        if (openIndex == null && closeIndex == null) {
            return new IndentContext(IndentType.OPEN_INDENT, indent);
        } else if (openIndex == null && closeIndex != null) {
            if (leadingWhitespaceLength(line, offset, closeIndex - offset) == closeIndex - offset) {
                return new IndentContext(IndentType.CLOSE_INDENT, Math.max(indent - INDENTATION_LENGTH, 0));
            } else {
                return new IndentContext(IndentType.OPEN_INDENT, indent);
            }
        } else if (openIndex != null && closeIndex == null) {
            return new IndentContext(IndentType.OPEN_INDENT, indent + INDENTATION_LENGTH);
        } else {
            PairType type = PairType.fromValues(line.charAt(openIndex), line.charAt(closeIndex));
            if (type == null) {
                return new IndentContext(IndentType.OPEN_INDENT, indent);
            } else if (type.needIndent) {
                boolean adjencentToOpenChar = leadingWhitespaceLength(line, openIndex + 1, offset - openIndex - 1) == offset - openIndex - 1;
                boolean adjencentToCloseChar = leadingWhitespaceLength(line, offset, closeIndex - offset) == closeIndex - offset;
                if (adjencentToOpenChar && adjencentToCloseChar) {
                    return new IndentContext(IndentType.DOUBLE_INDENT, indent + INDENTATION_LENGTH);
                } else if (adjencentToCloseChar) {
                    return new IndentContext(IndentType.CLOSE_INDENT, indent);
                } else {
                    return new IndentContext(IndentType.OPEN_INDENT, indent + INDENTATION_LENGTH);
                }
            } else {
                indent = closeIndex - offset + 1;
                return new IndentContext(IndentType.JUMP_OUT, indent);
            }
        }
    }

    private static int leadingWhitespaceLength(String line, int offset, int length) {
        assert(offset + length <= line.length());
        int i = offset;
        while (offset + length > i && Character.isWhitespace(line.charAt(i))) {
            ++i;
        }
        return i - offset;
    }

    /**
     * Parse a line of string and return the index of the last unpaired character.
     * Ex.
     *   - {"key":"value      returns "
     *   - {"key":"value"     returns {
     *   - {"key":["value"    returns [
     */
    private static Integer parseLineLeftToRight(String line, int offset, int length) {
        if (length <= 0) {
            return null;
        }
        Stack<Integer> stack = new Stack<>();
        for (int i = offset; i < offset + length; ++i) {
            PairType type = PairType.fromValue(line.charAt(i));
            if (type == null) {
                continue;
            }
            if (stack.isEmpty()) {
                if (PairType.isOpenCharacter(line.charAt(i))) {
                    stack.push(i);
                }
            } else {
                char lastCharInStack = line.charAt(stack.peek());
                if (PairType.fromValues(lastCharInStack, line.charAt(i)) != null) {
                    stack.pop();
                } else if (PairType.isOpenCharacter(line.charAt(i))) {
                    stack.push(i);
                }
            }
        }
        return stack.isEmpty() ? null : stack.pop();
    }

    /**
     * Parse a line of string and return the first unpaired character.
     * Ex.
     *   - value"}            returns "
     *   - , "key2":"value2"} returns }
     *   - , "value2"]}       returns ]
     */
    private static Integer parseLineRightToLeft(String line, int offset, int length) {
        if (length <= 0) {
            return null;
        }
        Stack<Integer> stack = new Stack<>();
        for (int i = offset + length - 1; i >= offset; --i) {
            PairType type = PairType.fromValue(line.charAt(i));
            if (type == null) {
                continue;
            }
            if (stack.isEmpty()) {
                if (PairType.isCloseCharacter(line.charAt(i))) {
                    stack.push(i);
                }
            } else {
                char lastCharInStack = line.charAt(stack.peek());
                if (PairType.fromValues(line.charAt(i), lastCharInStack) != null) {
                    stack.pop();
                } else if (PairType.isCloseCharacter(line.charAt(i))) {
                    stack.push(i);
                }
            }
        }
        return stack.isEmpty() ? null : stack.pop();
    }

    static class IndentContext {
        IndentType indentType;
        int indentValue;

        public IndentContext(IndentType indentType, int indentValue) {
            this.indentType = indentType;
            this.indentValue = indentValue;
        }
    }

    // Indent types when typing 'return' key
    static enum IndentType {
                        // '|' stands for cursor
        DOUBLE_INDENT,  // {|}                => {\n  |\n}
        OPEN_INDENT,    // {|"key":"value"}   => {\n  |"key":"value"}
        CLOSE_INDENT,   // {"key":"value"|}   => {"key":"value"\n|}
        SHIFT_INDENT,   //   |  "key":"value" =>
        JUMP_OUT,       // {"key":"val|ue"}   => {"key":"value"|}
        ;
    }

    private static enum PairType {
        BRACES("{", "}", true, true),
        BRACKETS("[", "]", true, true),
        PARENTHESES("(", ")", false, false),
        QUOTES("\"", "\"", false, true),
        SINGLE_QUOTES("'", "'", false, false)
        ;

        private PairType(String openChar, String closeChar, boolean needIndent, boolean isJsonToken) {
            this.openChar = openChar;
            this.closeChar = closeChar;
            this.needIndent = needIndent;
            this.isJsonToken = isJsonToken;
        }
        String openChar;
        String closeChar;
        boolean needIndent;
        boolean isJsonToken;

        static PairType fromValue(String character) {
            for (PairType pair : PairType.values()) {
                if (pair.openChar.equals(character) || pair.closeChar.equals(character)) {
                    return pair;
                }
            }
            return null;
        }

        static PairType fromValue(char character) {
            return fromValue(String.valueOf(character));
        }

        static boolean isOpenCharacter(String character) {
            for (PairType pair : PairType.values()) {
                if (pair.openChar.equals(character)) {
                    return true;
                }
            }
            return false;
        }

        static boolean isOpenCharacter(char character) {
            return isOpenCharacter(String.valueOf(character));
        }

        static boolean isCloseCharacter(String character) {
            for (PairType pair : PairType.values()) {
                if (pair.closeChar.equals(character)) {
                    return true;
                }
            }
            return false;
        }

        static boolean isCloseCharacter(char character) {
            return isCloseCharacter(String.valueOf(character));
        }

        static PairType fromValues(String open, String close) {
            for (PairType pair : PairType.values()) {
                if (pair.openChar.equals(open) && pair.closeChar.equals(close)) {
                    return pair;
                }
            }
            return null;
        }

        static PairType fromValues(char open, char close) {
            return fromValues(String.valueOf(open), String.valueOf(close));
        }
    }
}