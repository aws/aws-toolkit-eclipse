package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCheckbox;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel.EnvironmentVariable;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel.SslConfiguration;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.S3ApplicationSource;
import com.amazonaws.eclipse.opsworks.explorer.image.OpsWorksExplorerImages;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class NewAppConfigurationComposite extends Composite {

    private DeployProjectToOpsworksWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /**
     * @see #setValidationStatusChangeListener(IChangeListener)
     * @see #removeValidationStatusChangeListener()
     */
    private IChangeListener validationStatusChangeListener;

    private IObservableValue bucketNameSelected = new WritableValue();

    private ISWTObservableValue bucketNameComboObservable;
    private ISWTObservableValue keyNameTextObservable;

    private ISWTObservableValue enableSslCheckBoxObservable;
    private ISWTObservableValue certTextObservable;
    private ISWTObservableValue chainTextObservable;
    private ISWTObservableValue privateKeyTextObservable;

    private Combo bucketNameCombo;
    private Text keyNameText;
    private ControlDecoration keyNameTextDecoration;

    private Button enableSslCheckBox;
    private Text certText;
    private Text chainText;
    private Text privateKeyText;

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    NewAppConfigurationComposite(Composite parent, DeployProjectToOpsworksWizardDataModel dataModel) {
        super(parent, SWT.NONE);

        this.dataModel = dataModel;
        // Clear S3ApplicationSource
        this.dataModel.setS3ApplicationSource(new S3ApplicationSource());

        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        setLayout(new GridLayout(1, false));
        createControls(this);

        bindControls();
        initializeValidators();
        initializeDefaults();

        loadS3BucketsAsync();
    }

    /**
     * Set listener that will be notified whenever the validation status of this
     * composite is updated. This method removes the listener (if any) that is
     * currently registered to this composite - only one listener instance is
     * allowed at a time.
     */
    public synchronized void setValidationStatusChangeListener(IChangeListener listener) {
        removeValidationStatusChangeListener();
        validationStatusChangeListener = listener;
        aggregateValidationStatus.addChangeListener(listener);
    }

    /**
     * @see #setValidationStatusChangeListener(IChangeListener)
     */
    public synchronized void removeValidationStatusChangeListener() {
        if (validationStatusChangeListener != null) {
            aggregateValidationStatus.removeChangeListener(validationStatusChangeListener);
            validationStatusChangeListener = null;
        }
    }

    public void updateValidationStatus() {
        Iterator<?> iterator = bindingContext.getBindings().iterator();
        while (iterator.hasNext()) {
            Binding binding = (Binding)iterator.next();
            binding.updateTargetToModel();
        }
    }

    private void createControls(Composite parent) {
        createBasicSettingSection(parent);
        createApplicationSourceSection(parent);
        createEnvironmentVariablesSection(parent);
        createCustomDomainsSection(parent);
        createSslSettingsSection(parent);
    }

    private void bindControls() {
        bucketNameComboObservable = SWTObservables
                .observeSelection(bucketNameCombo);
        bindingContext.bindValue(
                bucketNameComboObservable,
                PojoObservables.observeValue(
                        dataModel.getS3ApplicationSource(),
                        S3ApplicationSource.BUCKET_NAME));

        keyNameTextObservable = SWTObservables
                .observeText(keyNameText, SWT.Modify);
        bindingContext.bindValue(
                keyNameTextObservable,
                PojoObservables.observeValue(
                        dataModel.getS3ApplicationSource(),
                        S3ApplicationSource.KEY_NAME));

        enableSslCheckBoxObservable = SWTObservables
                .observeSelection(enableSslCheckBox);
        bindingContext.bindValue(
                enableSslCheckBoxObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToOpsworksWizardDataModel.ENABLE_SSL));

        certTextObservable = SWTObservables
                .observeText(certText, SWT.Modify);
        bindingContext.bindValue(
                certTextObservable,
                PojoObservables.observeValue(
                        dataModel.getSslConfiguration(),
                        SslConfiguration.CERTIFICATE));

        chainTextObservable = SWTObservables
                .observeText(chainText, SWT.Modify);
        bindingContext.bindValue(
                chainTextObservable,
                PojoObservables.observeValue(
                        dataModel.getSslConfiguration(),
                        SslConfiguration.CHAIN));

        privateKeyTextObservable = SWTObservables
                .observeText(privateKeyText, SWT.Modify);
        bindingContext.bindValue(
                privateKeyTextObservable,
                PojoObservables.observeValue(
                        dataModel.getSslConfiguration(),
                        SslConfiguration.PRIVATE_KEY));
    }

    private void initializeValidators() {
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                bucketNameSelected,
                new BooleanValidator("Please select a bucket for storing the application source")));

        ChainValidator<String> keyNameValidator = new ChainValidator<>(
                keyNameTextObservable,
                new NotEmptyValidator("Please provide a valid S3 key name"));
        bindingContext.addValidationStatusProvider(keyNameValidator);
        new DecorationChangeListener(keyNameTextDecoration,
                keyNameValidator.getValidationStatus());
    }

    private void initializeDefaults() {
        keyNameTextObservable.setValue("archive.zip");
        enableSslCheckBoxObservable.setValue(false);
        certTextObservable.setValue("");
        chainTextObservable.setValue("");
        privateKeyTextObservable.setValue("");
    }

    private void createBasicSettingSection(Composite parent) {
        Group settingsGroup = newGroup(parent, "Settings");
        settingsGroup.setLayout(new GridLayout(3, true));

        newFillingLabel(settingsGroup, "Name").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, dataModel.getNewJavaAppName(), 2);

        newFillingLabel(settingsGroup, "Type").setFont(JFaceResources.getBannerFont());
        newFillingLabel(settingsGroup, "Java", 2);
    }

    private void createApplicationSourceSection(Composite parent) {
        Group applicationSourceGroup = newGroup(parent, "Application Source");
        applicationSourceGroup.setLayout(new GridLayout(3, true));

        newFillingLabel(
                applicationSourceGroup,
                "Specify the S3 location where your application source will be stored.",
                3);

        newFillingLabel(applicationSourceGroup, "Bucket name").setFont(JFaceResources.getBannerFont());
        bucketNameCombo = newCombo(applicationSourceGroup, 2);
        bucketNameCombo.setEnabled(false);
        bucketNameSelected.setValue(false);

        newFillingLabel(applicationSourceGroup, "Key name").setFont(JFaceResources.getBannerFont());
        keyNameText = newText(applicationSourceGroup, "", 2);

        keyNameTextDecoration = newControlDecoration(keyNameText,
                "Enter a valid S3 key name");
    }

    private void loadS3BucketsAsync() {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                bucketNameCombo.setItems(new String[] {LOADING});
                bucketNameCombo.select(0);
            }
        });

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    AmazonS3 client = AwsToolkitCore.getClientFactory().getS3Client();

                    List<Bucket> allBuckets = client.listBuckets();
                    if (allBuckets.isEmpty()) {
                        bucketNameCombo.setItems(new String[] {NONE_FOUND});
                        bucketNameSelected.setValue(false);

                    } else {
                        List<String> allBucketNames = new LinkedList<>();
                        for (Bucket bucket : allBuckets) {
                            allBucketNames.add(bucket.getName());
                        }
                        bucketNameCombo.setItems(allBucketNames.toArray(new String[allBucketNames.size()]));
                        bucketNameCombo.select(0);
                        bucketNameCombo.setEnabled(true);

                        bucketNameSelected.setValue(true);
                    }

                } catch (Exception e) {
                    OpsWorksPlugin.getDefault().reportException(
                            "Failed to load S3 buckets.", e);
                }
            }
        });
    }

    private void createEnvironmentVariablesSection(Composite parent) {
        final Group envVarGroup = newGroup(parent, "Environment Variables");
        envVarGroup.setLayout(new GridLayout(4, true));

        newFillingLabel(
                envVarGroup,
                "Specify the name and value of the environment variables for your application",
                4);

        // Input box for new variables
        final Text keyText = newText(envVarGroup);
        keyText.setMessage("Key");
        final Text valueText = newText(envVarGroup);
        valueText.setMessage("Value");
        final Button checkBox = newCheckbox(envVarGroup, "Protected value", 1);

        Button addButton = new Button(envVarGroup, SWT.PUSH);
        addButton.setImage(OpsWorksPlugin.getDefault().getImageRegistry()
                .get(OpsWorksExplorerImages.IMG_ADD));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (keyText.getText().isEmpty() && valueText.getText().isEmpty()) {
                    return;
                }

                EnvironmentVariable newVar = new EnvironmentVariable();
                newVar.setKey(keyText.getText());
                newVar.setValue(valueText.getText());
                newVar.setSecure(checkBox.getSelection());

                dataModel.addEnvironmentVariable(newVar);
                addNewEnvironmentVariable(envVarGroup, newVar);

                keyText.setText("");
                valueText.setText("");
                checkBox.setSelection(false);

                NewAppConfigurationComposite.this.layout(true, true);
            }
        });

        // Added variables
        for (EnvironmentVariable envVar : dataModel.getEnvironmentVariables()) {
            addNewEnvironmentVariable(envVarGroup, envVar);
        }
    }

    private void addNewEnvironmentVariable(Group parentGroup, EnvironmentVariable variable) {
        newFillingLabel(parentGroup, variable.getKey()).setFont(JFaceResources.getBannerFont());
        newFillingLabel(parentGroup, variable.getValue());
        newFillingLabel(parentGroup, variable.isSecure() ? "(Protected)" : "(Not protected)", 2);
    }

    private void createCustomDomainsSection(Composite parent) {
        final Group domainsGroup = newGroup(parent, "Custom Domains");
        domainsGroup.setLayout(new GridLayout(2, true));

        newFillingLabel(
                domainsGroup,
                "Add one or more custom domains for your application",
                2);

        // Input box for new domains
        final Text domainText = newText(domainsGroup);
        domainText.setMessage("www.example.com");

        Button addButton = new Button(domainsGroup, SWT.PUSH);
        addButton.setImage(OpsWorksPlugin.getDefault().getImageRegistry()
                .get(OpsWorksExplorerImages.IMG_ADD));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if (domainText.getText().isEmpty()) {
                    return;
                }

                String newDomain = domainText.getText();

                dataModel.addCustomDomain(newDomain);
                newFillingLabel(domainsGroup, "[+] " + newDomain, 2);

                NewAppConfigurationComposite.this.layout(true, true);
            }
        });

        // Added domains
        for (String domain : dataModel.getCustomDomains()) {
            newFillingLabel(domainsGroup, "[+] " + domain, 2);
        }
    }

    private void createSslSettingsSection(Composite parent) {
        Group sslSettingsGroup = newGroup(parent, "SSL Settings");

        enableSslCheckBox = newCheckbox(sslSettingsGroup, "SSL enabled", 1);

        final Group advancedSslSettings = newGroup(sslSettingsGroup, "");
        advancedSslSettings.setLayout(new GridLayout(2, true));

        newFillingLabel(advancedSslSettings, "SSL certificate").setFont(JFaceResources.getBannerFont());
        certText = newText(advancedSslSettings, "");
        certText.setEnabled(false);

        newFillingLabel(advancedSslSettings, "SSL certificate key").setFont(JFaceResources.getBannerFont());
        privateKeyText = newText(advancedSslSettings, "");
        privateKeyText.setEnabled(false);

        newFillingLabel(advancedSslSettings, "SSL certificates of CA").setFont(JFaceResources.getBannerFont());
        chainText = newText(advancedSslSettings, "");
        chainText.setEnabled(false);

        enableSslCheckBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                boolean enabled = enableSslCheckBox.getSelection();
                certText.setEnabled(enabled);
                privateKeyText.setEnabled(enabled);
                chainText.setEnabled(enabled);
            }
        });
    }

}
