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

public abstract class AWSGitPushRequest {
    static final String METHOD = "GIT";
    private static final String SERVICE = "devtools";

    protected Date date = new Date();
    protected String host;
    protected String region;
    protected String service = SERVICE;

    public AWSGitPushRequest() {}

    public AWSGitPushRequest(Date dateTime) {
        if (dateTime == null) throw new IllegalArgumentException("dateTime not specified");
        this.date = dateTime;
    }

    public abstract String derivePath();
    public abstract String deriveRequest();

    public Date getDate() {
        return date;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getService() {
        return service;
    }
}