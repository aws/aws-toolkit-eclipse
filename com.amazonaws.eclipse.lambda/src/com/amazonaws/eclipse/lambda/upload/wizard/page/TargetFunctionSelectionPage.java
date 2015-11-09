/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.upload.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newRadioButton;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_NAME_CHANGE_SELECTION;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.ATTR_VALUE_REGION_SELECTION_COMBO;
import static com.amazonaws.eclipse.lambda.LambdaAnalytics.EVENT_TYPE_UPLOAD_FUNCTION_WIZARD;

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
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.mobileanalytics.ToolkitAnalyticsManager;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.databinding.NotInListValidator;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.FunctionConfiguration;

public class TargetFunctionSelectionPage extends WizardPageWithOnEnterHook {

    /* Data model and binding */
    private final UploadFunctionWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* UI widgets */

    // Select region
    private Combo regionCombo;

    // Select Java function
    private IObservableValue existingJavaFunctionLoaded = new WritableValue();
    private IObservableSet existingJavaFunctionNames = new WritableSet();
    private Button useExistingJavaFunctionRadioButton;
    private ISWTObservableValue useExistingJavaFunctionRadioButtonObservable;
    private Combo existingJavaFunctionNameCombo;

    private Button createNewJavaFunctionRadioButton;
    private ISWTObservableValue createNewJavaFunctionRadioButtonObservable;
    private Text newJavaFunctionNameText;
    private ControlDecoration newJavaFunctionNameDecoration;
    private ISWTObservableValue newJavaFunctionNameTextObservable;

    private final SelectionListener javaFunctionSectionRadioButtonSelectionListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
            radioButtonSelected(e.getSource());
            runValidators();
        }
    };
    /* Other */

    private AWSLambda lambdaClient;
    private LoadJavaFunctionsThread loadJavaFunctionsThread;

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public TargetFunctionSelectionPage(UploadFunctionWizardDataModel dataModel) {
        super("Select Target Lambda Function");
        setTitle("Select Target Lambda Function");
        setDescription("");
        setPageComplete(false);

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createRegionSection(composite);
        createJavaFunctionSection(composite);

        bindControls();
        initializeValidators();
        initializeDefaults();

        // Force refresh function list
        onRegionSelectionChange();

        setControl(composite);
        setPageComplete(false);
    }

    private void createRegionSection(Composite composite) {
        Group regionGroup = newGroup(composite, "Select AWS Region");
        regionGroup.setLayout(new GridLayout(1, false));

        newFillingLabel(regionGroup,
                "Select the AWS region where your Lambda function is created.");

        Region initialRegion = null;

        Region lastDeploymentRegion = getLastDeploymentRegion();

        regionCombo = newCombo(regionGroup);
        for (Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.LAMBDA)) {
            regionCombo.add(region.getName());
            regionCombo.setData(region.getName(), region);
            if (region.equals(lastDeploymentRegion)) {
                initialRegion = region;
            }
        }

        // Find the default region selection
        if (initialRegion == null) {
            if ( RegionUtils.isServiceSupportedInCurrentRegion(ServiceAbbreviations.LAMBDA) ) {
                initialRegion = RegionUtils.getCurrentRegion();
            } else {
                initialRegion = RegionUtils.getRegion(LambdaPlugin.DEFAULT_REGION);
            }
        }
        regionCombo.setText(initialRegion.getName());

        regionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                trackRegionComboChangeSelection();
                onRegionSelectionChange();
            }
        });

    }

    private Region getLastDeploymentRegion() {
        return this.dataModel.getProjectMetadataBeforeUpload() == null
                ? null
                : this.dataModel.getProjectMetadataBeforeUpload()
                        .getLastDeploymentRegion();
    }

    private void createJavaFunctionSection(Composite composite) {
        Group javaFunctionGroup = newGroup(composite, "Select or create a Lambda function:");
        javaFunctionGroup.setLayout(new GridLayout(2, false));

        useExistingJavaFunctionRadioButton = newRadioButton(javaFunctionGroup,
                "Choose an existing Lambda function:", 1, true,
                javaFunctionSectionRadioButtonSelectionListener);
        existingJavaFunctionNameCombo = newCombo(javaFunctionGroup);
        existingJavaFunctionNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onExistingJavaFunctionSelectionChange();
            }
        });

        createNewJavaFunctionRadioButton = newRadioButton(javaFunctionGroup,
                "Create a new Lambda function:", 1, true,
                javaFunctionSectionRadioButtonSelectionListener);
        newJavaFunctionNameText = newText(javaFunctionGroup);

        newJavaFunctionNameDecoration = newControlDecoration(
                newJavaFunctionNameText,
                "Enter a new function name or select an existing function.");
    }

    private void onRegionSelectionChange() {
        Region region = (Region)regionCombo.getData(regionCombo.getText());
        String endpoint = region.getServiceEndpoints()
                .get(ServiceAbbreviations.LAMBDA);
        lambdaClient = AwsToolkitCore.getClientFactory()
                .getLambdaClientByEndpoint(endpoint);

        dataModel.setRegion(region);

        refreshFunctions();
    }

    private void onExistingJavaFunctionSelectionChange() {
        if (existingJavaFunctionLoaded.getValue().equals(Boolean.TRUE)) {
            Object selectedData = existingJavaFunctionNameCombo
                    .getData(existingJavaFunctionNameCombo.getText());
            FunctionConfiguration selectedFunction = (FunctionConfiguration)selectedData;
            dataModel.setExistingFunction(selectedFunction);
        }
    }

    private void refreshFunctions() {
        existingJavaFunctionLoaded.setValue(false);
        existingJavaFunctionNames.clear();

        if (existingJavaFunctionNameCombo != null) {
            existingJavaFunctionNameCombo.setItems(new String[] {LOADING});
            existingJavaFunctionNameCombo.select(0);
        }

        CancelableThread.cancelThread(loadJavaFunctionsThread);
        loadJavaFunctionsThread = new LoadJavaFunctionsThread(
                getLastDeploymentFunctionName());
        loadJavaFunctionsThread.start();
    }

    private String getLastDeploymentFunctionName() {
        return this.dataModel.getProjectMetadataBeforeUpload() == null
                ? null
                : this.dataModel.getProjectMetadataBeforeUpload()
                        .getLastDeploymentFunctionName();
    }

    private void radioButtonSelected(Object source) {
        if ( source == useExistingJavaFunctionRadioButton || source == createNewJavaFunctionRadioButton) {
            boolean isCreatingNewFunction = (Boolean) createNewJavaFunctionRadioButtonObservable.getValue();

            existingJavaFunctionNameCombo.setEnabled(!isCreatingNewFunction);
            newJavaFunctionNameText.setEnabled(isCreatingNewFunction);
        }
    }

    private void bindControls() {
        useExistingJavaFunctionRadioButtonObservable = SWTObservables
                .observeSelection(useExistingJavaFunctionRadioButton);

        createNewJavaFunctionRadioButtonObservable = SWTObservables
                .observeSelection(createNewJavaFunctionRadioButton);
        bindingContext.bindValue(
                createNewJavaFunctionRadioButtonObservable,
                PojoObservables.observeValue(
                        dataModel,
                        UploadFunctionWizardDataModel.P_IS_CREATING_NEW_FUNCTION));

        newJavaFunctionNameTextObservable = SWTObservables
                .observeText(newJavaFunctionNameText, SWT.Modify);
        bindingContext.bindValue(
                newJavaFunctionNameTextObservable,
                PojoObservables.observeValue(
                        dataModel,
                        UploadFunctionWizardDataModel.P_NEW_FUNCTION_NAME));
    }

    private void initializeValidators() {
        // Bind the validation status to the wizard page message
        aggregateValidationStatus.addChangeListener(new IChangeListener() {

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
                existingJavaFunctionLoaded,
                useExistingJavaFunctionRadioButtonObservable, // enabler
                new BooleanValidator("Please select a Lambda function")));

        ChainValidator<String> functionNameValidator = new ChainValidator<String>(
                newJavaFunctionNameTextObservable,
                createNewJavaFunctionRadioButtonObservable, // enabler
                new NotEmptyValidator("Please provide a Lambda function name"),
                new NotInListValidator<String>(existingJavaFunctionNames, "Duplidate Lambda function name"));
        bindingContext.addValidationStatusProvider(functionNameValidator);

        new DecorationChangeListener(newJavaFunctionNameDecoration,
                functionNameValidator.getValidationStatus());
    }

    private void runValidators() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

    private void initializeDefaults() {
        existingJavaFunctionLoaded.setValue(false);
        useExistingJavaFunctionRadioButtonObservable.setValue(true);
        createNewJavaFunctionRadioButtonObservable.setValue(false);
        newJavaFunctionNameTextObservable.setValue("MyFunction");

        radioButtonSelected(useExistingJavaFunctionRadioButton);
    }


    private final class LoadJavaFunctionsThread extends CancelableThread {

        private final String defaultFunctionName;

        /**
         * @param defaultFunctionName
         *            the function that should be selected by default after all
         *            functions are loaded.
         */
        LoadJavaFunctionsThread(String defaultFunctionName) {
            this.defaultFunctionName = defaultFunctionName;
        }

        @Override
        public void run() {
            final List<String> javaFunctionNames = new ArrayList<String>();
            final Map<String, FunctionConfiguration> javaFunctions = new HashMap<String, FunctionConfiguration>();

            try {
                for (FunctionConfiguration funcConfig : ServiceApiUtils.getAllJavaFunctions(lambdaClient)) {
                    javaFunctionNames.add(funcConfig.getFunctionName());
                    javaFunctions.put(funcConfig.getFunctionName(), funcConfig);
                }
                // Sort by name
                Collections.sort(javaFunctionNames);

            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException(
                        "Unable to load existing Java functions.", e);
                setRunning(false);
                return;
            }

            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    try {
                        synchronized (LoadJavaFunctionsThread.this) {
                            if ( !isCanceled() ) {
                                existingJavaFunctionNameCombo.removeAll();

                                if (javaFunctionNames.size() > 0) {
                                    existingJavaFunctionNames.clear();
                                    existingJavaFunctionNames.addAll(javaFunctionNames);

                                    existingJavaFunctionLoaded.setValue(true);

                                    for ( String funcName : javaFunctionNames ) {
                                        existingJavaFunctionNameCombo.add(funcName);
                                        existingJavaFunctionNameCombo.setData(
                                                funcName, javaFunctions.get(funcName));
                                    }

                                    useExistingJavaFunctionRadioButton.setEnabled(true);
                                    existingJavaFunctionNameCombo.setEnabled(true);
                                    existingJavaFunctionNameCombo
                                            .select(findDefaultFunction(javaFunctionNames));
                                    onExistingJavaFunctionSelectionChange();

                                } else {
                                    existingJavaFunctionLoaded.setValue(false);

                                    useExistingJavaFunctionRadioButton.setEnabled(false);
                                    existingJavaFunctionNameCombo.setEnabled(false);
                                    existingJavaFunctionNameCombo.setItems(new String[] { NONE_FOUND});
                                    existingJavaFunctionNameCombo.select(0);

                                    useExistingJavaFunctionRadioButtonObservable.setValue(false);
                                    createNewJavaFunctionRadioButtonObservable.setValue(true);
                                    radioButtonSelected(useExistingJavaFunctionRadioButton);
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

        private int findDefaultFunction(List<String> functionNames) {
            if (this.defaultFunctionName == null) {
                return 0;
            }
            int defaultInd = functionNames.indexOf(this.defaultFunctionName);
            return defaultInd < 0 ? 0 : defaultInd;
        }
    }

    @Override
    protected void onEnterPage() {
    }

    /*
     * Analytics
     */

    private void trackRegionComboChangeSelection() {
        ToolkitAnalyticsManager analytics = AwsToolkitCore.getDefault()
                .getAnalyticsManager();
        analytics.publishEvent(analytics.eventBuilder()
                .setEventType(EVENT_TYPE_UPLOAD_FUNCTION_WIZARD)
                .addAttribute(ATTR_NAME_CHANGE_SELECTION, ATTR_VALUE_REGION_SELECTION_COMBO)
                .build());
    }

}
