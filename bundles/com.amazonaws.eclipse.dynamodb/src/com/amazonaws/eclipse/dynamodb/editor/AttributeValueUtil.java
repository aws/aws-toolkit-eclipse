/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.dynamodb.editor;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.json.Jackson;

/**
 * Utility methods for working with attribute values
 */
public class AttributeValueUtil {

    /**
     * Data type constants corresponding to the six fields of the
     * {@link AttributeValue} object.
     */
    static final int S = 0;
    static final int SS = 1;
    static final int N = 2;
    static final int NS = 3;
    static final int B = 4;
    static final int BS = 5;

    /**
     * Sets exactly one field of the {@link AttributeValue} given, clearing all
     * others. The field set is determined by the datatype.
     */
    static void setAttribute(AttributeValue attributeValue, final Collection<String> newValue, int dataType) {
        clearAttributes(attributeValue);
        if ( newValue.isEmpty() )
            return;
        switch (dataType) {
        case NS:
            attributeValue.setNS(newValue);
            break;
        case SS:
            attributeValue.setSS(newValue);
            break;
        case BS:
            attributeValue.setBS(getByteBuffers(newValue));
            break;
        case N:
            attributeValue.setN(newValue.iterator().next());
            break;
        case S:
            attributeValue.setS(newValue.iterator().next());
            break;
        case B:
            attributeValue.setB(getByteBuffer(newValue.iterator().next()));
            break;
        default:
            throw new RuntimeException("Unknown data type " + dataType);
        }
    }

    /**
     * Gets a byte buffer corresponding to the base64 string given.
     */
    private static ByteBuffer getByteBuffer(String base64) {
        byte[] binary = BinaryUtils.fromBase64(base64);
        return ByteBuffer.wrap(binary);
    }

    /**
     * Returns a list of ByteBuffers corresponding to the base64 strings given.
     */
    private static Collection<ByteBuffer> getByteBuffers(Collection<String> newValue) {
        List<ByteBuffer> buffers = new LinkedList<>();
        for (String value : newValue) {
            buffers.add(getByteBuffer(value));
        }
        return buffers;
    }

    static void setAttribute(AttributeValue attributeValue, final Collection<String> newValue, String dataType) {
        if ( ScalarAttributeType.N.toString().equals(dataType) ) {
            setAttribute(attributeValue, newValue, N);
        } else if ( ScalarAttributeType.S.toString().equals(dataType) ) {
            setAttribute(attributeValue, newValue, S);
        } else if ( ScalarAttributeType.B.toString().equals(dataType) ) {
            setAttribute(attributeValue, newValue, B);
        } else {
            throw new RuntimeException("Unknown data type " + dataType);
        }
    }

    /**
     * Translates the data types returned by some Dynamo apis into the integers
     * used by this class.
     */
    static int getDataType(String dataType) {
        if ( ScalarAttributeType.S.toString().equals(dataType) ) {
            return S;
        } else if ( ScalarAttributeType.N.toString().equals(dataType) ) {
            return N;
        } else if ( ScalarAttributeType.B.toString().equals(dataType) ) {
            return B;
        } else if ( "SS".equals(dataType) ) {
            return SS;
        } else if ( "NS".equals(dataType) ) {
            return NS;
        } else if ( "BS".equals(dataType) ) {
            return BS;
        } else {
            throw new RuntimeException("Unknown data type " + dataType);
        }
    }

    /**
     * Clears all fields from the object given.
     */
    static void clearAttributes(AttributeValue attributeValue) {
        attributeValue.setSS(null);
        attributeValue.setNS(null);
        attributeValue.setS(null);
        attributeValue.setN(null);
        attributeValue.setB(null);
        attributeValue.setBS(null);
    }

    /**
     * Formats the value of the given {@link AttributeValue} for display,
     * joining list elements with a comma and enclosing them in brackets.
     */
    static String format(final AttributeValue value) {
        if ( value == null )
            return "";
        if ( value.getN() != null )
            return value.getN();
        else if ( value.getNS() != null )
            return join(value.getNS());
        else if ( value.getS() != null )
            return value.getS();
        else if ( value.getSS() != null )
            return join(value.getSS(), true);
        else if ( value.getB() != null )
            return base64Format(value.getB());
        else if ( value.getBS() != null )
            return joinBase64(value.getBS());
        else if ( value.getBOOL() != null )
            return value.getBOOL() ? "true" : "false";
        else if ( value.getNULL() != null )
            return value.getNULL() ? "null" : "";
        else if ( value.getM() != null )
            return formatMapAttribute(value.getM());
        else if ( value.getL() != null )
            return formatListAttribute(value.getL());
        return "";
    }

    private static String formatMapAttribute(Map<String, AttributeValue> map) {
        if (map == null) return "";

        try {
            Map<String, Object> objectMap = InternalUtils.toSimpleMapValue(map);
            return Jackson.toJsonString(objectMap);
        } catch (Exception e) {
            return "A map of " + map.size() + " entries";
        }
    }

    private static String formatListAttribute(List<AttributeValue> list) {
        if (list == null) return "";

        try {
            List<Object> objectList = InternalUtils.toSimpleListValue(list);
            return Jackson.toJsonString(objectList);
        } catch (Exception e) {
            return "A list of " + list.size() + " entries";
        }
    }

    /**
     * Returns the given byte buffer list as a base-64 formatted list
     */
    private static String joinBase64(List<ByteBuffer> bs) {
        Collection<String> base64Strings = base64FormatOfBinarySet(bs);
        return join(base64Strings);
    }

    /**
     * Returns a base-64 string of the given bytes
     */
    private static String base64Format(ByteBuffer b) {
        return BinaryUtils.toBase64(b.array());
    }

    /**
     * Returns a base-64 string of the given bytes
     */
    private static Collection<String> base64FormatOfBinarySet(Collection<ByteBuffer> bs) {
        List<String> base64Strings = new LinkedList<>();
        for (ByteBuffer b : bs) {
            base64Strings.add(base64Format(b));
        }
        return base64Strings;
    }

    /**
     * Joins a collection of values with commas, enclosed by brackets. An empty
     * or null set of values returns the empty string.
     */
    static String join(final Collection<String> values) {
        return join(values, false);
    }

    /**
     * Joins a collection of values with commas, enclosed by brackets. An empty
     * or null set of values returns the empty string.
     *
     * @param quoted
     *            Whether each value should be quoted in the output.
     */
    static String join(final Collection<String> values, boolean quoted) {
        if ( values == null || values.isEmpty() ) {
            return "";
        }

        StringBuilder builder = new StringBuilder("{");
        boolean seenOne = false;
        for ( String s : values ) {
            if ( seenOne ) {
                builder.append(",");
            } else {
                seenOne = true;
            }
            builder.append(quoted ? "\"" + s + "\"" : s);
        }
        builder.append("}");

        return builder.toString();
    }

    /**
     * Returns the values from this {@link AttributeValue} as a collection,
     * which may contain only one element or be empty.
     */
    static Collection<String> getValuesFromAttribute(AttributeValue value) {
        if ( value == null )
            return Collections.emptyList();
        if ( value.getN() != null ) {
            return Arrays.asList(value.getN());
        } else if ( value.getNS() != null ) {
            return value.getNS();
        } else if ( value.getS() != null ) {
            return Arrays.asList(value.getS());
        } else if ( value.getSS() != null ) {
            return value.getSS();
        } else if ( value.getB() != null ) {
            return Arrays.asList(base64Format(value.getB()));
        } else if ( value.getBS() != null ) {
            return base64FormatOfBinarySet(value.getBS());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Validates the user input of a scalar attribute value.
     */
    public static boolean validateScalarAttributeInput(String attributeInput, int dataType, boolean acceptEmpty) {
        if ( null == attributeInput ) {
            return false;
        }
        if ( attributeInput.isEmpty() ) {
            return acceptEmpty;
        }
        if ( dataType == S ) {
            return attributeInput.length() > 0;
        } else if ( dataType == N ) {
            return validateNumberStringInput(attributeInput);
        } else if ( dataType == B ) {
            return validateBase64StringInput(attributeInput);
        } else {
            return false;
        }
    }

    /**
     * Returns the warning message for the given attribute type.
     */
    public static String getScalarAttributeValidationWarning(String attributeName, int dataType) {
        if ( dataType == S ) {
            return "Invalid String value for " + attributeName + ".";
        } else if ( dataType == N ) {
            return "Invalid Number value for " + attributeName + ".";
        } else if ( dataType == B ) {
            return "Invalid Base64 string for " + attributeName + ".";
        } else {
            return "";
        }
    }

    /**
     * Validates the user input of Base64 string.
     */
    private static boolean validateBase64StringInput(String base64) {
        byte[] decoded = BinaryUtils.fromBase64(base64);
        String encodedAgain = BinaryUtils.toBase64(decoded);
        return base64.equals(encodedAgain);
    }

    /**
     * Validates the user input of a number.
     */
    private static boolean validateNumberStringInput(String number) {
        try {
            new BigInteger(number, 10);
        } catch ( NumberFormatException e ) {
            return false;
        }
        return true;
    }

}
