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
package com.amazonaws.eclipse.lambda.launching;

import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_ACTION;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_CODE_URI;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_DEBUG_PORT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_ENV_VARS;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_EVENT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_HOST;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_LAMBDA_IDENTIFIER;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_MAVEN_GOALS;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PORT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PROFILE;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_PROJECT;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_REGION;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_SAM;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_TEMPLATE;
import static com.amazonaws.eclipse.lambda.launching.SamLocalConstants.A_TIME_OUT;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.AccountSelectionComposite;
import com.amazonaws.eclipse.core.ui.ImportFileComposite;
import com.amazonaws.eclipse.core.ui.RegionComposite;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.util.PluginUtils;
import com.amazonaws.eclipse.core.validator.FilePathValidator;
import com.amazonaws.eclipse.core.validator.IntegerRangeValidator;
import com.amazonaws.eclipse.core.validator.WorkspacePathValidator;
import com.amazonaws.eclipse.core.widget.ComboViewerComplex;
import com.amazonaws.eclipse.core.widget.TextComplex;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.lambda.LambdaConstants;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.dialog.SamLocalGenerateEventDialog;
import com.amazonaws.eclipse.lambda.dialog.SamLocalGenerateEventDialog.SamLocalLambdaEventDataModel;
import com.amazonaws.eclipse.lambda.preferences.SamLocalPreferencePage;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel.SamAction;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel.SamLocalInvokeFunctionDataModel;
import com.amazonaws.eclipse.lambda.project.wizard.model.RunSamLocalDataModel.SamLocalStartApiDataModel;
import com.amazonaws.eclipse.lambda.serverless.model.transform.ServerlessModel;
import com.amazonaws.eclipse.lambda.serverless.validator.ServerlessTemplateFilePathValidator;

public class SamLocalTab extends AbstractLaunchConfigurationTab {
    private final RunSamLocalDataModel dataModel = new RunSamLocalDataModel();

    private final SamLocalInvokeFunctionDataModel invokeDataModel = new SamLocalInvokeFunctionDataModel();
    private final SamLocalStartApiDataModel startApiDataModel = new SamLocalStartApiDataModel();

    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    // UI
    // AWS specific UI
    private AccountSelectionComposite accountComposite;
    private RegionComposite regionComposite;

    // SAM Local UI for common settings
    private ImportFileComposite workspaceComposite;
    private TextComplex mavenGoalsComplex;
    private TextComplex samLocalExecutableComplex;
    private ImportFileComposite templateFileComposite;
    private ImportFileComposite envvarFileComposite;
    private TextComplex debugPortComplex;

    // UI for Lambda function settings
    private TextComplex codeUriComplex;
    private TextComplex timeoutComplex;

    private ComboViewerComplex<SamAction> samCommandsComboViewer;
    private Group samCommandGroup;

    // SAM Local invoke specific UI
    private ComboViewerComplex<String> lambdaPhysicalIdCombo;
    private ImportFileComposite eventFileComposite;

    // SAM Local start-api specific UI
    private TextComplex hostComplex;
    private TextComplex portComplex;

    private ExpandableComposite advancedSettingsExpandable;

    public SamLocalTab() {
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        setErrorMessage(null);

        IStatus status = getValidationStatus();
        if (status == null) {
            return true;
        }

        if (status.getSeverity() != IStatus.OK) {
            setErrorMessage(status.getMessage());
            return false;
        }
        return true;
    }

    private void entriesChanges() {
        setDirty(true);
        updateLaunchConfigurationDialog();
    }

    private IStatus getValidationStatus() {
        if (aggregateValidationStatus == null) return null;
        Object value = aggregateValidationStatus.getValue();
        if (!(value instanceof IStatus)) return null;
        return (IStatus)value;
    }

    private void createAwsScopeGroup(Composite composite) {
        Group group = WizardWidgetFactory.newGroup(composite, "AWS Configuration");
        accountComposite = new AccountSelectionComposite(group, SWT.NONE);
        accountComposite.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dataModel.setAccount(AwsToolkitCore.getDefault().getAccountManager()
                        .getAccountInfo(accountComposite.getSelectedAccountId()));
                entriesChanges();
            }
        });
        regionComposite = RegionComposite.builder()
                .parent(group)
                .bindingContext(bindingContext)
                .dataModel(dataModel.getRegionDataModel())
                .serviceName(ServiceAbbreviations.LAMBDA)
                .addListener(e -> entriesChanges())
                .build();
    }

    private void createAdvancedSettingsGroup(Composite composite) {
        Group group = WizardWidgetFactory.newGroup(composite, "SAM Local Configuration");

        mavenGoalsComplex = TextComplex.builder(WizardWidgetFactory.newComposite(group, 1, 2, false),
                    bindingContext, PojoProperties.value(RunSamLocalDataModel.P_MAVEN_GOALS).observe(dataModel))
                .defaultValue(dataModel.getMavenGoals())
                .labelValue("Maven goals: ")
                .addValidator(new NotEmptyValidator("Maven goals must not be empty!"))
                .modifyListener(e -> entriesChanges())
                .textMessage("Maven goals for generating the uber jar of your Lambda project.")
                .build();

        Composite samLocalRuntimeComposite = WizardWidgetFactory.newComposite(group, 1, 3);
        samLocalExecutableComplex = TextComplex.builder(
            samLocalRuntimeComposite, bindingContext, PojoProperties.value(
                    RunSamLocalDataModel.P_SAM_RUNTIME).observe(dataModel))
            .createLabel(true)
            .labelValue("SAM runtime: ")
            .addValidator(new FilePathValidator("SAM runtime"))
            .textMessage("AWS SAM Local executable path.")
            .modifyListener(e -> entriesChanges())
            .build();

        // Only disable this Text widget but not the validation.
        samLocalExecutableComplex.getText().setEditable(false);

        Link link = new Link(samLocalRuntimeComposite, SWT.NONE);
        link.setText("<A>Configure AWS SAM Local...</A>");
        link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
                        SamLocalPreferencePage.ID, new String[] { SamLocalPreferencePage.ID }, null).open();
                samLocalExecutableComplex.setText(LambdaPlugin.getDefault().getPreferenceStore().getString(
                        SamLocalConstants.P_SAM_LOCAL_EXECUTABLE));
            }
        });

        debugPortComplex = TextComplex.builder(WizardWidgetFactory.newComposite(group, 1, 2, false),
                    bindingContext, PojoProperties.value(RunSamLocalDataModel.P_DEBUG_PORT).observe(dataModel))
                .defaultValue(String.valueOf(dataModel.getDebugPort()))
                .labelValue("Debug port: ")
                .addValidator(new IntegerRangeValidator("Debug port", 1024, 65535))
                .modifyListener(e -> entriesChanges())
                .textMessage("When specified, Lambda function container will start in debug mode and will expose this port on localhost.")
                .build();

        envvarFileComposite = ImportFileComposite.builder(group, bindingContext, dataModel.getEnvvarFileLocationDataModel())
                .textLabel("Env vars: ")
                .filePathValidator(new WorkspacePathValidator("Env vars file", true))
                .modifyListener(e -> entriesChanges())
                .textMessage("JSON file containing values for Lambda function's environment variables.")
                .buildWorkspaceFileBrowser();
    }

    private void createLambdaFunctionGroup(Composite composite) {
        Group group = WizardWidgetFactory.newGroup(composite, "Lambda Function Configuration");

        codeUriComplex = TextComplex.builder(WizardWidgetFactory.newComposite(group, 1, 2, false),
                bindingContext, PojoProperties.value(RunSamLocalDataModel.P_CODE_URI).observe(dataModel))
            .labelValue("Code URI: ")
            .modifyListener(e -> entriesChanges())
            .textMessage("Location to the function code as a Lambda deployment package.")
            .build();

        timeoutComplex = TextComplex.builder(WizardWidgetFactory.newComposite(group, 1, 2, false),
                bindingContext, PojoProperties.value(RunSamLocalDataModel.P_TIME_OUT).observe(dataModel))
            .defaultValue(String.valueOf(RunSamLocalDataModel.DEFAULT_TIME_OUT))
            .addValidator(new IntegerRangeValidator("Lambda function timeout", 0, 900))
            .labelValue("Timeout (secs): ")
            .modifyListener(e -> entriesChanges())
            .textMessage("Lambda function execution time (in seconds) after which Lambda terminates the function.")
            .build();
    }

    private void createSamLocalConfigSection(Composite composite) {
        Group group = WizardWidgetFactory.newGroup(composite, "SAM Local Configuration");

        WizardWidgetFactory.newLink(
                group,
                LambdaConstants.webLinkListener,
                "AWS Toolkit for Eclipse is using <a href=\""
                        + SamLocalConstants.LINKS_SAM_LOCAL_ANNOUNCEMENT
                        + "\">AWS SAM Local</a> for locally debugging your Lambda function. You need to preinstall <a href=\""
                        + SamLocalConstants.LINKS_INSTALL_SAM_LOCAL
                        + "\">Docker and SAM</a> for this feature.", 1, 100, 30);

        workspaceComposite = ImportFileComposite.builder(group, bindingContext, dataModel.getWorkspaceDataModel())
                .textLabel("Project:")
                .filePathValidator(new NotEmptyValidator("Project must be specified!"))
                .modifyListener(e -> {
                    entriesChanges();
                    onProjectSelectChanged();
                })
                .textMessage("Target SAM project which must be a Maven project.")
                .buildWorkspaceProjectBrowser();

        templateFileComposite = ImportFileComposite.builder(group, bindingContext, dataModel.getTemplateFileLocationDataModel())
                .textLabel("Template:")
                .filePathValidator(new ServerlessTemplateFilePathValidator())
                .modifyListener(e -> {
                    entriesChanges();
                    onSamLocalTemplateFileChanged();
                })
                .textMessage("AWS SAM template file (default: \"serverless.[template|json], sam.[template|json]\")")
                .buildWorkspaceFileBrowser();

        samCommandsComboViewer = ComboViewerComplex.<SamAction>builder()
                .composite(WizardWidgetFactory.newComposite(group, 1, 2, false))
                .bindingContext(bindingContext)
                .pojoObservableValue(PojoProperties.value(RunSamLocalDataModel.P_SAM_ACTION).observe(dataModel))
                .addListeners((e) -> {
                    onSamCommandsComboViewerSelect();
                    entriesChanges();
                })
                .items(SamAction.toList())
                .labelValue("Run as: ")
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof SamAction) {
                            SamAction samAction = (SamAction) element;
                            return samAction.getDescription();
                        } else {
                            return super.getText(element);
                        }
                    }
                })
                .build();
    }

    private void onSamCommandsComboViewerSelect() {
        switch (dataModel.getSamAction()) {
        case INVOKE:
            dataModel.setActionDataModel(invokeDataModel);
            createInvokeSection();
            break;
        case START_API:
            dataModel.setActionDataModel(startApiDataModel);
            createStartApiSection();
            break;
        }
    }

    private void onProjectSelectChanged() {
        String projectName = dataModel.getWorkspaceDataModel().getFilePath();
        String codeUri = SamLocalPathFinder.findCodeUri(projectName);
        codeUriComplex.setText(codeUri);
    }

    private void onSamLocalTemplateFileChanged() {
        if (lambdaPhysicalIdCombo == null) {
            return;
        }

        Collection<String> data = getLambdaFunctionPhysicalIDs();
        lambdaPhysicalIdCombo.getComboViewer().setInput(data);
        if (!data.isEmpty()) {
            lambdaPhysicalIdCombo.selectItem(data.iterator().next());
        }
        lambdaPhysicalIdCombo.getComboViewer().refresh();
    }

    private static final String UNAVAILABLE_PHYSICAL_ID = "Lambda function not found...";

    private Collection<String> getLambdaFunctionPhysicalIDs() {
        ServerlessTemplateFilePathValidator validator = new ServerlessTemplateFilePathValidator();
        try {
            ServerlessModel model = validator.validateFilePath(dataModel.getTemplateFileLocationDataModel().getFilePath());
            return model.getServerlessFunctions().keySet();
        } catch (Exception e) {
            return Arrays.asList(UNAVAILABLE_PHYSICAL_ID);
        }
    }

    private void createSamLocalActionSection(Composite parent) {
        samCommandGroup = WizardWidgetFactory.newGroup(parent, "SAM Local Command Configuration");
        onSamCommandsComboViewerSelect();
    }

    private void createInvokeSection() {
        for (Control control : samCommandGroup.getChildren()) {
            control.dispose();
        }

        hostComplex = null;
        portComplex = null;

        lambdaPhysicalIdCombo = ComboViewerComplex.<String>builder()
                .bindingContext(bindingContext)
                .composite(WizardWidgetFactory.newComposite(samCommandGroup, 1, 2, false))
                .labelValue("Function identifier: ")
                .labelProvider(new LabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof String) {
                            String text = (String) element;
                            return text;
                        }
                        return super.getText(element);
                    }
                })
                .pojoObservableValue(PojoProperties.value(invokeDataModel.P_LAMBDA_IDENTIFIER).observe(invokeDataModel))
                .build();
        onSamLocalTemplateFileChanged();

        Composite composite = WizardWidgetFactory.newComposite(samCommandGroup, 1, 2, false);
        eventFileComposite = ImportFileComposite.builder(composite, bindingContext, invokeDataModel.getEventFileLocationDataModel())
                .textLabel("Event:")
                .filePathValidator(new WorkspacePathValidator("Event", false))
                .modifyListener(e -> entriesChanges())
                .textMessage("JSON file containing event data passed to the Lambda function during invoke")
                .buildWorkspaceFileBrowser();
        Button generateEventButton = WizardWidgetFactory.newPushButton(composite, "Generate", 1);
        generateEventButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                SamLocalGenerateEventDialog generateEventDialog = new SamLocalGenerateEventDialog(getShell());
                int returnValue = generateEventDialog.open();
                if (returnValue == Window.OK) {
                    SamLocalLambdaEventDataModel dataModel = generateEventDialog.getDataModel();
                    IPath resultPath = dataModel.getResultPath();
                    eventFileComposite.setFilePath(PluginUtils.variablePluginGenerateWorkspacePath(resultPath));
                }
            }
        });
        samCommandGroup.layout();
    }

    private void createStartApiSection() {
        for (Control control : samCommandGroup.getChildren()) {
            control.dispose();
        }

        lambdaPhysicalIdCombo = null;
        eventFileComposite = null;

        Composite composite = WizardWidgetFactory.newComposite(samCommandGroup, 1, 2);
        hostComplex = TextComplex.builder(composite, bindingContext, PojoProperties.value(SamLocalStartApiDataModel.P_HOST).observe(startApiDataModel))
                .defaultValue(startApiDataModel.getHost())
                .labelValue("Host:")
                .textMessage("Local hostname or IP address to bind to (default: \"127.0.0.1\")")
                .modifyListener(e -> entriesChanges())
                .build();

        portComplex = TextComplex.builder(composite, bindingContext, PojoProperties.value(SamLocalStartApiDataModel.P_PORT).observe(startApiDataModel))
                .defaultValue(String.valueOf(startApiDataModel.getPort()))
                .labelValue("Port:")
                .textMessage("Local port number to listen on (default: \"3000\")")
                .modifyListener(e -> entriesChanges())
                .build();

        samCommandGroup.layout();
    }

    @Override
    public void createControl(Composite parent) {
        Composite rootComposite = WizardWidgetFactory.newComposite(parent, 1, 1);
        setControl(rootComposite);

        rootComposite.setLayout(new GridLayout(1, false));

        createSamLocalConfigSection(rootComposite);
        createSamLocalActionSection(rootComposite);

        createAdvancedSettingSection(rootComposite);
        dataModel.setAccount(AwsToolkitCore.getDefault().getAccountManager()
                .getAccountInfo(accountComposite.getSelectedAccountId()));
    }

    private void createAdvancedSettingSection(final Composite parent) {
        advancedSettingsExpandable = new ExpandableComposite(parent, SWT.NONE,
                ExpandableComposite.TWISTIE | ExpandableComposite.COMPACT | ExpandableComposite.EXPANDED);
        advancedSettingsExpandable.setText("Advanced Settings");
        advancedSettingsExpandable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Composite expandableComposite = WizardWidgetFactory.newComposite(advancedSettingsExpandable, 1, 1);
        advancedSettingsExpandable.setClient(expandableComposite);
        advancedSettingsExpandable.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                Shell shell = parent.getShell();
                shell.layout(true, true);
            }
        });

        createAwsScopeGroup(expandableComposite);
        createAdvancedSettingsGroup(expandableComposite);
        createLambdaFunctionGroup(expandableComposite);
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        accountComposite.selectAccountName(getAttribute(configuration, A_PROFILE, RunSamLocalDataModel.DEFAULT_PROFILE));
        dataModel.setAccount(AwsToolkitCore.getDefault().getAccountManager()
                .getAccountInfo(accountComposite.getSelectedAccountId()));

        regionComposite.selectAwsRegion(RegionUtils.getRegion(getAttribute(configuration, A_REGION, RunSamLocalDataModel.DEFAULT_REGION)));
        String projectName = getAttribute(configuration, A_PROJECT, "");
        workspaceComposite.setFilePath(projectName);

        mavenGoalsComplex.setText(getAttribute(configuration, A_MAVEN_GOALS, RunSamLocalDataModel.DEFAULT_MAVEN_GOALS));

        samLocalExecutableComplex.setText(getAttribute(configuration, A_SAM,
                LambdaPlugin.getDefault().getPreferenceStore().getString(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE)));

        debugPortComplex.setText(getAttribute(configuration, A_DEBUG_PORT, String.valueOf(RunSamLocalDataModel.DEFAULT_DEBUG_PORT)));
        templateFileComposite.setFilePath(getAttribute(configuration, A_TEMPLATE, SamLocalPathFinder.findTemplateFile(projectName)));
        envvarFileComposite.setFilePath(getAttribute(configuration, A_ENV_VARS, ""));

        codeUriComplex.setText(getAttribute(configuration, A_CODE_URI, SamLocalPathFinder.findCodeUri(projectName)));
        timeoutComplex.setText(getAttribute(configuration, A_TIME_OUT, String.valueOf(RunSamLocalDataModel.DEFAULT_TIME_OUT)));

        SamAction samCommand = SamAction.fromValue(getAttribute(configuration, A_ACTION, SamAction.INVOKE.getName()));
        samCommandsComboViewer.selectItem(samCommand);

        switch (samCommand) {
        case INVOKE:
            lambdaPhysicalIdCombo.selectItem(getAttribute(configuration, A_LAMBDA_IDENTIFIER, (String) null));
            eventFileComposite.setFilePath(getAttribute(configuration, A_EVENT, SamLocalPathFinder.findEventFile(projectName)));
            break;
        case START_API:
            hostComplex.setText(getAttribute(configuration, A_HOST, SamLocalStartApiDataModel.DEFAULT_HOST));
            portComplex.setText(getAttribute(configuration, A_PORT, String.valueOf(SamLocalStartApiDataModel.DEFAULT_PORT)));
            break;
        }

        advancedSettingsExpandable.setExpanded(false);
        setDirty(false);
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(A_PROFILE, dataModel.getAccount().getAccountName());
        configuration.setAttribute(A_REGION, dataModel.getRegionDataModel().getRegionId());
        configuration.setAttribute(A_PROJECT, dataModel.getWorkspaceDataModel().getFilePath());
        configuration.setAttribute(A_MAVEN_GOALS, dataModel.getMavenGoals());
        configuration.setAttribute(A_SAM, dataModel.getSamRuntime());
        configuration.setAttribute(A_DEBUG_PORT, String.valueOf(dataModel.getDebugPort()));
        configuration.setAttribute(A_TEMPLATE, dataModel.getTemplateFileLocationDataModel().getFilePath());
        configuration.setAttribute(A_ENV_VARS, dataModel.getEnvvarFileLocationDataModel().getFilePath());

        configuration.setAttribute(A_CODE_URI, dataModel.getCodeUri());
        configuration.setAttribute(A_TIME_OUT, String.valueOf(dataModel.getTimeOut()));

        SamAction samAction = dataModel.getSamAction();
        configuration.setAttribute(A_ACTION, samAction.getName());
        dataModel.getActionDataModel().toAttributeMap().forEach(configuration::setAttribute);
    }

    @Override
    public String getName() {
        return "Main";
    }

    @Override
    public Image getImage() {
        return LambdaPlugin.getDefault().getImageRegistry().getDescriptor(LambdaPlugin.IMAGE_SAM_LOCAL).createImage();
    }

    private String getAttribute(ILaunchConfiguration configuration, String name, String defaultValue) {
        try {
            return configuration.getAttribute(name, defaultValue);
        } catch (CoreException ex) {
            return defaultValue;
        }
    }
}
