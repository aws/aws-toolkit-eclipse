package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel.SslConfiguration;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.S3ApplicationSource;
import com.amazonaws.services.opsworks.model.Source;

public class AppConfigurationPage extends WizardPageWithOnEnterHook {

    /* Data model */
    private final DeployProjectToOpsworksWizardDataModel dataModel;

    /* UI widgets */
    private final StackLayout stackLayout = new StackLayout();

    private Composite stackComposite;
    private ExistingAppConfigurationReviewComposite existingAppConfigComposite;
    private NewAppConfigurationComposite newAppConfigurationComposite;

    /**
     * The validation status listener to be registered to the UI composite for
     * the new Java app configuration.
     */
    private final IChangeListener newJavaAppConfigValidationStatusListener = new IChangeListener() {

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

    public AppConfigurationPage(DeployProjectToOpsworksWizardDataModel dataModel) {
        super("App Configuration");
        setTitle("App Configuration");
        setDescription("");

        this.dataModel = dataModel;

    }

    @Override
    public void createControl(Composite parent) {
        stackComposite = new Composite(parent, SWT.NONE);
        stackComposite.setLayout(stackLayout);

        setControl(stackComposite);
    }

    @Override
    public void onEnterPage() {
        if (newAppConfigurationComposite != null) {
            newAppConfigurationComposite.removeValidationStatusChangeListener();
            newAppConfigurationComposite.dispose();
        }
        if (existingAppConfigComposite != null) {
            existingAppConfigComposite.dispose();
        }

        resetErrorMessage();
        resetDataModel();

        if (dataModel.getIsCreatingNewJavaApp()) {
            newAppConfigurationComposite = new NewAppConfigurationComposite(stackComposite, dataModel);
            newAppConfigurationComposite.setValidationStatusChangeListener(newJavaAppConfigValidationStatusListener);

            stackLayout.topControl = newAppConfigurationComposite;

        } else {
            // Show the review page of the selected Java app
            existingAppConfigComposite = new ExistingAppConfigurationReviewComposite(
                    stackComposite, dataModel.getExistingJavaApp());

            if ( existingAppConfigComposite.getParsedS3ApplicationSource() == null ) {
                // Set the page to incomplete if the application source is not from S3
                Source appSource = dataModel.getExistingJavaApp().getAppSource();
                setMessage(String.format("Unsupported application source %s:[%s], " +
                        "the OpsWorks Eclipse plugin only supports S3 as the data source. " +
                        "Please use AWS Console to deploy to this app.",
                        appSource.getType(), appSource.getUrl()), IStatus.ERROR);
                setPageComplete(false);

            } else {
                dataModel.setS3ApplicationSource(existingAppConfigComposite
                        .getParsedS3ApplicationSource());
            }

            stackLayout.topControl = existingAppConfigComposite;
        }

        stackComposite.layout(true, true);
    }

    /* Private interface */

    private void resetDataModel() {
        dataModel.setS3ApplicationSource(new S3ApplicationSource());
        dataModel.clearEnvironmentVariable();
        dataModel.clearCustomDomains();
        dataModel.setEnableSsl(false);
        dataModel.setSslConfiguration(new SslConfiguration());
    }

    private void resetErrorMessage() {
        setMessage("", IStatus.OK);
        setPageComplete(true);
    }

}
