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
package com.amazonaws.eclipse.explorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import com.amazonaws.eclipse.core.mobileanalytics.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.mobileanalytics.MetricsDataModel;

public abstract class AwsAction extends Action {
    public static final String END_RESULT = "EndResult";
    public static final String PERFORMED = "Performed";
    public static final String SUCCEEDED = "Succeeded";
    public static final String FAILED = "Failed";
    public static final String CANCELED = "Canceled";

    private final MetricsDataModel metricsDataModel;

    protected AwsAction(AwsToolkitMetricType metricType) {
        metricsDataModel = new MetricsDataModel(metricType);
    }

    protected AwsAction(AwsToolkitMetricType metricType, String text) {
        super(text);
        this.metricsDataModel = new MetricsDataModel(metricType);
    }

    protected AwsAction(AwsToolkitMetricType metricType, String text, int style) {
        super(text, style);
        this.metricsDataModel = new MetricsDataModel(metricType);
    }

    protected AwsAction(AwsToolkitMetricType metricType, String text, ImageDescriptor image) {
        super(text, image);
        this.metricsDataModel = new MetricsDataModel(metricType);
    }

    private final void actionPerformed() {
        metricsDataModel.addAttribute(END_RESULT, PERFORMED);
    }

    protected final void actionSucceeded() {
        metricsDataModel.addAttribute(END_RESULT, SUCCEEDED);
    }

    protected final void actionFailed() {
        metricsDataModel.addAttribute(END_RESULT, FAILED);
    }

    protected final void actionCanceled() {
        metricsDataModel.addAttribute(END_RESULT, CANCELED);
    }

    protected final void actionFinished() {
        metricsDataModel.publishEvent();
    }

    @Override
    public void run() {
        actionPerformed();
        doRun();
    }

    protected abstract void doRun();

    // Helper method to publish a filed action metric immediately
    public static void publishFailedAction(AwsToolkitMetricType metricType) {
        new MetricsDataModel(metricType).addAttribute(END_RESULT, FAILED).publishEvent();
    }

    // Helper method to publish a succeeded action metric immediately
    public static void publishSucceededAction(AwsToolkitMetricType metricType) {
        new MetricsDataModel(metricType).addAttribute(END_RESULT, SUCCEEDED).publishEvent();
    }

    // Helper method to publish a performed action metric immediately
    public static void publishPerformedAction(AwsToolkitMetricType metricType) {
        new MetricsDataModel(metricType).addAttribute(END_RESULT, PERFORMED).publishEvent();
    }
}
