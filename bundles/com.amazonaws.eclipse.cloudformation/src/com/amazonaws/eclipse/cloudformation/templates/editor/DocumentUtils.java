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

import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;

/**
 * Common utilities for searching through documents.
 */
public class DocumentUtils {

    /**
     * Starting at the given position in the specified document, this method
     * reads over chars until it finds a non-whitespace char and returns that
     * char. Return null if none is found.
     *
     * @param document
     *            The document to search in.
     * @param position
     *            The offset in the document to start searching at.
     *
     * @return The first non-whitespace char found after the specified position
     *         in the document.
     */
    public static Character readToNextChar(TemplateDocument document, int position) {
        try {
            while (true) {
                char c = document.getChar(position++);
                if (Character.isWhitespace(c)) continue;

                return c;
            }
        } catch (BadLocationException e) {
            return null;
        }
    }

    /**
     * Reads chars backwards, starting at the specified position in the
     * document, until it finds the first unmatched open brace (i.e. '[' or
     * '{'). If closed braces are found, they are pushed on a stack, so that the
     * nested opening brace matching that one is *not* returned. This method is
     * used to find the opening brace for the array or map that contains the
     * specified position.
     *
     * @param document
     *            The document to search in.
     * @param position
     *            The offset in the document to start searching at.
     *
     * @return The first unmatched open brace, which indicates the map or array
     *         that contains the specified position.
     */
    public static Character readToPreviousUnmatchedOpenBrace(TemplateDocument document, int position) {
        try {
            Stack<Character> stack = new Stack<>();
            position--;
            while (true) {
                char c = document.getChar(position--);

                if (Character.isWhitespace(c)) continue;

                if (c == '}' || c == ']') {
                    stack.push(c);
                } else if (c == '{' || c == '[') {
                    if (stack.isEmpty()) return c;
                    // Just assume the braces are nested correctly
                    stack.pop();
                }
            }
        } catch (BadLocationException e) {
            return null;
        }
    }

    /**
     * Searches the specified document, backwards, starting from the specified
     * position, looking for the first occurrence of the target character, and
     * returns the document position of that first occurrence. return -1 if none
     * is found.
     *
     * @param document
     *            The document to search in.
     * @param position
     *            The offset in the document to start searching at.
     * @param charToFind
     *            The character being searched for.
     *
     * @return The document position of the first occurrence of the specified
     *         target character, occurring before the specified starting
     *         position in the document.
     */
    public static int findPreviousCharPosition(TemplateDocument document, int position, char charToFind) {
        try {
            while (true) {
                position--;
                if (charToFind == document.getChar(position)) return position;
            }
        } catch (BadLocationException e) {
            return -1;
        }
    }

    /**
     * Searches the document backwards, starting at the specified position,
     * looking for the first non-whitespace character, and returns that
     * character. Return null if none is found.
     *
     * @param document
     *            The document to search in.
     * @param position
     *            The offset in the document to start searching at.
     *
     * @return The first non-whitespace character that occurs before the
     *         specified position in the document.
     */
    public static Character readToPreviousChar(TemplateDocument document, int position) {
        try {
            position--;
            while (true) {
                char c = document.getChar(position--);
                if (Character.isWhitespace(c)) continue;
                return c;
            }
        } catch (BadLocationException e) {
            return null;
        }
    }

    /**
     * Reads a string backwards from the current offset position to first
     * occurrence of a double quote. Return null if none is found or a new line is found.
     *
     * @param document
     *            The document to search in.
     * @param position
     *            The offset in the document to start searching at.
     * @return The string from the specified position to the first occurrence of
     *         double quote.
     */
    public static String readToPreviousQuote(TemplateDocument document, int position) {
        StringBuilder typedString = new StringBuilder();
        char c;
        try {
            position--;
            while (true) {
                c = document.getChar(position--);
                if (c == '"') {
                    break;
                } else if (c == '\n' || c == '\r') {
                    return null;
                }
                typedString.append(c);
            }
        } catch (BadLocationException e) {
            return null;
        }
        return typedString.reverse().toString();
    }
}