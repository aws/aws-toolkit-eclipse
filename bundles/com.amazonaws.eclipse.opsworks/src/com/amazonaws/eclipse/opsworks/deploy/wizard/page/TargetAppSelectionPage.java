package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newRadioButton;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
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
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.ServiceAPIUtils;
import com.amazonaws.eclipse.opsworks.UrlConstants;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.Stack;

public class TargetAppSelectionPage extends WizardPageWithOnEnterHook {

    /* Data model and binding */
    private final DeployProjectToOpsworksWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* UI widgets */

    // Select region
    private Combo regionCombo;

    // Select stack
    private IObservableValue existingStackLoaded = new WritableValue();
    private Combo stackNameCombo;
    private Link stackSelectionMessageLabel;

    // Select Java application
    private IObservableValue existingJavaAppLoaded = new WritableValue();
    private Button useExistingJavaAppRadioButton;
    private ISWTObservableValue useExistingJavaAppRadioButtonObservable;
    private Combo existingJavaAppNameCombo;

    private Button createNewJavaAppRadioButton;
    private ISWTObservableValue createNewJavaAppRadioButtonObservable;
    private Text newApplicationNameText;
    private ControlDecoration newApplicationNameDecoration;
    private ISWTObservableValue newApplicationNameTextObservable;

    /* Other */

    private AWSOpsWorks opsworksClient;
    private LoadStacksThread loadStacksThread;
    private LoadJavaAppsThread loadJavaAppsThread;

    private final WebLinkListener webLinkListener = new WebLinkListener();

    protected final SelectionListener javaAppSectionRadioButtonSelectionListener = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            radioButtonSelected(e.getSource());
            runValidators();
        }
    };

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public TargetAppSelectionPage(DeployProjectToOpsworksWizardDataModel dataModel) {
        super("Select Target Stack and App");
        setTitle("Select Target Stack and App");
        setDescription("");

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createRegionSection(composite);
        createStackSection(composite);
        createJavaAppSection(composite);

        bindControls();
        initializeValidators();
        initializeDefaults();

        onRegionSelectionChange();

        setControl(composite);
        setPageComplete(false);
    }

    private void createRegionSection(Composite composite) {
        Group regionGroup = newGroup(composite, "Select AWS Region");
        regionGroup.setLayout(new GridLayout(1, false));

        regionCombo = newCombo(regionGroup);
        for (Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.OPSWORKS)) {
            regionCombo.add(region.getName());
            regionCombo.setData(region.getName(), region);
        }

        // Find the default region selection
        Region selectedRegion = dataModel.getRegion();
        if (selectedRegion == null) {
            if ( RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.OPSWORKS) ) {
                selectedRegion = RegionUtils.getCurrentRegion();
            } else {
                selectedRegion = RegionUtils.getRegion(OpsWorksPlugin.DEFAULT_REGION);
            }
        }
        regionCombo.setText(selectedRegion.getName());

        regionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRegionSelectionChange();
            }
        });

        newFillingLabel(regionGroup,
                "Select the AWS region where your OpsWorks stack and app was created.");
    }

    private void createStackSection(Composite composite) {
        Group stackGroup = newGroup(composite, "Select OpsWorks stack:");
        stackGroup.setLayout(new GridLayout(1, false));

        stackNameCombo = newCombo(stackGroup);
        stackNameCombo.setEnabled(false);

        stackNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onStackSelectionChange();
            }
        });

        stackSelectionMessageLabel = newLink(stackGroup, webLinkListener, "", 1);
    }

    private void createJavaAppSection(Composite composite) {
        Group javaAppGroupSection = newGroup(composite, "Select or create a Java app:");
        javaAppGroupSection.setLayout(new GridLayout(2, false));

        useExistingJavaAppRadioButton = newRadioButton(javaAppGroupSection,
                "Choose an existing Java app:", 1, true,
                javaAppSectionRadioButtonSelectionListener);
        existingJavaAppNameCombo = newCombo(javaAppGroupSection);
        existingJavaAppNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onJavaAppSelectionChange();
            }
        });

        createNewJavaAppRadioButton = newRadioButton(javaAppGroupSection,
                "Create a new Java app:", 1, true,
                javaAppSectionRadioButtonSelectionListener);
        newApplicationNameText = newText(javaAppGroupSection);

        newApplicationNameDecoration = newControlDecoration(
                newApplicationNameText,
                "Enter a new app name or select an existing app.");
    }

    private void onRegionSelectionChange() {
        Region region = (Region)regionCombo.getData(regionCombo.getText());
        String endpoint = region.getServiceEndpoints()
                .get(ServiceAbbreviations.OPSWORKS);
        opsworksClient = AwsToolkitCore.getClientFactory()
                .getOpsWorksClientByEndpoint(endpoint);

        dataModel.setRegion(region);

        refreshStacks();
    }

    private void onStackSelectionChange() {
        if (existingStackLoaded.getValue().equals(Boolean.TRUE)) {
            Stack stack = (Stack)stackNameCombo.getData(stackNameCombo.getText());
            dataModel.setExistingStack(stack);
            refreshJavaApps();
        }
    }

    private void onJavaAppSelectionChange() {
        if (existingJavaAppLoaded.getValue().equals(Boolean.TRUE)) {
            App selectedApp = (App) existingJavaAppNameCombo
                    .getData(existingJavaAppNameCombo.getText());
            dataModel.setExistingJavaApp(selectedApp);
        }
    }

    private void refreshStacks() {
        existingStackLoaded.setValue(false);

        if (stackNameCombo != null) {
            stackNameCombo.setItems(new String[] {LOADING});
            stackNameCombo.select(0);
        }
        if (stackSelectionMessageLabel != null) {
            stackSelectionMessageLabel.setText("");
        }

        CancelableThread.cancelThread(loadStacksThread);
        loadStacksThread = new LoadStacksThread();
        loadStacksThread.start();
    }

    private void refreshJavaApps() {
        existingJavaAppLoaded.setValue(false);

        // Disable all the UI widgets for app selection until the apps are
        // loaded
        setJavaAppSectionEnabled(false);

        if (existingJavaAppNameCombo != null) {
            existingJavaAppNameCombo.setItems(new String[] {LOADING});
            existingJavaAppNameCombo.select(0);
        }

        CancelableThread.cancelThread(loadJavaAppsThread);
        loadJavaAppsThread = new LoadJavaAppsThread();
        loadJavaAppsThread.start();
    }

    private void radioButtonSelected(Object source) {
        if ( source == useExistingJavaAppRadioButton || source == createNewJavaAppRadioButton) {
            boolean isCreatingNewApp = (Boolean) createNewJavaAppRadioButtonObservable.getValue();

            existingJavaAppNameCombo.setEnabled(!isCreatingNewApp);
            newApplicationNameText.setEnabled(isCreatingNewApp);
        }
    }

    private void bindControls() {
        useExistingJavaAppRadioButtonObservable = SWTObservables
                .observeSelection(useExistingJavaAppRadioButton);

        createNewJavaAppRadioButtonObservable = SWTObservables
                .observeSelection(createNewJavaAppRadioButton);
        bindingContext.bindValue(
                createNewJavaAppRadioButtonObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToOpsworksWizardDataModel.IS_CREATING_NEW_JAVA_APP));

        newApplicationNameTextObservable = SWTObservables
                .observeText(newApplicationNameText, SWT.Modify);
        bindingContext.bindValue(
                newApplicationNameTextObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToOpsworksWizardDataModel.NEW_JAVA_APP_NAME));
    }

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
                existingStackLoaded,
                new BooleanValidator("Please select a stack")));
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                existingJavaAppLoaded,
                useExistingJavaAppRadioButtonObservable, // enabler
                new BooleanValidator("Please select a Java app")));

        ChainValidator<String> appNameValidator = new ChainValidator<>(
                newApplicationNameTextObservable,
                createNewJavaAppRadioButtonObservable, // enabler
                new NotEmptyValidator("Please provide a valid app name"));
        bindingContext.addValidationStatusProvider(appNameValidator);

        new DecorationChangeListener(newApplicationNameDecoration,
                appNameValidator.getValidationStatus());
    }

    private void runValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

    private void initializeDefaults() {
        existingStackLoaded.setValue(false);
        existingJavaAppLoaded.setValue(false);
        useExistingJavaAppRadioButtonObservable.setValue(true);
        createNewJavaAppRadioButtonObservable.setValue(false);
        newApplicationNameTextObservable.setValue("My App");

        radioButtonSelected(useExistingJavaAppRadioButton);
    }

    private final class LoadStacksThread extends CancelableThread {

        @Override
        public void run() {
            final List<String> stackNames = new ArrayList<>();
            final Map<String, Stack> stacks = new HashMap<>();

            try {
                for (Stack stack : ServiceAPIUtils.getAllStacks(opsworksClient)) {
                    stackNames.add(stack.getName());
                    stacks.put(stack.getName(), stack);
                }
                // Sort by name
                Collections.sort(stackNames);

            } catch (Exception e) {
                OpsWorksPlugin.getDefault().reportException(
                        "Unable to load existing stacks.", e);
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (LoadStacksThread.this) {
                            if ( !isCanceled() ) {
                                stackNameCombo.removeAll();
                                for ( String stackName : stackNames ) {
                                    stackNameCombo.add(stackName);
                                    stackNameCombo.setData(stackName, stacks.get(stackName));
                                }

                                if ( stackNames.size() > 0 ) {
                                    existingStackLoaded.setValue(true);

                                    stackNameCombo.setEnabled(true);
                                    stackNameCombo.select(0);

                                    onStackSelectionChange();

                                } else {
                                    existingStackLoaded.setValue(false);

                                    stackNameCombo.setEnabled(false);
                                    stackNameCombo.setItems(new String[] { NONE_FOUND});
                                    stackNameCombo.select(0);

                                    stackSelectionMessageLabel.setText(
                                            "No stack is found in this region. " +
                                            "Please create a new OpsWorks stack via " +
                                            "<a href=\"" + UrlConstants.OPSWORKS_CONSOLE_URL +
                                            "\">AWS Console</a> before making a deployment");

                                    useExistingJavaAppRadioButton.setEnabled(false);
                                    existingJavaAppNameCombo.setEnabled(false);
                                    existingJavaAppNameCombo.setItems(new String[0]);
                                    existingJavaAppLoaded.setValue(false);

                                    createNewJavaAppRadioButton.setEnabled(false);
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

    private final class LoadJavaAppsThread extends CancelableThread {

        @Override
        public void run() {

            final List<String> javaAppUINames = new ArrayList<>();
            final Map<String, App> javaApps = new HashMap<>();

            try {
                String stackId = dataModel.getExistingStack().getStackId();
                for (App javaApp : ServiceAPIUtils
                        .getAllJavaAppsInStack(opsworksClient, stackId)) {
                    // UI names format : "app-name (app-shortname)"
                    String appUIName = String.format("%s (%s)", javaApp.getName(), javaApp.getShortname());
                    javaAppUINames.add(appUIName);
                    javaApps.put(appUIName, javaApp);
                }
                Collections.sort(javaAppUINames);

            } catch (Exception e) {
                Status status = new Status(Status.ERROR, OpsWorksPlugin.PLUGIN_ID,
                        "Unable to load existing Java Apps: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW);
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (LoadJavaAppsThread.this) {
                            if ( !isCanceled() ) {

                                // Restore the UI enabled status of the app section
                                setJavaAppSectionEnabled(true);
                                radioButtonSelected(useExistingJavaAppRadioButton);

                                existingJavaAppNameCombo.removeAll();
                                for ( String javaAppUIName : javaAppUINames ) {
                                    existingJavaAppNameCombo.add(javaAppUIName);
                                    existingJavaAppNameCombo.setData(javaAppUIName, javaApps.get(javaAppUIName));
                                }

                                if ( javaAppUINames.size() > 0 ) {
                                    existingJavaAppNameCombo.select(0);
                                    existingJavaAppLoaded.setValue(true);

                                    onJavaAppSelectionChange();

                                } else {
                                    useExistingJavaAppRadioButton.setEnabled(false);

                                    existingJavaAppNameCombo.setEnabled(false);
                                    existingJavaAppNameCombo.setItems(new String[] { NONE_FOUND});
                                    existingJavaAppNameCombo.select(0);

                                    existingJavaAppLoaded.setValue(false);

                                    useExistingJavaAppRadioButtonObservable.setValue(false);
                                    createNewJavaAppRadioButtonObservable.setValue(true);
                                    radioButtonSelected(useExistingJavaAppRadioButton);
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

    /**
     * Enable/disable all the UI widgets in the app selection section.
     */
    private void setJavaAppSectionEnabled(boolean enabled) {
        if (createNewJavaAppRadioButton != null)
            createNewJavaAppRadioButton.setEnabled(enabled);
        if (newApplicationNameText != null)
            newApplicationNameText.setEnabled(enabled);

        if (useExistingJavaAppRadioButton != null)
            useExistingJavaAppRadioButton.setEnabled(enabled);
        if (existingJavaAppNameCombo != null)
            existingJavaAppNameCombo.setEnabled(enabled);
    }

    @Override
    protected void onEnterPage() {
    }
}
