/*
 * Copyright 2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.cloudformation.wizard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.ValidationStatusProvider;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.cloudformation.CloudFormationUtils;
import com.amazonaws.eclipse.cloudformation.CloudFormationUtils.StackSummaryConverter;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.explorer.cloudformation.wizard.CreateStackWizardDataModel.Mode;
import com.amazonaws.eclipse.explorer.sns.CreateTopicDialog;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.services.cloudformation.model.TemplateParameter;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The first page of the stack creation wizard, which prompts for a name and
 * template.
 */
class CreateStackWizardFirstPage extends WizardPage {

    private static final String LOADING_STACKS = "Loading stacks...";
    private static final String OK_MESSAGE = "Provide a name and a template for your new stack.";
    private static final String ESTIMATE_COST_OK_MESSAGE = "Provide a template to esitmate the cost";
    private static final String VALIDATING = "validating";
    private static final String INVALID = "invalid";
    private static final String VALID = "valid";

    /*
     * Data model
     */
    private IObservableValue stackName;
    private IObservableValue templateUrl;
    private IObservableValue templateFile;
    private IObservableValue useTemplateFile;
    private IObservableValue useTemplateUrl;
    private IObservableValue snsTopicArn;
    private IObservableValue notifyWithSNS;
    private IObservableValue timeoutMinutes;
    private IObservableValue rollbackOnFailure;
    private IObservableValue templateValidated = new WritableValue();
    private final DataBindingContext bindingContext = new DataBindingContext();

    private boolean complete = false;
    private ValidateTemplateThread validateTemplateThread;
    private LoadStackNamesThread loadStackNamesThread;
    private Exception templateValidationException;

    private Text fileTemplateText;
    private Text templateURLText;

    private CreateStackWizard wizard;

    protected CreateStackWizardFirstPage(CreateStackWizard createStackWizard) {
        super("");
        wizard = createStackWizard;
        if (wizard.getDataModel().getMode() == Mode.EstimateCost) {
            setMessage(ESTIMATE_COST_OK_MESSAGE);
        } else {
            setMessage(OK_MESSAGE);
        }

        stackName = PojoObservables.observeValue(wizard.getDataModel(), "stackName");
        templateUrl = PojoObservables.observeValue(wizard.getDataModel(), "templateUrl");
        templateFile = PojoObservables.observeValue(wizard.getDataModel(), "templateFile");
        useTemplateFile = PojoObservables.observeValue(wizard.getDataModel(), "useTemplateFile");
        useTemplateUrl = PojoObservables.observeValue(wizard.getDataModel(), "useTemplateUrl");
        notifyWithSNS = PojoObservables.observeValue(wizard.getDataModel(), "notifyWithSNS");
        snsTopicArn = PojoObservables.observeValue(wizard.getDataModel(), "snsTopicArn");
        timeoutMinutes = PojoObservables.observeValue(wizard.getDataModel(), "timeoutMinutes");
        rollbackOnFailure = PojoObservables.observeValue(wizard.getDataModel(), "rollbackOnFailure");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt
     * .widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        final Composite comp = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(comp);

        // Unfortunately, we have to manually adjust for field decorations
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        int fieldDecorationWidth = fieldDecoration.getImage().getBounds().width;
        if (wizard.getDataModel().getMode() != Mode.EstimateCost) {
        createStackNameControl(comp, fieldDecorationWidth);
        }
        createTemplateSelectionControl(comp, fieldDecorationWidth);

        // Some fields are only for creation, not update
        if ( wizard.getDataModel().getMode() == Mode.Create ) {
            createSNSTopicControl(comp, fieldDecorationWidth);
            createTimeoutControl(comp);
            createRollbackControl(comp);
        }

        setUpValidation(comp);

        // Set initial values for radio buttons
        useTemplateFile.setValue(true);
        templateValidated.setValue(null);
        // If we already have a file template filled in, validate it
        if ( wizard.getDataModel().isUsePreselectedTemplateFile() ) {
            validateTemplateFile((String) templateFile.getValue());
        }

        setControl(comp);
    }

    private void createStackNameControl(final Composite comp, int fieldDecorationWidth) {
        // Whether the user already set the stack name.
        boolean stackNameExists = false;
        new Label(comp, SWT.READ_ONLY).setText("Stack Name: ");
        Control stackNameControl = null;
        if ( wizard.getDataModel().getMode() == Mode.Create ) {
            Text stackNameText = new Text(comp, SWT.BORDER);
            bindingContext.bindValue(SWTObservables.observeText(stackNameText, SWT.Modify), stackName)
                    .updateTargetToModel();
            stackNameControl = stackNameText;
        } else {
            Combo combo = new Combo(comp, SWT.READ_ONLY | SWT.DROP_DOWN);

            if (stackName.getValue() != null) {
                combo.setItems(new String[] { (String)stackName.getValue() });
                stackNameExists = true;
            } else {
            combo.setItems(new String[] { LOADING_STACKS });
            }

            combo.select(0);
            bindingContext.bindValue(SWTObservables.observeSelection(combo), stackName).updateTargetToModel();
            stackNameControl = combo;

            stackName.addChangeListener(new IChangeListener() {
                @Override
                public void handleChange(ChangeEvent event) {
                    if ( (Boolean) useTemplateFile.getValue() ) {
                        validateTemplateFile((String) templateFile.getValue());
                    } else {
                        validateTemplateUrl((String) templateUrl.getValue());
                    }
                }
            });

            if (!stackNameExists) {
            loadStackNames(combo);
            }
        }

        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).indent(fieldDecorationWidth, 0)
                .applyTo(stackNameControl);
        ChainValidator<String> stackNameValidationStatusProvider = new ChainValidator<>(stackName,
                new NotEmptyValidator("Please provide a stack name"));
        bindingContext.addValidationStatusProvider(stackNameValidationStatusProvider);
        addStatusDecorator(stackNameControl, stackNameValidationStatusProvider);
    }

    /**
     * Loads the names of all the stacks asynchronously.
     */
    private void loadStackNames(Combo combo) {
        CancelableThread.cancelThread(loadStackNamesThread);
        templateValidated.setValue(VALIDATING);
        loadStackNamesThread = new LoadStackNamesThread(combo);
        loadStackNamesThread.start();
    }

    private final class LoadStackNamesThread extends CancelableThread {

        private Combo combo;

        private LoadStackNamesThread(Combo combo) {
            super();
            this.combo = combo;
        }

        @Override
        public void run() {
            final List<String> stackNames = CloudFormationUtils.listExistingStacks(
                    new StackSummaryConverter<String>() {
                        @Override
                        public String convert(StackSummary stack) {
                            return stack.getStackName();
                        }
                    });

            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized ( this ) {
                            if (!isCanceled()) {
                                combo.setItems(stackNames.toArray(new String[stackNames.size()]));
                                combo.select(0);
                                templateValidated.setValue(VALID);
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });

        }
    }

    private void createTemplateSelectionControl(final Composite comp, int fieldDecorationWidth) {
        Group stackTemplateSourceGroup = new Group(comp, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(3, 1).indent(fieldDecorationWidth, 0)
                .applyTo(stackTemplateSourceGroup);
        GridLayoutFactory.fillDefaults().numColumns(3).applyTo(stackTemplateSourceGroup);
        stackTemplateSourceGroup.setText("Stack Template Source:");

        createTemplateFileControl(stackTemplateSourceGroup, fieldDecorationWidth);
        createTemplateUrlControl(stackTemplateSourceGroup, fieldDecorationWidth);
    }

    private void createTemplateUrlControl(Group stackTemplateSourceGroup, int fieldDecorationWidth) {
        final Button templateUrlOption = new Button(stackTemplateSourceGroup, SWT.RADIO);
        templateUrlOption.setText("Template URL: ");
        templateURLText = new Text(stackTemplateSourceGroup, SWT.BORDER);
        templateURLText.setEnabled(false);
        GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(templateURLText);
        Link link = new Link(stackTemplateSourceGroup, SWT.None);

        // TODO: this should really live in the regions file, not hardcoded here
        Region currentRegion = RegionUtils.getCurrentRegion();

        final String sampleUrl = String.format(
                "http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-sample-templates-%s.html",
                currentRegion.getId());

        link.setText("<a href=\"" +
                sampleUrl +
                "\">Browse for samples</a>");
        link.addListener(SWT.Selection, new WebLinkListener());

        bindingContext.bindValue(SWTObservables.observeText(templateURLText, SWT.Modify), templateUrl)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeSelection(templateUrlOption), useTemplateUrl)
                .updateTargetToModel();
        ChainValidator<String> templateUrlValidationStatusProvider = new ChainValidator<>(templateUrl,
                useTemplateUrl, new NotEmptyValidator("Please provide a valid URL for your template"));
        bindingContext.addValidationStatusProvider(templateUrlValidationStatusProvider);
        addStatusDecorator(templateURLText, templateUrlValidationStatusProvider);

        templateUrlOption.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selected = templateUrlOption.getSelection();
                templateURLText.setEnabled(selected);
            }
        });
    }

    private void createTemplateFileControl(Group stackTemplateSourceGroup, int fieldDecorationWidth) {
        Button fileTemplateOption = new Button(stackTemplateSourceGroup, SWT.RADIO);
        fileTemplateOption.setText("Template File: ");
        fileTemplateOption.setSelection(true);

        fileTemplateText = new Text(stackTemplateSourceGroup, SWT.BORDER | SWT.READ_ONLY);

        GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(fileTemplateText);
        Button browseButton = new Button(stackTemplateSourceGroup, SWT.PUSH);
        browseButton.setText("Browse...");
        Listener fileTemplateSelectionListener = new Listener() {

            @Override
            public void handleEvent(Event event) {
                if ( (Boolean) useTemplateFile.getValue() ) {
                    FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
                    String result = dialog.open();
                    if ( result != null ) {
                        fileTemplateText.setText(result);
                    }
                }
            }
        };
        browseButton.addListener(SWT.Selection, fileTemplateSelectionListener);
        fileTemplateText.addListener(SWT.MouseUp, fileTemplateSelectionListener);

        bindingContext.bindValue(SWTObservables.observeSelection(fileTemplateOption), useTemplateFile)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeText(fileTemplateText, SWT.Modify), templateFile)
                .updateTargetToModel();
        ChainValidator<String> templateFileValidationStatusProvider = new ChainValidator<>(templateFile,
                useTemplateFile, new NotEmptyValidator("Please provide a valid file for your template"));
        bindingContext.addValidationStatusProvider(templateFileValidationStatusProvider);
        addStatusDecorator(fileTemplateText, templateFileValidationStatusProvider);
    }

    private void createSNSTopicControl(final Composite comp, int fieldDecorationWidth) {
        final Button notifyWithSNSButton = new Button(comp, SWT.CHECK);
        notifyWithSNSButton.setText("SNS Topic (Optional):");
        final Combo snsTopicCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridDataFactory.fillDefaults().grab(true, false).indent(fieldDecorationWidth, 0).applyTo(snsTopicCombo);
        loadTopics(snsTopicCombo);
        bindingContext.bindValue(SWTObservables.observeSelection(notifyWithSNSButton), notifyWithSNS)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeSelection(snsTopicCombo), snsTopicArn).updateTargetToModel();
        ChainValidator<String> snsTopicValidationStatusProvider = new ChainValidator<>(snsTopicArn,
                notifyWithSNS, new NotEmptyValidator("Please select an SNS notification topic"));
        bindingContext.addValidationStatusProvider(snsTopicValidationStatusProvider);
        addStatusDecorator(snsTopicCombo, snsTopicValidationStatusProvider);

        final Button newTopicButton = new Button(comp, SWT.PUSH);
        newTopicButton.setText("Create New Topic");
        newTopicButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                CreateTopicDialog dialog = new CreateTopicDialog();
                if ( dialog.open() == 0 ) {
                    try {
                        AwsToolkitCore.getClientFactory().getSNSClient()
                                .createTopic(new CreateTopicRequest().withName(dialog.getTopicName()));
                    } catch ( Exception ex ) {
                        CloudFormationPlugin.getDefault().logError("Failed to create new topic", ex);
                    }
                    loadTopics(snsTopicCombo);
                }
            }
        });

        snsTopicCombo.setEnabled(false);
        newTopicButton.setEnabled(false);
        notifyWithSNSButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selection = notifyWithSNSButton.getSelection();
                snsTopicCombo.setEnabled(selection);
                newTopicButton.setEnabled(selection);
            }
        });
    }

    private void createTimeoutControl(final Composite comp) {
        new Label(comp, SWT.None).setText("Creation Timeout:");
        Combo timeoutCombo = new Combo(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridDataFactory.fillDefaults().grab(false, false).span(2, 1).applyTo(timeoutCombo);
        timeoutCombo.setItems(new String[] { "None", "5 minutes", "10 minutes", "15 minutes", "20 minutes",
                "30 minutes", "60 minutes", "90 minutes", });
        timeoutCombo.select(0);
        UpdateValueStrategy timeoutUpdateStrategy = new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE);
        timeoutUpdateStrategy.setConverter(new IConverter() {

            @Override
            public Object getToType() {
                return Integer.class;
            }

            @Override
            public Object getFromType() {
                return String.class;
            }

            @Override
            public Object convert(Object fromObject) {
                String value = (String) fromObject;
                if ( "None".equals(value) ) {
                    return 0;
                } else {
                    String minutes = value.substring(0, value.indexOf(' '));
                    return Integer.parseInt(minutes);
                }
            }
        });
        bindingContext.bindValue(SWTObservables.observeSelection(timeoutCombo), timeoutMinutes, timeoutUpdateStrategy,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER)).updateTargetToModel();
    }

    private void createRollbackControl(final Composite comp) {
        Button rollbackButton = new Button(comp, SWT.CHECK);
        rollbackButton.setText("Rollback on Failure");
        GridDataFactory.fillDefaults().grab(false, false).span(3, 1).applyTo(rollbackButton);
        rollbackButton.setSelection(true);
        bindingContext.bindValue(SWTObservables.observeSelection(rollbackButton), rollbackOnFailure)
                .updateTargetToModel();
    }

    private void setUpValidation(final Composite comp) {

        // Change listeners to re-validate the template whenever
        // the customer changes whether to use a file or a URL
        templateUrl.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                if ( ((String) templateUrl.getValue()).length() > 0 ) {
                    validateTemplateUrl((String) templateUrl.getValue());
                }
            }
        });
        useTemplateUrl.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                if ( (Boolean) useTemplateUrl.getValue() && ((String) templateUrl.getValue()).length() > 0 ) {
                    validateTemplateUrl((String) templateUrl.getValue());
                }
            }
        });

        templateFile.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                validateTemplateFile((String) templateFile.getValue());
            }
        });
        useTemplateFile.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                if ( (Boolean) useTemplateFile.getValue() ) {
                    validateTemplateFile((String) templateFile.getValue());
                }
            }
        });

        // Status validator for template validation, which occurs out of band
        IValidator templateValidator = new IValidator() {

            @Override
            public IStatus validate(Object value) {
                if ( value == null ) {
                    return ValidationStatus.error("No template selected");
                }

                if ( ((String) value).equals(VALID) ) {
                    return ValidationStatus.ok();
                } else if ( ((String) value).equals(VALIDATING) ) {
                    return ValidationStatus.warning("Validating template...");
                } else if ( ((String) value).equals(INVALID) ) {
                    if ( templateValidationException != null ) {
                        return ValidationStatus.error("Invalid template: " + templateValidationException.getMessage());
                    } else {
                        return ValidationStatus.error("No template selected");
                    }
                }

                return ValidationStatus.ok();
            }
        };
        bindingContext.addValidationStatusProvider(new ChainValidator<String>(templateValidated, templateValidator));

        // Also hook up this template validator to the two template fields
        // conditionally
        addStatusDecorator(fileTemplateText, new ChainValidator<String>(templateValidated, useTemplateFile,
                templateValidator));
        addStatusDecorator(templateURLText, new ChainValidator<String>(templateValidated, useTemplateUrl,
                templateValidator));

        // Finally provide aggregate status reporting for the entire wizard page
        final AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
                AggregateValidationStatus.MAX_SEVERITY);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                if ( status.isOK() ) {
                    setErrorMessage(null);
                    if (wizard.getDataModel().getMode() == Mode.EstimateCost) {
                        setMessage(ESTIMATE_COST_OK_MESSAGE, Status.OK);
                    } else {
                        setMessage(OK_MESSAGE, Status.OK);
                    }
                } else if (status.getSeverity() == Status.WARNING) {
                    setErrorMessage(null);
                    setMessage(status.getMessage(), Status.WARNING);
                } else if (status.getSeverity() == Status.ERROR) {
                    setErrorMessage(status.getMessage());
                }

                setComplete(status.isOK());
            }
        });
    }

    /**
     * Adds a control status decorator for the control given.
     */
    private void addStatusDecorator(final Control control, ValidationStatusProvider validationStatusProvider) {
        ControlDecoration decoration = new ControlDecoration(control, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText("Invalid value");
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());
    }

    @Override
    public boolean isPageComplete() {
        return complete;
    }

    private void setComplete(boolean complete) {
        this.complete = complete;
        if ( getWizard().getContainer() != null && getWizard().getContainer().getCurrentPage() != null )
            getWizard().getContainer().updateButtons();
    }

    /**
     * Loads all SNS topics into the dropdown given
     */
    private void loadTopics(final Combo snsTopicCombo) {
        snsTopicCombo.setItems(new String[] { "Loading..." });
        new Thread() {

            @Override
            public void run() {
                AmazonSNS sns = AwsToolkitCore.getClientFactory().getSNSClient();
                ListTopicsResult topicsResult = sns.listTopics();
                final List<String> arns = new ArrayList<>();
                for ( Topic topic : topicsResult.getTopics() ) {
                    arns.add(topic.getTopicArn());
                }
                Display.getDefault().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        if ( !snsTopicCombo.isDisposed() ) {
                            snsTopicCombo.setItems(arns.toArray(new String[arns.size()]));
                        }
                    }
                });
            }

        }.start();
    }

    /**
     * Validates the template file given in a separate thread.
     */
    private void validateTemplateFile(String filePath) {
        CancelableThread.cancelThread(validateTemplateThread);
        templateValidated.setValue(VALIDATING);
        try {
            String fileContents = FileUtils.readFileToString(new File(filePath), "UTF8");
            validateTemplateThread = new ValidateTemplateThread(
                    new ValidateTemplateRequest().withTemplateBody(fileContents));
            validateTemplateThread.start();
        } catch ( Exception e ) {
            templateValidated.setValue(INVALID);
            templateValidationException = e;
        }
    }

    /**
     * Validates the template url given in a separate thread.
     */
    private void validateTemplateUrl(String url) {
        CancelableThread.cancelThread(validateTemplateThread);
        templateValidated.setValue(VALIDATING);
        validateTemplateThread = new ValidateTemplateThread(new ValidateTemplateRequest().withTemplateURL(url));
        validateTemplateThread.start();
    }

    /**
     * Cancelable thread to validate a template and update the validation
     * status.
     */
    private final class ValidateTemplateThread extends CancelableThread {

        private final ValidateTemplateRequest rq;

        private ValidateTemplateThread(ValidateTemplateRequest rq) {
            this.rq = rq;
        }

        @Override
        public void run() {
            ValidateTemplateResult validateTemplateResult;
            Stack existingStack = null;

            Map templateMap;
            try {

                // TODO: region should come from context for file-based actions
                AmazonCloudFormation cf = getCloudFormationClient();
                validateTemplateResult = cf.validateTemplate(rq);

                if ( wizard.getDataModel().getMode() == Mode.Update && wizard.getDataModel().getStackName() != LOADING_STACKS ) {
                    DescribeStacksResult describeStacks = cf.describeStacks(new DescribeStacksRequest()
                            .withStackName(wizard.getDataModel().getStackName()));
                    if ( describeStacks.getStacks().size() == 1 ) {
                        existingStack = describeStacks.getStacks().iterator().next();
                    }
                }

                String templateBody = null;
                if ( rq.getTemplateBody() != null ) {
                    templateBody = rq.getTemplateBody();
                } else {
                    InputStream in = new URL(rq.getTemplateURL()).openStream();
                    try {
                        templateBody = IOUtils.toString(in);
                    } finally {
                        IOUtils.closeQuietly(in);
                    }
                }

                templateMap = parseTemplate(templateBody);
                wizard.getDataModel().setTemplateBody(templateBody);

            } catch ( Exception e ) {
                templateValidationException = e;
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        synchronized ( this ) {
                            if ( !isCanceled() ) {
                                templateValidated.setValue(INVALID);
                            }
                        }
                    }
                });
                setRunning(false);
                return;
            }

            final List<TemplateParameter> templateParams = validateTemplateResult.getParameters();
            final Map templateJson = templateMap;
            final List<String> requiredCapabilities = validateTemplateResult.getCapabilities();
            final Stack stack = existingStack;

            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized ( this ) {
                            if ( !isCanceled() ) {
                                wizard.getDataModel().getParametersDataModel().setTemplateParameters(templateParams);
                                wizard.setNeedsSecondPage(!templateParams.isEmpty());
                                wizard.getDataModel().getParametersDataModel().setTemplate(templateJson);
                                wizard.getDataModel().setRequiredCapabilities(requiredCapabilities);
                                if ( stack != null ) {
                                    for ( Parameter param : stack.getParameters() ) {
                                        boolean noEcho = false;

                                        // This is a pain, but any "noEcho" parameters get returned as asterisks in the service response.
                                        // The customer must fill these values out again, even for a running stack.
                                        for ( TemplateParameter templateParam : wizard.getDataModel().getParametersDataModel().getTemplateParameters() ) {
                                            if (templateParam.getNoEcho() && templateParam.getParameterKey().equals(param.getParameterKey())) {
                                                noEcho = true;
                                                break;
                                            }
                                        }

                                        if ( !noEcho ) {
                                            wizard.getDataModel().getParametersDataModel().getParameterValues()
                                                    .put(param.getParameterKey(), param.getParameterValue());
                                        }
                                    }
                                }
                                templateValidated.setValue(VALID);
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }
    }

    /**
     * Parses the (already validated) template given and returns a map of its
     * structure.
     */
    private Map parseTemplate(String templateBody) throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(templateBody, Map.class);
    }

    /**
     * @return
     */
    private AmazonCloudFormation getCloudFormationClient() {
        AmazonCloudFormation cf = AwsToolkitCore.getClientFactory().getCloudFormationClient();
        return cf;
    }

    /**
     * Whether we can flip to the next page depends on whether there's one to go to.
     */
    @Override
    public boolean canFlipToNextPage() {
        return (wizard.needsSecondPage() || wizard.needsThirdPage()) && super.canFlipToNextPage();
    }

}