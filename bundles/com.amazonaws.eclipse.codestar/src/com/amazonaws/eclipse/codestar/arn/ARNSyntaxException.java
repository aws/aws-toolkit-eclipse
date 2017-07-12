/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.codestar.arn;

public class ARNSyntaxException extends Exception {

    private static final long serialVersionUID = -6197308298174187307L;

    private String input;

    public ARNSyntaxException(String input, String reason) {
        super(reason);

        if (input == null || reason == null) {
            throw new NullPointerException();
        }

        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public String getReason() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append(getReason());
        sb.append(": ");
        sb.append(input);
        return sb.toString();
    }

}