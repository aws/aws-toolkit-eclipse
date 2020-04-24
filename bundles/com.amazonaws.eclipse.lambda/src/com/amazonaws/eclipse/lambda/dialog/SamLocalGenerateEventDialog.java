/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.dialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SaveAsDialog;

import com.amazonaws.eclipse.core.exceptions.AwsActionException;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.telemetry.MetricsDataModel;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.util.CliUtil;
import com.amazonaws.eclipse.core.util.CliUtil.CliProcessTracker;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.launching.SamLocalConstants;
import com.amazonaws.eclipse.lambda.project.wizard.util.FunctionProjectUtil;

/**
 * Dialog for generating Lambda handler event json file using SAM Local.
 */
public class SamLocalGenerateEventDialog extends TitleAreaDialog {
    private final SamLocalLambdaEventDataModel dataModel = new SamLocalLambdaEventDataModel();
    private final DataBindingContext bindingContext;

    private ScrolledComposite scrolledComposite;
    private Composite composite;

    private ComboViewerComplex<SamLocalLambdaEvent> lambdaEventCombo;

    public SamLocalGenerateEventDialog(Shell parentShell) {
        super(parentShell);
        this.bindingContext = new DataBindingContext();
    }

    public SamLocalLambdaEventDataModel getDataModel() {
        return dataModel;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Generate Lambda event template");
        setMessage("Generates Lambda events (e.g. for S3/Kinesis etc) that can be used as the input of the Lambda function.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite rootComposite = (Composite) super.createDialogArea(parent);

        lambdaEventCombo = ComboViewerComplex.<SamLocalLambdaEvent>builder()
                .composite(WizardWidgetFactory.newComposite(rootComposite, 1, 2))
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof SamLocalLambdaEvent) {
                            return ((SamLocalLambdaEvent) element).presentation;
                        }
                        return super.getText(element);
                    }
                })
                .pojoObservableValue(PojoProperties.value(SamLocalLambdaEventDataModel.P_EVENT_TYPE).observe(dataModel))
                .bindingContext(bindingContext)
                .items(Arrays.asList(SamLocalLambdaEvent.values()))
                .defaultItem(SamLocalLambdaEvent.S3)
                .labelValue("Select event type: ")
                .addListeners(e -> onLambdaEventComboSelect())
                .build();

        Group parameterListGroup = WizardWidgetFactory.newGroup(rootComposite, "Parameter List");

        scrolledComposite = new ScrolledComposite(parameterListGroup, SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(scrolledComposite);

        composite = new Composite(scrolledComposite, SWT.None);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
        scrolledComposite.setContent(composite);

        onLambdaEventComboSelect();

        return rootComposite;
    }

    private void onLambdaEventComboSelect() {
        for (Control control : composite.getChildren()) {
            control.dispose();
        }

        SamLocalLambdaEvent eventType = dataModel.getEventType();
        for (SamLocalEventParameter parameter : eventType.parameterList) {
            TextComplex.builder(composite, bindingContext, Observables.observeMapEntry(dataModel.getParameterList(), parameter.name, String.class))
                .defaultValue(parameter.defaultValue)
                .labelValue(parameter.presentation)
                .textMessage(parameter.description)
                .build();
        }
        setMessage(eventType.description);

        Shell shell = composite.getShell();
        shell.setMinimumSize(getInitialSize());
        shell.layout(true, true);
    }

    @Override
    protected void okPressed() {
        SaveAsDialog saveAsDialog = new SaveAsDialog(this.getShell());
        if (saveAsDialog.open() == Window.OK) {
            dataModel.setResultPath(saveAsDialog.getResult());
            generateEventFile();
        }
        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private void generateEventFile() {
        SamLocalLambdaEvent eventType = dataModel.getEventType();
        List<String> builder = new ArrayList<>();
        builder.add(LambdaPlugin.getDefault().getPreferenceStore().getString(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE));
        builder.add("local");
        builder.add("generate-event");
        builder.add(eventType.commandName);

        for (SamLocalEventParameter parameter : eventType.parameterList) {
            String value = (String) dataModel.parameterList.get(parameter.name);
            if (value == null || value.isEmpty()) {
                value = parameter.defaultValue;
            }
            builder.add("--" + parameter.name);
            builder.add(value.replace("\"", "\\\""));
        }

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(dataModel.resultPath.segment(0));
        File outputFile = project.getLocation().append(dataModel.resultPath.removeFirstSegments(1)).toFile();
        MetricsDataModel metricsDataModel = new MetricsDataModel(AwsToolkitMetricType.SAMLOCAL_GENERATE_EVENT);
        metricsDataModel.addAttribute("EventType", eventType.presentation);
        try {
            ByteArrayOutputStream stdErrOutput = new ByteArrayOutputStream();
            CliProcessTracker tracker = CliUtil.executeCommand(builder, Collections.emptyMap(),
                    new FileOutputStream(outputFile), stdErrOutput);
            int exitValue = tracker.waitForStream();
            FunctionProjectUtil.refreshProject(project);

            String stdErr = stdErrOutput.toString();
            if (exitValue != 0 && stdErr != null && !stdErr.isEmpty()) {
                LambdaPlugin.getDefault().reportException("Failed to generate SAM Local event.",
                        new AwsActionException(AwsToolkitMetricType.SAMLOCAL_GENERATE_EVENT.getName(), stdErr, null));
                AwsAction.publishFailedAction(metricsDataModel);
            } else {
                AwsAction.publishSucceededAction(metricsDataModel);
            }
        } catch (IOException e1) {
            LambdaPlugin.getDefault().reportException("Failed to generate event json file.", e1);
            AwsAction.publishFailedAction(metricsDataModel);
        }
    }

    public static class SamLocalLambdaEventDataModel {
        public static final String P_EVENT_TYPE = "eventType";

        private SamLocalLambdaEvent eventType;
        private WritableMap parameterList = new WritableMap(String.class, String.class);
        private IPath resultPath;

        public IPath getResultPath() {
            return resultPath;
        }

        public void setResultPath(IPath resultPath) {
            this.resultPath = resultPath;
        }

        public SamLocalLambdaEvent getEventType() {
            return eventType;
        }

        public void setEventType(SamLocalLambdaEvent eventType) {
            this.eventType = eventType;
        }

        public WritableMap getParameterList() {
            return parameterList;
        }
    }

    public static enum SamLocalLambdaEvent {
        S3("s3", "S3 Event", "Generates a sample Amazon S3 event",
                new SamLocalEventParameter("region", "Region", "The region the event should come from (default: \"us-east-1\")", "us-east-1"),
                new SamLocalEventParameter("bucket", "Bucket", "The S3 bucket the event should reference (default: \"example-bucket\")", "example-bucket"),
                new SamLocalEventParameter("key", "Key", "The S3 key the event should reference (default: \"test/key\")", "test/key")),
        SNS("sns", "SNS Event", "Generates a sample Amazon SNS event",
                new SamLocalEventParameter("message", "Message", "The SNS message body (default: \"example message\")", "example message"),
                new SamLocalEventParameter("topic", "Topic", "The SNS topic (default: \"arn:aws:sns:us-east-1:111122223333:ExampleTopic\")", "arn:aws:sns:us-east-1:111122223333:ExampleTopic"),
                new SamLocalEventParameter("subject", "Subject", "The SNS subject (default: \"example subject\")", "example subject")),
        KINESIS("kinesis", "Kinesis Event", "Generates a sample Amazon Kinesis event",
                new SamLocalEventParameter("region", "AWS Region", "The region the event should come from (default: \"us-east-1\")", "us-east-1"),
                new SamLocalEventParameter("partition", "Partition", "The Kinesis partition key (default: \"partitionKey-03\")", "partitionKey-03"),
                new SamLocalEventParameter("sequence", "Sequence", "The Kinesis sequence number (default: \"49545115243490985018280067714973144582180062593244200961\")", "49545115243490985018280067714973144582180062593244200961"),
                new SamLocalEventParameter("data", "Data", "The Kinesis message payload, with no base64 encoded (default: \"Hello, this is a test 123.\")", "Hello, this is a test 123.")),
        DYNAMODB("dynamodb", "DynamoDB Event", "Generates a sample Amazon DynamoDB event",
                new SamLocalEventParameter("region", "Region", "The region the event should come from (default: \"us-east-1\")", "us-east-1")),
        API("api", "API Gateway Event", "Generates a sample Amazon API Gateway event",
                new SamLocalEventParameter("method", "Method", "HTTP method (default: \"POST\")", "POST"),
                new SamLocalEventParameter("body", "Body", "HTTP body (default: \"{ \"test\": \"body\"}\")", "{ \"test\": \"body\"}"),
                new SamLocalEventParameter("resource", "Resource", "API Gateway resource name (default: \"/{proxy+}\")", "/{proxy+}"),
                new SamLocalEventParameter("path", "Path", "HTTP path (default: \"/examplepath\")", "/examplepath")),
        SCHEDULE("schedule", "Scheduled Event", "Generates a sample scheduled event",
                new SamLocalEventParameter("region", "Region", "The region the event should come from (default: \"us-east-1\")", "us-east-1")),
        ;

        private final String commandName;
        private final String presentation;
        private final String description;
        private final SamLocalEventParameter[] parameterList;

        private SamLocalLambdaEvent(String commandName, String presentation, String description, SamLocalEventParameter... eventParameters) {
            this.commandName = commandName;
            this.presentation = presentation;
            this.description = description;
            parameterList = eventParameters;
        }
    }

    public static class SamLocalEventParameter {
        private final String name;
        private final String presentation;
        private final String description;
        private final String defaultValue;

        public SamLocalEventParameter(String name, String presentation, String description, String defaultValue) {
            this.name = name;
            this.presentation = presentation;
            this.description = description;
            this.defaultValue = defaultValue;
        }
    }
}
