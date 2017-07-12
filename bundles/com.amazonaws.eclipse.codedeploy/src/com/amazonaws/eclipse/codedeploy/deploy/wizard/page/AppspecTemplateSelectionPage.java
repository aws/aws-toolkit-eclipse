package com.amazonaws.eclipse.codedeploy.deploy.wizard.page;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.amazonaws.eclipse.codedeploy.appspec.AppspecTemplateRegistry;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateMetadataModel;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.model.DeployProjectToCodeDeployWizardDataModel;

public class AppspecTemplateSelectionPage extends WizardPage {

    /* Data model */
    private final DeployProjectToCodeDeployWizardDataModel dataModel;

    /**
     * For fast look-up when switching the top control
     */
    private final Map<AppspecTemplateMetadataModel, AppspecTemplateConfigComposite> templateConfigCompositeMap =
            new HashMap<>();

    /* UI widgets */

    private ComboViewer appspecTemplateSelectionCombo;
    private Composite stackArea;
    private AppspecTemplateConfigComposite selectedTemplateComposite;

    /**
     * The validation status listener to be registered to the template config UI
     * composite that is currently shown in the page.
     */
    private final IChangeListener selectedTemplateConfigValidationStatusListener = new IChangeListener() {

        @Override
        public void handleChange(ChangeEvent event) {
            Object observable = event.getObservable();
            if (observable instanceof AggregateValidationStatus == false) return;

            AggregateValidationStatus statusObservable = (AggregateValidationStatus)observable;
            Object statusObservableValue = statusObservable.getValue();
            if (statusObservableValue instanceof IStatus == false) return;

            IStatus status = (IStatus)statusObservableValue;
            boolean success = (status.getSeverity() == IStatus.OK);
            setPageComplete(success);
            if (success) {
                setMessage("", IStatus.OK);
            } else {
                setMessage(status.getMessage(), IStatus.ERROR);
            }
        }
    };

    public AppspecTemplateSelectionPage(DeployProjectToCodeDeployWizardDataModel dataModel) {
        super("Appspec Template Configuration");
        setTitle("Appspec Template Configuration");
        setDescription("");

        this.dataModel = dataModel;
    }

    public Map<String, String> getParamValuesForSelectedTemplate() {
        if (selectedTemplateComposite != null) {
            return selectedTemplateComposite.getAllParameterValues();
        }
        return Collections.emptyMap();
    }


    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(3, false));

        List<AppspecTemplateMetadataModel> allModels =
                AppspecTemplateRegistry.getInstance().getDefaultTemplates();
        allModels.addAll(AppspecTemplateRegistry.getInstance().getCustomTemplates());

        createAppspecTemplateSelection(composite, allModels);

        stackArea = new Composite(composite, SWT.NONE);
        GridData stackAreaLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        stackAreaLayoutData.horizontalSpan = 3;
        stackArea.setLayoutData(stackAreaLayoutData);
        stackArea.setLayout(new StackLayout());

        createStackedTemplateConfigComposites(stackArea, allModels);

        AppspecTemplateMetadataModel initSelection = allModels.get(0);
        appspecTemplateSelectionCombo.setSelection(
                new StructuredSelection(initSelection), true);

        setControl(composite);
        setPageComplete(true);
    }

    private void createAppspecTemplateSelection(
            Composite parent, List<AppspecTemplateMetadataModel> allModels) {

        new Label(parent, SWT.READ_ONLY).setText("Appspec template: ");

        appspecTemplateSelectionCombo = new ComboViewer(parent, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        appspecTemplateSelectionCombo.getCombo().setLayoutData(
                new GridData(SWT.FILL, SWT.CENTER, true, false));
        appspecTemplateSelectionCombo.setContentProvider(ArrayContentProvider.getInstance());
        appspecTemplateSelectionCombo.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AppspecTemplateMetadataModel) {
                    AppspecTemplateMetadataModel model = (AppspecTemplateMetadataModel) element;
                    return model.getTemplateName();
                }
                return super.getText(element);
            }
        });

        appspecTemplateSelectionCombo.setInput(allModels);

        appspecTemplateSelectionCombo
                .addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event
                        .getSelection();
                Object selectedObject = selection.getFirstElement();

                if (selectedObject instanceof AppspecTemplateMetadataModel) {
                    AppspecTemplateMetadataModel model = (AppspecTemplateMetadataModel) selectedObject;
                    onTemplateSelectionChanged(model);
                }
            }
        });

        Button importTemplateBtn = new Button(parent, SWT.PUSH);
        importTemplateBtn.setText("Import new template");
        importTemplateBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        importTemplateBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                ImportAppspecTemplateMetadataDialog importDialog = new ImportAppspecTemplateMetadataDialog(
                        Display.getCurrent().getActiveShell());
                int returnCode = importDialog.open();

                if (returnCode != IDialogConstants.CANCEL_ID) {
                    selectImportedTemplate();
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void selectImportedTemplate() {
        Set<String> existingTemplateNames = new HashSet<>();
        for (AppspecTemplateMetadataModel template : templateConfigCompositeMap.keySet()) {
            existingTemplateNames.add(template.getTemplateName());
        }

        // Now re-load from the registry and find the new template model
        for (AppspecTemplateMetadataModel template : AppspecTemplateRegistry
                .getInstance().getCustomTemplates()) {

            if ( !existingTemplateNames.contains(template.getTemplateName()) ) {
                AppspecTemplateMetadataModel newTemplate = template;

                ((List<AppspecTemplateMetadataModel>) appspecTemplateSelectionCombo
                        .getInput()).add(newTemplate);

                AppspecTemplateConfigComposite newTemplateComposite = new AppspecTemplateConfigComposite(
                        stackArea, SWT.NONE, newTemplate);
                templateConfigCompositeMap.put(newTemplate, newTemplateComposite);

                appspecTemplateSelectionCombo.refresh();
                appspecTemplateSelectionCombo.setSelection(
                        new StructuredSelection(newTemplate), true);

                return;
            }
        }
    }

    private void createStackedTemplateConfigComposites(
            Composite parent, List<AppspecTemplateMetadataModel> allModels) {

        for (AppspecTemplateMetadataModel templateModel : allModels) {
            AppspecTemplateConfigComposite templateConfigComposite = new AppspecTemplateConfigComposite(
                    parent, SWT.NONE, templateModel);
            // put it into the look-up hashmap
            templateConfigCompositeMap.put(templateModel, templateConfigComposite);
        }
    }

    private void onTemplateSelectionChanged(AppspecTemplateMetadataModel model) {
        if (selectedTemplateComposite != null) {
            selectedTemplateComposite.removeValidationStatusChangeListener();
        }

        AppspecTemplateConfigComposite compositeToShow = templateConfigCompositeMap.get(model);

        if (compositeToShow != null) {
            StackLayout stackLayout = (StackLayout) stackArea.getLayout();
            stackLayout.topControl = compositeToShow;

            compositeToShow.setValidationStatusChangeListener(selectedTemplateConfigValidationStatusListener);
            compositeToShow.updateValidationStatus();

            stackArea.layout();

            selectedTemplateComposite = compositeToShow;
            dataModel.setTemplateModel(compositeToShow.getTemplateModel());
        }
    }

}
