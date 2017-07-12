/*
 * Copyright 2009-2012 Amazon Technologies, Inc. 
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

/**
 * Represents the results of an attempt to copy a local file to a remote host.
 */
public class RemoteFileCopyResults {

    /** The local file being copied */
    private final String localFile;
    
    /** The remote destination file */
    private final String remoteFile;
    
    /** True if the file was successfully copied to the remote location */
    private boolean succeeded;
    
    /** An optional error message if the remote file copy was not successful */
    private String errorMessage;
    
    /** An optional exception describing why this attempt failed */
    private Exception error;

    /**
     * An optional string containing the external output from the copy command
     * such as any error messages from the command used for the copy
     */
    private String externalOutput;

    /**
     * Creates a new RemoteFileCopyResults object describing the results of
     * copying the specified local file to the specified remote file location.
     * 
     * @param localFile
     *            The local file being copied.
     * @param remoteFile
     *            The remote file location.
     */
    public RemoteFileCopyResults(String localFile, String remoteFile) {
        this.localFile = localFile;
        this.remoteFile = remoteFile;
    }

    /**
     * @return The local file location.
     */
    public String getLocalFile() {
        return localFile;
    }

    /**
     * @return The remote file location.
     */
    public String getRemoteFile() {
        return remoteFile;
    }

    /**
     * @param errorMessage
     *            the error message describing how this remote file copy failed.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the error message describing how this remote file copy attempt
     *         failed.
     */
    public String getErrorMessage() {
        if (externalOutput == null || externalOutput.trim().length() == 0) {
            return errorMessage;
        }
        
        return errorMessage + ":\n\t" + externalOutput;
    }

    /**
     * @param wasSuccessful
     *            True if the file was successfully copied to the remote file
     *            location.
     */
    public void setSucceeded(boolean wasSuccessful) {
        this.succeeded = wasSuccessful;
    }

    /**
     * @return True if the file was successfully copied to the remote file
     *         location.
     */
    public boolean isSucceeded() {
        return succeeded;
    }

    /**
     * @param error the error to set
     */
    public void setError(Exception error) {
        this.error = error;
    }

    /**
     * @return the error
     */
    public Exception getError() {
        return error;
    }

    /**
     * @param externalOutput
     *            The command output from the attempt to copy the file to the
     *            remote location.
     */
    public void setExternalOutput(String externalOutput) {
        this.externalOutput = externalOutput;
    }
}
