package com.amazonaws.eclipse.codedeploy.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.ServiceAPIUtils;
import com.amazonaws.eclipse.codedeploy.UrlConstants;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.model.DeployProjectToCodeDeployWizardDataModel;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;

public class DeploymentGroupSelectionPage extends WizardPageWithOnEnterHook{

    /* Data model and binding */
    private final DeployProjectToCodeDeployWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    private IObservableValue applicationSelected = new WritableValue();
    private IObservableValue deploymentGroupSelected = new WritableValue();

    /* UI widgets */

    // Select region
    private Combo regionCombo;

    // Select application
    private Combo applicationNameCombo;
    private Link applicationSelectionMessageLabel;

    // Select deployment-group
    private Combo deploymentGroupNameCombo;
    private Link deploymentGroupSelectionMessageLabel;

    /* Other */

    private AmazonCodeDeploy codeDeployClient;
    private LoadApplicationsThread loadApplicationsThread;
    private LoadDeploymentGroupsThread loadDeploymentGroupsThread;

    private final WebLinkListener webLinkListener = new WebLinkListener();

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public DeploymentGroupSelectionPage(DeployProjectToCodeDeployWizardDataModel dataModel) {
        super("Select the target Application and Deployment Group");
        setTitle("Select the target Application and Deployment Group");
        setDescription("");

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        initializeValidators();
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createRegionSection(composite);
        createApplicationSelection(composite);
        createDeploymentGroupSelection(composite);

        onRegionChange();

        bindControls();

        setControl(composite);
        setPageComplete(false);
    }

    @Override
    public void onEnterPage() {
    }

    /* Private interface */

    private void initializeValidators() {
        // Bind the validation status to the wizard page message
        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent arg0) {
                Object value = aggregateValidationStatus.getValue();
                if (value instanceof IStatus == false) return;

                IStatus status = (IStatus)value;
                boolean success = (status.getSeverity() == IStatus.OK);
                setPageComplete(success);
                if (success) {
                    setMessage("", IStatus.OK);
                } else {
                    setMessage(status.getMessage(), IStatus.ERROR);
                }
            }
        });

        // Validation status providers
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                applicationSelected,
                new BooleanValidator("Please select the application")));
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                deploymentGroupSelected,
                new BooleanValidator("Please select the deployment group")));
    }

    private void bindControls() {
        ISWTObservableValue applicationNameComboObservable = SWTObservables
                .observeSelection(applicationNameCombo);
        bindingContext.bindValue(
                applicationNameComboObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToCodeDeployWizardDataModel.APPLICATION_NAME_PROPERTY));

        ISWTObservableValue deploymentGroupNameComboObservable = SWTObservables
                .observeSelection(deploymentGroupNameCombo);
        bindingContext.bindValue(
                deploymentGroupNameComboObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToCodeDeployWizardDataModel.DEPLOYMENT_GROUP_NAME_PROPERTY));
    }

    private void createRegionSection(Composite composite) {
        Group regionGroup = newGroup(composite, "Select AWS Region");
        regionGroup.setLayout(new GridLayout(1, false));

        regionCombo = newCombo(regionGroup);
        for (Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.CODE_DEPLOY)) {
            regionCombo.add(region.getName());
            regionCombo.setData(region.getName(), region);
        }

        // Find the default region selection
        Region selectedRegion = dataModel.getRegion();
        if (selectedRegion == null) {
            if ( RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.CODE_DEPLOY) ) {
                selectedRegion = RegionUtils.getCurrentRegion();
            } else {
                selectedRegion = RegionUtils.getRegion(CodeDeployPlugin.DEFAULT_REGION);
            }
        }
        regionCombo.setText(selectedRegion.getName());

        regionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRegionChange();
            }
        });

        newFillingLabel(regionGroup,
                "Select the AWS region where your CodeDeploy application was created.");
    }

    private void createApplicationSelection(Composite composite) {
        Group applicationGroup = newGroup(composite, "Select CodeDeploy application:");
        applicationGroup.setLayout(new GridLayout(1, false));

        applicationNameCombo = newCombo(applicationGroup);
        applicationNameCombo.setEnabled(false);

        applicationNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onApplicationSelectionChange();
            }
        });

        applicationSelectionMessageLabel = newLink(applicationGroup, webLinkListener, "", 1);
    }

    private void createDeploymentGroupSelection(Composite composite) {
        Group deploymentGroupSection = newGroup(composite, "Select CodeDeploy Deployment Group:");
        deploymentGroupSection.setLayout(new GridLayout(1, false));

        deploymentGroupNameCombo = newCombo(deploymentGroupSection);
        deploymentGroupNameCombo.setEnabled(false);

        deploymentGroupSelectionMessageLabel = newLink(deploymentGroupSection, webLinkListener, "", 1);
    }

    private void onRegionChange() {
        Region region = (Region)regionCombo.getData(regionCombo.getText());
        String endpoint = region.getServiceEndpoints()
                .get(ServiceAbbreviations.CODE_DEPLOY);
        codeDeployClient = AwsToolkitCore.getClientFactory()
                .getCodeDeployClientByEndpoint(endpoint);

        dataModel.setRegion(region);

        refreshApplications();
    }

    private void onApplicationSelectionChange() {
        if (applicationSelected.getValue().equals(Boolean.TRUE)) {
            refreshDeploymentGroups();
        }
    }

    private void refreshApplications() {
        applicationSelected.setValue(false);
        if (applicationNameCombo != null) {
            applicationNameCombo.setItems(new String[] {LOADING});
            applicationNameCombo.select(0);
        }
        if (applicationSelectionMessageLabel != null) {
            applicationSelectionMessageLabel.setText("");
        }

        CancelableThread.cancelThread(loadApplicationsThread);
        loadApplicationsThread = new LoadApplicationsThread();
        loadApplicationsThread.start();
    }

    private void refreshDeploymentGroups() {
        deploymentGroupSelected.setValue(false);
        if (deploymentGroupNameCombo != null) {
            deploymentGroupNameCombo.setItems(new String[] {LOADING});
            deploymentGroupNameCombo.select(0);
        }
        if (deploymentGroupSelectionMessageLabel != null) {
            deploymentGroupSelectionMessageLabel.setText("");
        }

        CancelableThread.cancelThread(loadDeploymentGroupsThread);
        loadDeploymentGroupsThread = new LoadDeploymentGroupsThread();
        loadDeploymentGroupsThread.start();
    }

    private final class LoadApplicationsThread extends CancelableThread {

        @Override
        public void run() {
            final List<String> appNames = new ArrayList<>();
            try {
                appNames.addAll(ServiceAPIUtils.getAllApplicationNames(codeDeployClient));
                Collections.sort(appNames);

            } catch (Exception e) {
                CodeDeployPlugin.getDefault().reportException(
                        "Unable to load existing applications.", e);
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (LoadApplicationsThread.this) {
                            if ( !isCanceled() ) {
                                applicationNameCombo.removeAll();
                                for ( String appName : appNames ) {
                                    applicationNameCombo.add(appName);
                                }

                                if ( appNames.size() > 0 ) {
                                    applicationNameCombo.setEnabled(true);
                                    applicationNameCombo.select(0);

                                    applicationSelected.setValue(true);

                                    refreshDeploymentGroups();

                                } else {
                                    applicationNameCombo.setEnabled(false);
                                    applicationNameCombo.setItems(new String[] { NONE_FOUND});
                                    applicationNameCombo.select(0);

                                    applicationSelected.setValue(false);

                                    applicationSelectionMessageLabel.setText(
                                            "No application is found in this region. " +
                                            "Please create a new CodeDeploy application via " +
                                            "<a href=\"" +
                                            String.format(UrlConstants.CODE_DEPLOY_CONSOLE_URL_FORMAT, dataModel.getRegion().getId()) +
                                            "\">AWS Console</a> before making a deployment");

                                    deploymentGroupNameCombo.setEnabled(false);
                                    deploymentGroupNameCombo.setItems(new String[0]);

                                    deploymentGroupSelected.setValue(false);
                                }

                                // Re-calculate UI layout
                                ((Composite)getControl()).layout();
                            }
                        }

                    } finally {
                        setRunning(false);
                    }
                }
            });
        }
    }

    private final class LoadDeploymentGroupsThread extends CancelableThread {

        @Override
        public void run() {
            final List<String> deployGroupNames = new ArrayList<>();
            try {
                String appName = dataModel.getApplicationName();
                deployGroupNames.addAll(ServiceAPIUtils
                        .getAllDeploymentGroupNames(codeDeployClient, appName));
                Collections.sort(deployGroupNames);

            } catch (Exception e) {
                Status status = new Status(Status.ERROR, CodeDeployPlugin.PLUGIN_ID,
                        "Unable to load existing deployment groups: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW);
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (LoadDeploymentGroupsThread.this) {
                            if ( !isCanceled() ) {
                                deploymentGroupNameCombo.removeAll();
                                for ( String deployGroupName : deployGroupNames ) {
                                    deploymentGroupNameCombo.add(deployGroupName);
                                }

                                if ( deployGroupNames.size() > 0 ) {
                                    deploymentGroupNameCombo.select(0);
                                    deploymentGroupNameCombo.setEnabled(true);

                                    deploymentGroupSelected.setValue(true);

                                } else {
                                    deploymentGroupNameCombo.setEnabled(false);
                                    deploymentGroupNameCombo.setItems(new String[] { NONE_FOUND});
                                    deploymentGroupNameCombo.select(0);

                                    deploymentGroupSelected.setValue(false);

                                    deploymentGroupSelectionMessageLabel.setText(
                                            "No deployment group is found. " +
                                            "Please create a new deployment group via " +
                                            "<a href=\"" +
                                            String.format(UrlConstants.CODE_DEPLOY_CONSOLE_URL_FORMAT, dataModel.getRegion().getId()) +
                                            "\">AWS Console</a> before making a deployment");
                                }

                                // Re-calculate UI layout
                                ((Composite)getControl()).layout();
                            }
                        }

                    } finally {
                        setRunning(false);
                    }
                }
            });
        }
    }

}
