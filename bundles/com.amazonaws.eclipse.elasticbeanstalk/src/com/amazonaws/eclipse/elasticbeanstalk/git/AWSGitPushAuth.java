/*
 * Copyright 2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.git;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.eclipse.elasticbeanstalk.git.util.BinaryUtils;
import com.amazonaws.eclipse.elasticbeanstalk.git.util.DateUtils;

public class AWSGitPushAuth {

    private static final String ALGORITHM  = "HMAC-SHA256";
    private static final String SCHEME     = "AWS4";
    private static final String TERMINATOR = "aws4_request";

    private final AWSGitPushRequest request;

    private static final Logger log = Logger.getLogger(AWSGitPushAuth.class.getCanonicalName());


    public AWSGitPushAuth(AWSGitPushRequest request) {
        this.request = request;
    }

    private static byte[] deriveKey(String secretKey, AWSGitPushRequest request) throws Exception {
        byte[] kSecret  = BinaryUtils.getBytes(AWSGitPushAuth.SCHEME + secretKey);
        byte[] kDate    = BinaryUtils.sign(BinaryUtils.getBytes(DateUtils.formatDateStamp(request.getDate())), kSecret);
        byte[] kRegion  = BinaryUtils.sign(BinaryUtils.getBytes(request.getRegion()), kDate);
        byte[] kService = BinaryUtils.sign(BinaryUtils.getBytes(request.getService()), kRegion);
        byte[] kSigning = BinaryUtils.sign(BinaryUtils.getBytes(AWSGitPushAuth.TERMINATOR), kService);
        return kSigning;
    }

    public String derivePassword(String awsSecretKey) throws Exception {
        String signature = signRequest(awsSecretKey, request);
        String password = DateUtils.formatDateTimeStamp(request.getDate()) + "Z" + signature;
        return password;
    }

    public URI deriveRemote(String awsAccessKey, String awsSecretKey) throws Exception {
        String path = request.derivePath();
        String password = derivePassword(awsSecretKey);
        String username = awsAccessKey;

        /*
         * Example Git Push Remote URL:
         * https://1TNT3D6W620GJR87HT02:20120228T212659Z4e6aa7d004b9aa8b8dae8523eaa4a2bebd74088a90ba33f606099d180bff317b@aws-git-push.amazonaws.com/repos/6170706c69636174696f6e/environment/info/refs
         */
        URI uri = new URI("https://" + username + ":" + password + "@" + request.getHost() + path);
        return uri;
    }

    static String signRequest(String awsSecretKey, AWSGitPushRequest request) throws Exception {
        String scope = DateUtils.formatDateStamp(request.getDate()) + "/"
                     + request.getRegion() + "/"
                     + request.getService() + "/"
                     + AWSGitPushAuth.TERMINATOR;

        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(AWSGitPushAuth.SCHEME + "-" + AWSGitPushAuth.ALGORITHM + "\n"
                + DateUtils.formatDateTimeStamp(request.getDate()) + "\n"
                + scope + "\n");

        if (log.isLoggable(Level.FINE)) log.fine("Request: " + request.deriveRequest());

        byte[] requestBytes = BinaryUtils.getBytes(request.deriveRequest());
        byte[] requestDigest = BinaryUtils.hash(requestBytes);
        stringToSign.append(BinaryUtils.toHex(requestDigest));

        if (log.isLoggable(Level.FINE)) log.fine("StringToSign: " + stringToSign);

        byte[] key = deriveKey(awsSecretKey, request);
        byte[] digest = BinaryUtils.sign(BinaryUtils.getBytes(stringToSign.toString()), key);
        String signature = BinaryUtils.toHex(digest);

        if (log.isLoggable(Level.FINE)) log.fine("Signature: " + signature);

        return signature;
    }

}
