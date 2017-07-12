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

import java.io.IOException;
import java.util.List;

/**
 * Exception containing details on a file that failed to be copied to a remote
 * host correctly.
 */
public class RemoteFileCopyException extends IOException {
    
    /** auto-generated serialization id */
    private static final long serialVersionUID = -8004706952817340784L;
    
    /** The location the file was to be copied on the remote host */
    private final String remoteFile;
    
    /** The location of the local file to copy */
    private final String localFile;

    /** A list of the results from each attempt to copy the local file to the remote host */
    private final List<RemoteFileCopyResults> resultsFromAllAttempts;


    /**
     * Constructs a new RemoteFileCopyException complete with all the results
     * from each attempt to copy the file to the remote host.
     * 
     * @param localFile
     *            The local file attempting to be copied.
     * @param remoteFile
     *            The remote location for the local file to be copied.
     * @param resultsFromAllAttempts
     *            A list of all the results from each attempt at trying to copy
     *            this file to the remote host.
     */
    public RemoteFileCopyException(String localFile, String remoteFile, List<RemoteFileCopyResults> resultsFromAllAttempts) {
        super("Unable to copy remote file after trying " + resultsFromAllAttempts.size() + " times");
        
        this.localFile = localFile;
        this.remoteFile = remoteFile;
        this.resultsFromAllAttempts = resultsFromAllAttempts;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        String superMessage = super.getMessage();
        
        String message = superMessage + 
                "\n\tlocal file: '" + localFile + "'" +
                "\n\tremote file: '" + remoteFile + "'\n";

        if (resultsFromAllAttempts != null && resultsFromAllAttempts.size() > 0) {
            RemoteFileCopyResults results = resultsFromAllAttempts.get(0);
            message += "\nResults from first attempt:";
            message += "\n\t" + results.getErrorMessage();
            
            if (results.getError() != null) {
                message += "\n\troot cause: " + results.getError().getMessage();
            }
            
            message += "\n";
        }
        
        return message;
    }

}
