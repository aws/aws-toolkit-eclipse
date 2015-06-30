package com.amazonaws.eclipse.lambda.upload.wizard.page;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.lambda.upload.wizard.model.UploadFunctionWizardDataModel;

public class FunctionConfigurationPage extends WizardPageWithOnEnterHook {

    /* Data model */
    private final UploadFunctionWizardDataModel dataModel;

    private FunctionConfigurationComposite functionConfigurationComposite;

    /**
     * The validation status listener to be registered to the composite
     */
    private final IChangeListener functionConfigValidationStatusListener = new IChangeListener() {

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

    public FunctionConfigurationPage(UploadFunctionWizardDataModel dataModel) {
        super("Function Configuration");
        setTitle("Function Configuration");
        setDescription("");
        setPageComplete(false);

        this.dataModel = dataModel;
    }

    public void createControl(Composite parent) {
        functionConfigurationComposite = new FunctionConfigurationComposite(
                parent, dataModel);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        functionConfigurationComposite.setLayoutData(layoutData);
        functionConfigurationComposite.setValidationStatusChangeListener(
                functionConfigValidationStatusListener);

        setControl(functionConfigurationComposite);
    }

    public void onEnterPage() {
        resetErrorMessage();

        functionConfigurationComposite.refreshBucketsInFunctionRegion();

        if (dataModel.isCreatingNewFunction()) {
            functionConfigurationComposite.populateNewFunctionName();
            functionConfigurationComposite.pupulateDefaultData();
        } else {
            functionConfigurationComposite
                    .populateExistingFunctionConfig(dataModel
                            .getExistingFunction());
        }
    }

    private void resetErrorMessage() {
        setMessage("", IStatus.OK);
        setPageComplete(true);
    }

}
