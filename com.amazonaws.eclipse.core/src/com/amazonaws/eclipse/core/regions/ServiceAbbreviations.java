/*
 * Copyright 2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.regions;

/**
 * Common abbreviations for looking up information about a specific service, for
 * example, used in {@link Region#getServiceEndpoints()}.
 */
public final class ServiceAbbreviations {
    public static final String AUTOSCALING = "AutoScaling";
    public static final String ELB         = "ELB";
    public static final String CLOUDFRONT  = "CloudFront";
    public static final String EC2         = "EC2";
    public static final String IAM         = "IAM";
    public static final String S3          = "S3";
    public static final String SIMPLEDB    = "SimpleDB";
    public static final String SNS         = "SNS";
    public static final String SQS         = "SQS";
    public static final String BEANSTALK   = "ElasticBeanstalk";
    public static final String RDS         = "RDS";
}