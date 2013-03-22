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

import java.util.Date;

import com.amazonaws.eclipse.elasticbeanstalk.git.util.BinaryUtils;

public class AWSElasticBeanstalkGitPushRequest extends AWSGitPushRequest {

    public String application;
    public String environment;


    public AWSElasticBeanstalkGitPushRequest() {
        super();
    }

    public AWSElasticBeanstalkGitPushRequest(Date date) {
        super(date);
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }


    @Override
    public String derivePath() {
        String hexEncodedApplication = BinaryUtils.toHex(BinaryUtils.getBytes(application));
        if (environment == null || environment.length() == 0) {
            return "/repos/" +  hexEncodedApplication;
        } else {
            return "/repos/" + hexEncodedApplication + "/" + environment;
        }
    }

    @Override
    public String deriveRequest() {
        String path = derivePath();
        String request = AWSGitPushRequest.METHOD + "\n" +
                         path + "\n\n" +
                         "host:" + host + "\n\n" +
                         "host\n";
        return request;
    }
}