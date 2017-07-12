/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotInListValidator;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.ApplicationNameValidator;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.EnvironmentNameValidator;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;

final class DeployWizardApplicationSelectionPage extends AbstractDeployWizardPage {

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    private static final String VPC_CONFIGURATION_DOC_URL = "https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/vpc.html";

    private DeployWizardVpcConfigurationPage vpcConfigPage;

    // Region controls
    private Combo regionCombo;
    private SelectionListener regionChangeListener;

    // Application controls
    private Button createNewApplicationRadioButton;
    private ISWTObservableValue createNewApplicationRadioButtonObservable;
    private Combo existingApplicationCombo;
    private Text newApplicationDescriptionText;
    private Text newApplicationNameText;
    private ControlDecoration newApplicationNameDecoration;
    private Button existingApplicationRadioButton;

    // Environment controls
    private ControlDecoration newEnvironmentNameDecoration;
    private Text newEnvironmentNameText;
    private Text newEnvironmentDescriptionText;
    private Combo environmentTypeCombo;
    private Button useNonDefaultVpcButton;

    private boolean useNonDefaultVpc = false;

    // Asynchronous workers
    private LoadApplicationsThread loadApplicationsThread;
    private LoadEnvironmentsThread loadEnvironmentsThread;

    private IObservableSet existingEnvironmentNames = new WritableSet();
    private IObservableSet existingApplicationNames = new WritableSet();
    private IObservableValue environmentNamesLoaded = new WritableValue();
    private IObservableValue applicationNamesLoaded = new WritableValue();

    private ISWTObservableValue newApplicationNameTextObservable;
    private ISWTObservableValue newApplicationDescriptionTextObservable;
    private ISWTObservableValue newEnvironmentDescriptionTextObservable;
    private ISWTObservableValue newEnvironmentNameTextObservable;
    private ISWTObservableValue environmentTypeComboObservable;
    private ISWTObservableValue useNonDefaultVpcButtonObservable;

    // Status of our connectivity to AWS Elastic Beanstalk
    private IStatus connectionStatus;

    private AWSElasticBeanstalk elasticBeanstalkClient;

    DeployWizardApplicationSelectionPage(DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
        environmentNamesLoaded.setValue(false);
        applicationNamesLoaded.setValue(false);
        vpcConfigPage = new DeployWizardVpcConfigurationPage(wizardDataModel);
    }

    @Override
    public List<WizardFragment> getChildFragments() {
        List<WizardFragment> fragmentList = new ArrayList<>();
        if (useNonDefaultVpc) {
            fragmentList.add(vpcConfigPage);
        }
        return fragmentList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.wst.server.ui.wizard.WizardFragment#createComposite(org.eclipse.swt.widgets.Composite, org.eclipse.wst.server.ui.wizard.IWizardHandle)
     */
    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        wizardHandle = handle;
        elasticBeanstalkClient = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(wizardDataModel.getRegionEndpoint());
        handle.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        handle.setMessage("", IStatus.OK);
        connectionStatus = testConnection();

        if (connectionStatus.isOK()) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            createRegionSection(composite);
            createApplicationSection(composite);
            createEnvironmentSection(composite);
            createImportSection(composite);

            bindControls();
            initializeDefaults();
            return composite;
        } else {
            return new ErrorComposite(parent, SWT.NONE, connectionStatus);
        }
    }

    private IStatus testConnection() {
        try {
            wizardHandle.setMessage("", IStatus.OK);
            wizardHandle.run(true, false, new CheckAccountRunnable(elasticBeanstalkClient));
            wizardHandle.setMessage("", IStatus.OK);
            return Status.OK_STATUS;
        } catch (InvocationTargetException ite) {
            String errorMessage = "Unable to connect to AWS Elastic Beanstalk.  ";
            try {
                throw ite.getCause();
            } catch (AmazonServiceException ase) {
                errorMessage += "Make sure you've registered your AWS account for the AWS Elastic Beanstalk service.";
            } catch (AmazonClientException ace) {
                errorMessage += "Make sure your computer is connected to the internet, and any network firewalls or proxys are configured appropriately.";
            } catch (Throwable t) {}

            return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, ite.getCause());
        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        }
    }

    /**
     * Initializes the page to its default selections
     */
    private void initializeDefaults() {
        createNewApplicationRadioButtonObservable.setValue(true);
        existingApplicationRadioButton.setSelection(false);
        newApplicationNameTextObservable.setValue("");
        newApplicationDescriptionTextObservable.setValue("");
        newEnvironmentNameTextObservable.setValue("");
        newEnvironmentDescriptionTextObservable.setValue("");
        environmentTypeComboObservable.setValue(ConfigurationOptionConstants.LOAD_BALANCED_ENV);

        if (RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.BEANSTALK)) {
            regionCombo.setText(RegionUtils.getCurrentRegion().getName());
            wizardDataModel.setRegion(RegionUtils.getCurrentRegion());
        } else {
            regionCombo.setText(RegionUtils.getRegion(ElasticBeanstalkPlugin.DEFAULT_REGION).getName());
            wizardDataModel.setRegion(RegionUtils.getRegion(ElasticBeanstalkPlugin.DEFAULT_REGION));
        }
        regionChangeListener.widgetSelected(null);

        // Trigger the standard enabled / disabled control logic
        radioButtonSelected(createNewApplicationRadioButton);
    }

    private void createRegionSection(Composite composite) {
        Group regionGroup = newGroup(composite, "", 2);
        regionGroup.setLayout(new GridLayout(2, false));

        newLabel(regionGroup, "Region:");

        regionCombo = newCombo(regionGroup);
        for (Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.BEANSTALK) ) {
            regionCombo.add(region.getName());
            regionCombo.setData(region.getName(), region);
        }
        Region region = RegionUtils.getRegionByEndpoint(wizardDataModel.getRegionEndpoint());
        regionCombo.setText(region.getName());

        newFillingLabel(regionGroup, "AWS regions are geographically isolated, " +
            "allowing you to position your Elastic Beanstalk application closer to you or your customers.", 2);

        regionChangeListener = new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Region region = (Region)regionCombo.getData(regionCombo.getText());
                String endpoint = region.getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK);
                elasticBeanstalkClient = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(endpoint);
                wizardDataModel.setRegion(region);

                if (wizardDataModel.getKeyPairComposite() != null) {
                    wizardDataModel.getKeyPairComposite().getKeyPairSelectionTable().setEc2RegionOverride(region);
                }

                createNewApplicationRadioButtonObservable.setValue(true);
                existingApplicationCombo.setEnabled(false);
                newApplicationNameText.setEnabled(true);
                newApplicationDescriptionText.setEnabled(true);

                refreshApplications();
                refreshEnvironments();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };
        regionCombo.addSelectionListener(regionChangeListener);
    }

    private void createApplicationSection(Composite composite) {
        Group applicationGroup = newGroup(composite, "Application:", 2);
        applicationGroup.setLayout(new GridLayout(2, false));

        createNewApplicationRadioButton = newRadioButton(applicationGroup, "Create a new application:", 2, true);
        createNewApplicationRadioButtonObservable = SWTObservables.observeSelection(
                createNewApplicationRadioButton);

        new NewApplicationOptionsComposite(applicationGroup);

        existingApplicationRadioButton = newRadioButton(applicationGroup, "Choose an existing application:", 1);
        existingApplicationCombo = newCombo(applicationGroup);
        existingApplicationCombo.setEnabled(false);
    }

    private void createEnvironmentSection(Composite composite) {
        Group environmentOptionsGroup = newGroup(composite, "Environment:", 2);
        environmentOptionsGroup.setLayout(new GridLayout(2, false));

        new NewEnvironmentOptionsComposite(environmentOptionsGroup);
    }

    private void createImportSection(final Composite composite) {
        Hyperlink link = new Hyperlink(composite, SWT.None);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                ImportEnvironmentsWizard newWizard = new ImportEnvironmentsWizard();
                WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newWizard);
                dialog.open();
            }
        });
        link.setText("Import an existing environment into the Servers view");
        link.setUnderlined(true);
        GridData layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        link.setLayoutData(layoutData);
    }

    private void bindControls() {
        super.initializeValidators();

        newApplicationNameTextObservable = SWTObservables.observeText(newApplicationNameText, SWT.Modify);
        bindingContext.bindValue(
                newApplicationNameTextObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.NEW_APPLICATION_NAME), null, null);
        ChainValidator<String> applicationNameValidator = new ChainValidator<>(
                newApplicationNameTextObservable, createNewApplicationRadioButtonObservable,
                new ApplicationNameValidator(),
                new NotInListValidator<String>(existingApplicationNames, "Duplicate application name."));
        bindingContext.addValidationStatusProvider(applicationNameValidator);
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(applicationNamesLoaded, new BooleanValidator("Appliction names not yet loaded")));
        new DecorationChangeListener(newApplicationNameDecoration, applicationNameValidator.getValidationStatus());
        newApplicationDescriptionTextObservable = SWTObservables.observeText(newApplicationDescriptionText, SWT.Modify);
        bindingContext.bindValue(
                newApplicationDescriptionTextObservable,
                PojoObservables.observeValue(wizardDataModel,
                    DeployWizardDataModel.NEW_APPLICATION_DESCRIPTION),
                    null, null);
        bindingContext.bindValue(
                createNewApplicationRadioButtonObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.CREATING_NEW_APPLICATION), null, null);

        // Existing application bindings
        bindingContext.bindValue(SWTObservables.observeSelection(existingApplicationCombo),
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.EXISTING_APPLICATION_NAME));

        // New environment bindings
        newEnvironmentNameTextObservable = SWTObservables.observeText(newEnvironmentNameText, SWT.Modify);
        bindingContext.bindValue(
                newEnvironmentNameTextObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.NEW_ENVIRONMENT_NAME), null, null);
        newEnvironmentDescriptionTextObservable = SWTObservables.observeText(newEnvironmentDescriptionText, SWT.Modify);
        bindingContext.bindValue(
                newEnvironmentDescriptionTextObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.NEW_ENVIRONMENT_DESCRIPTION), null, null);
        ChainValidator<String> environmentNameValidator = new ChainValidator<>(
                newEnvironmentNameTextObservable,
                new EnvironmentNameValidator(),
                new NotInListValidator<String>(existingEnvironmentNames, "Duplicate environment name."));
        bindingContext.addValidationStatusProvider(environmentNameValidator);
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(environmentNamesLoaded,
                new BooleanValidator("Environment names not yet loaded")));
        new DecorationChangeListener(newEnvironmentNameDecoration, environmentNameValidator.getValidationStatus());

        environmentTypeComboObservable = SWTObservables.observeSelection(environmentTypeCombo);
        bindingContext.bindValue(
                environmentTypeComboObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.ENVIRONMENT_TYPE));

        useNonDefaultVpcButtonObservable = SWTObservables.observeSelection(useNonDefaultVpcButton);
        bindingContext.bindValue(
                useNonDefaultVpcButtonObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.USE_NON_DEFAULT_VPC));
    }

    /**
     * Asynchronously updates the set of existing applications. Must be called
     * from the UI thread.
     */
    private void refreshApplications() {
        /*
         * While the values load, we need to disable the controls and fake a
         * radio button selected event.
         */
        createNewApplicationRadioButton.setSelection(true);
        existingApplicationRadioButton.setSelection(false);
        existingApplicationRadioButton.setEnabled(false);
        radioButtonSelected(createNewApplicationRadioButton);

        existingApplicationCombo.setItems(new String[] { LOADING });
        existingApplicationCombo.select(0);
        cancelThread(loadApplicationsThread);
        applicationNamesLoaded.setValue(false);
        loadApplicationsThread = new LoadApplicationsThread();
        loadApplicationsThread.start();
    }

    /**
     * Safely cancels the thread given.
     */
    private void cancelThread(CancelableThread thread) {
        if ( thread != null ) {
            synchronized (thread) {
                if ( thread.isRunning() ) {
                    thread.cancel();
                }
            }
        }
    }

    /**
     * Asynchronously updates the set of existing environments for the current
     * application.
     */
    private void refreshEnvironments() {
        cancelThread(loadEnvironmentsThread);
        environmentNamesLoaded.setValue(false);
        loadEnvironmentsThread = new LoadEnvironmentsThread();
        loadEnvironmentsThread.start();
    }

    /**
     * We handle radio button selections by enabling and disabling various
     * controls. There are only two sources of these events that we care about.
     */
    @Override
    protected void radioButtonSelected(Object source) {
        if ( source == existingApplicationRadioButton || source == createNewApplicationRadioButton) {
            boolean isCreatingNewApplication = (Boolean) createNewApplicationRadioButtonObservable.getValue();

            existingApplicationCombo.setEnabled(!isCreatingNewApplication);
            newApplicationNameText.setEnabled(isCreatingNewApplication);
            newApplicationDescriptionText.setEnabled(isCreatingNewApplication);
        }
    }

    @Override
    public void enter() {
        super.enter();

        if ( connectionStatus != null && connectionStatus.isOK() ) {
            refreshApplications();
            refreshEnvironments();
        }
    }

    /**
     * Cancel any outstanding work before exiting.
     */
    @Override
    public void exit() {
        cancelThread(loadApplicationsThread);
        cancelThread(loadEnvironmentsThread);
        super.exit();
    }

    private boolean isServiceSignUpException(Exception e) {
        if (e instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException)e;
            return "OptInRequired".equalsIgnoreCase(ase.getErrorCode());
        }

        return false;
    }

    private IStatus newServiceSignUpErrorStatus(Exception e) {
        return new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
            "Error connecting to AWS Elastic Beanstalk.  " +
            "Make sure you've signed up your AWS account for Elastic Beanstalk, and " +
            "waited for the changes to propagate.", e);
    }

    private class NewApplicationOptionsComposite extends Composite {
        public NewApplicationOptionsComposite(Composite parent) {
            super(parent, SWT.NONE);
            setLayout(new GridLayout(2, false));
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalIndent = 15;
            gridData.horizontalSpan = 2;
            setLayoutData(gridData);

            newLabel(this, "Name:");
            newApplicationNameText = newText(this);
            newApplicationNameDecoration = newControlDecoration(
                    newApplicationNameText,
                    "Enter a new application name or select an existing application.");

            newLabel(this, "Description:");
            newApplicationDescriptionText = newText(this);
        }
    }

    private class NewEnvironmentOptionsComposite extends Composite {
        public NewEnvironmentOptionsComposite(Composite parent) {
            super(parent, SWT.NONE);
            setLayout(new GridLayout(2, false));
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalIndent = 15;
            gridData.horizontalSpan = 2;
            setLayoutData(gridData);

            newLabel(this, "Name:");
            newEnvironmentNameText = newText(this);
            newEnvironmentNameDecoration = newControlDecoration(
                    newEnvironmentNameText,
                    "Enter a new environment name");

            newLabel(this, "Description:");
            newEnvironmentDescriptionText = newText(this);

            final String[] items = {
                ConfigurationOptionConstants.SINGLE_INSTANCE_ENV,
                ConfigurationOptionConstants.LOAD_BALANCED_ENV,
                ConfigurationOptionConstants.WORKER_ENV
            };

            newLabel(this, "Type:");
            environmentTypeCombo = newCombo(this);
            environmentTypeCombo.setItems(items);

            useNonDefaultVpcButton = newCheckbox(parent, "", 1);
            useNonDefaultVpcButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    useNonDefaultVpc = useNonDefaultVpcButton.getSelection();
                    wizardHandle.update();
                }
            });
            createVpcSelectionLabel(parent);
        }
    }

    private void createVpcSelectionLabel(Composite composite) {
        adjustLinkLayout(newLink(composite,
                "Select the VPC to use when creating your environment. "
                        + "<a href=\""
                        + VPC_CONFIGURATION_DOC_URL
                        + "\">Learn more</a>"), 1);
    }

    private final class LoadApplicationsThread extends CancelableThread {
        @Override
        public void run() {
            final List<ApplicationDescription> applications = new ArrayList<>();
            try {
                applications.addAll(elasticBeanstalkClient.describeApplications().getApplications());
            } catch (Exception e) {
                if (isServiceSignUpException(e)) {
                    StatusManager.getManager().handle(newServiceSignUpErrorStatus(e), StatusManager.SHOW | StatusManager.LOG);
                } else {
                    Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                            "Unable to load existing applications: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        List<String> applicationNames = new ArrayList<>();
                        for ( ApplicationDescription application : applications ) {
                            applicationNames.add(application.getApplicationName());
                        }
                        Collections.sort(applicationNames);

                        synchronized (LoadApplicationsThread.this) {
                            if ( !isCanceled() ) {
                                existingApplicationNames.clear();
                                existingApplicationNames.addAll(applicationNames);

                                existingApplicationCombo.removeAll();
                                for ( String applicationName : applicationNames ) {
                                    existingApplicationCombo.add(applicationName);
                                }

                                if ( applications.size() > 0 ) {
                                    existingApplicationCombo.select(0);
                                    existingApplicationRadioButton.setEnabled(true);
                                } else {
                                    existingApplicationCombo.setEnabled(false);
                                    existingApplicationRadioButton.setEnabled(false);
                                    createNewApplicationRadioButtonObservable.setValue(true);
                                    existingApplicationCombo.setItems(new String[] { NONE_FOUND});
                                    existingApplicationCombo.select(0);
                                }
                                applicationNamesLoaded.setValue(true);
                                runValidators();
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }
    }

    private final class LoadEnvironmentsThread extends CancelableThread {
        @Override
        public void run() {
            final List<EnvironmentDescription> environments = new ArrayList<>();
            try {
                environments.addAll(elasticBeanstalkClient.describeEnvironments().getEnvironments());
            } catch (Exception e) {
                if (isServiceSignUpException(e)) {
                    StatusManager.getManager().handle(newServiceSignUpErrorStatus(e), StatusManager.SHOW | StatusManager.LOG);
                } else {
                    Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                        "Unable to load existing environments: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<String> environmentNames = new ArrayList<>();
                        for ( EnvironmentDescription environment : environments ) {
                            // Skip any terminated environments, since we can safely reuse their names
                            if ( isEnvironmentTerminated(environment) ) {
                                continue;
                            }
                            environmentNames.add(environment.getEnvironmentName());
                        }
                        Collections.sort(environmentNames);

                        synchronized (LoadEnvironmentsThread.this) {
                            if ( !isCanceled() ) {
                                existingEnvironmentNames.clear();
                                existingEnvironmentNames.addAll(environmentNames);
                                environmentNamesLoaded.setValue(true);
                                runValidators();
                            }
                        }
                    } finally {
                        setRunning(false);
                    }
                }
            });
        }
    }

    private boolean isEnvironmentTerminated(EnvironmentDescription environment) {
        if (environment == null || environment.getStatus() == null) {
            return false;
        }

        try {
            EnvironmentStatus status = EnvironmentStatus.valueOf(environment.getStatus());
            return (status == EnvironmentStatus.Terminated);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPageTitle() {
        return "Configure Application and Environment";
    }

    @Override
    public String getPageDescription() {
        return "Choose a name for your application and environment";
    }
}
