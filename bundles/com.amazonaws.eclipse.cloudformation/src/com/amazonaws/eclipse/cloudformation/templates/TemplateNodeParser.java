/*
 * Copyright 2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.cloudformation.templates;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.amazonaws.eclipse.cloudformation.templates.TemplateNodePath.PathNode;
import com.amazonaws.eclipse.cloudformation.templates.editor.TemplateDocument;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Template parser to build the hierarchical TemplateNode tree. Ex:
 * {
 *   "Resources": {
 *     "S3Bucket": {
 *       "Type": "AWS::S3::Bucket",
 *       "Properties": {
 *         ...
 *       }
 *     }
 *   },
 *   "Conditions": [
 *     {
 *       "Key": "somekey1",
 *       "Value": "somevalue1"
 *     },
 *     {
 *       "Key": "somekey2",
 *       "Value": "somevalue2"
 *     }
 *   ]
 * }
 *
 * is to be parsed as the following structure:
 *
 * ROOT(O)
 * |--> Resources(F) --> Resources(O)
 * |                     +--> "S3Bucket"(F) --> AWS::S3::Bucket(O)
 * |                                            |--> Type(F) --> "AWS::S3::Bucket"(V)
 * |                                            +--> Properties(F) --> Properties(O)
 * +--> Conditions(F) --> Conditions(A)
 *                        |--> 0(I) --> Condition(O)
 *                        |             |--> Key(F) --> "somekey1"(V)
 *                        |             +--> Value(F) --> "somevalue1"(V)
 *                        +--> 1(I) --> Condition(O)
 *                                      |--> Key(F) --> "somekey2"(V)
 *                                      +--> Value(F) --> "somevalue2"(V)
 *
 *** (O: Object node; F: Field node; A: Array node; I: Index node; V: Value node)
 *
 * If the document is not a valid Json file, the parser would record the {@link JsonParseException}
 * and the {@link TemplateNodePath} to the position where error occurs.
 */
public class TemplateNodeParser {

    private static final JsonFactory FACTORY = new ObjectMapper().getFactory();
    private static final String ROOT_SCHEMA_OBJECT = TemplateNode.ROOT_PATH;

    private JsonLocation lastLocation;
    private JsonLocation currentLocation;
    private final Stack<PathNode> path = new Stack<>();
    private Exception exception;

    // Test use only.
    TemplateObjectNode parse(String document) throws Exception {
        return parse(document, document.length());
    }

    private TemplateObjectNode parse(String document, int offset) throws Exception {
        path.clear();
        exception = null;
        try {
            JsonParser parser = FACTORY.createParser(document.substring(0, offset));
            return parse(parser);
        } catch (Exception e) {
            exception = e;
            throw e;
        }
    }

    public TemplateObjectNode parse(TemplateDocument document, int offset) {
        TemplateObjectNode node = null;
        try {
            node = parse(document.get(), offset);
            document.setModel(node);
        } catch (Exception e) {
            // do nothing
        } finally {
            document.setSubPaths(getSubPaths());
        }
        return node;
    }

    public TemplateObjectNode parse(TemplateDocument document) {
        return parse(document, document.getLength());
    }

    public Exception getJsonParseException() {
        return exception;
    }

    public List<PathNode> getSubPaths() {
        return Collections.unmodifiableList(path);
    }

    // Test use only
    String getPath() {
        List<PathNode> subPaths = getSubPaths();
        StringBuilder builder = new StringBuilder();
        for (PathNode subPath : subPaths) {
            builder.append(String.format("%s%s", subPath.getReadiblePath(), TemplateNode.PATH_SEPARATOR));
        }
        return builder.toString();
    }

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
            push(new PathNode(currentField));
            token = nextToken(parser);

            if (token == JsonToken.START_OBJECT)
                object.put(currentField, parseObject(parser));
            if (token == JsonToken.START_ARRAY)
                object.put(currentField, parseArray(parser));
            if (token == JsonToken.VALUE_STRING)
                object.put(currentField, parseValue(parser));

            pop();
            // we have to hack it here to mark the map key as a parameter in the path with the Type value for child schema
            if (currentField.equals("Type") && object.get(currentField) instanceof TemplateValueNode) {
                String value = ((TemplateValueNode) object.get(currentField)).getText();
                PathNode mapKey = pop();
                mapKey = new PathNode(mapKey.getFieldName(), value);
                push(mapKey);
            }
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
        int index = 0;
        do {
            token = nextToken(parser);
            if (token == JsonToken.END_ARRAY)
                break;

            push(new PathNode(index));
            if (token == JsonToken.START_OBJECT)
                array.add(parseObject(parser));
            if (token == JsonToken.START_ARRAY)
                array.add(parseArray(parser));
            if (token == JsonToken.VALUE_STRING)
                array.add(parseValue(parser));
            pop();
            ++index;

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
        push(new PathNode(ROOT_SCHEMA_OBJECT));
        TemplateObjectNode rootNode = parseObject(parser);
        pop();
        return rootNode;
    }

    /**
     * Pushes the given token to the stack.
     * @param token
     */
    private void push(PathNode token){
        path.push(token);
    }

    /**
     * Pops a token from the stack.
     * @return
     */
    private PathNode pop(){
        return path.pop();
    }
}
