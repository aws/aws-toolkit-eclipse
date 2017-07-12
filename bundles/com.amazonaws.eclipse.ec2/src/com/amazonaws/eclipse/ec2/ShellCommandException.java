/*
 * Copyright 2008-2012 Amazon Technologies, Inc. 
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

package com.amazonaws.eclipse.ec2;

import java.io.IOException;
import java.util.List;


/**
 * Exception describing a failure of a shell command to execute correctly,
 * including output from the command, information about retries, etc.
 */
public class ShellCommandException extends IOException {

    /** default serial version id */
    private static final long serialVersionUID = 1L;

    /** The results of trying to execute the shell command */
    private final List<ShellCommandResults> results;

    /**
     * Creates a new ShellCommandException with the specified message and
     * description of the shell command results.
     * 
     * @param message
     *            A summary of the shell command failure.
     * @param results
     *            The results from attempts to execute the command.
     */
    public ShellCommandException(String message, List<ShellCommandResults> results) {
        super(message);
        this.results = results;
    }

    /**
     * The results of all attempts to execute the associated command.
     * 
     * @return The results of all attempts to execute the associated command.
     */
    public List<ShellCommandResults> getShellCommandResults() {
        return results;
    }

    /**
     * Returns the number of times the associated command was attempted.
     * 
     * @return The number of times the associated command was attempted.
     */
    public int getNumberOfAttempts() {
        return results.size();
    }

}
