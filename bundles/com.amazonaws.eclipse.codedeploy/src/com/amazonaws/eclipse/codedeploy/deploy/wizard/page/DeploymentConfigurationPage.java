package com.amazonaws.eclipse.codedeploy.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCheckbox;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLink;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.ServiceAPIUtils;
import com.amazonaws.eclipse.codedeploy.UrlConstants;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.model.DeployProjectToCodeDeployWizardDataModel;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.databinding.BooleanValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

public class DeploymentConfigurationPage extends WizardPageWithOnEnterHook {

    /* Data model */
    private final DeployProjectToCodeDeployWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    private IObservableValue deploymentConfigSelected = new WritableValue();
    private IObservableValue bucketNameSelected = new WritableValue();

    /* UI widgets */

    // Select deployment config
    private Combo deploymentConfigCombo;

    // Select S3 bucket
    private Combo bucketNameCombo;
    private Link bucketNameSelectionMessageLabel;

    // ignoreApplicationStopFailures
    private Button ignoreApplicationStopFailuresCheckBox;

    /* Constants */

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public DeploymentConfigurationPage(DeployProjectToCodeDeployWizardDataModel dataModel) {
        super("Deployment Configuration");
        setTitle("Deployment Configuration");
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

        createDeploymentConfigSelection(composite);
        createS3BucketSelection(composite);

        bindControls();

        setControl(composite);
        setPageComplete(false);
    }

    @Override
    public void onEnterPage() {
        loadDeploymentConfigsAsync();
        loadS3BucketsAsync();
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
                deploymentConfigSelected,
                new BooleanValidator("Please select the deployment config")));
        bindingContext.addValidationStatusProvider(new ChainValidator<Boolean>(
                bucketNameSelected,
                new BooleanValidator("Please select the S3 bucket name")));
    }

    private void bindControls() {
        ISWTObservableValue deploymentConfigComboObservable = SWTObservables
                .observeSelection(deploymentConfigCombo);
        bindingContext.bindValue(
                deploymentConfigComboObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToCodeDeployWizardDataModel.DEPLOYMENT_CONFIG_NAME_PROPERTY));

        ISWTObservableValue bucketNameComboObservable = SWTObservables
                .observeSelection(bucketNameCombo);
        bindingContext.bindValue(
                bucketNameComboObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToCodeDeployWizardDataModel.BUCKET_NAME_PROPERTY));

        ISWTObservableValue ignoreApplicationStopFailuresCheckBoxObservable = SWTObservables
                .observeSelection(ignoreApplicationStopFailuresCheckBox);
        bindingContext.bindValue(
                ignoreApplicationStopFailuresCheckBoxObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToCodeDeployWizardDataModel.IGNORE_APPLICATION_STOP_FAILURES_PROPERTY));
    }

    private void createDeploymentConfigSelection(Composite composite) {
        Group deploymentConfigGroup = newGroup(composite, "Select CodeDeploy deployment config:");
        deploymentConfigGroup.setLayout(new GridLayout(1, false));

        deploymentConfigCombo = newCombo(deploymentConfigGroup);
        deploymentConfigCombo.setEnabled(false);

        deploymentConfigSelected.setValue(false);

        newLink(deploymentConfigGroup, UrlConstants.webLinkListener,
                "<a href=\"" +
                "http://docs.aws.amazon.com/codedeploy/latest/userguide/how-to-create-deployment-configuration.html" +
                "\">Create new deployment configuration.</a>",
                1);

        ignoreApplicationStopFailuresCheckBox = newCheckbox(deploymentConfigGroup,
                "Ignore ApplicationStop step failures.", 1);
    }

    private void loadDeploymentConfigsAsync() {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                deploymentConfigCombo.setItems(new String[] {LOADING});
                deploymentConfigCombo.select(0);
            }
        });

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    String endpoint = dataModel.getRegion().getServiceEndpoints()
                            .get(ServiceAbbreviations.CODE_DEPLOY);
                    AmazonCodeDeploy client = AwsToolkitCore.getClientFactory()
                            .getCodeDeployClientByEndpoint(endpoint);

                    List<String> configNames = ServiceAPIUtils.getAllDeploymentConfigNames(client);
                    if (configNames.isEmpty()) {
                        deploymentConfigCombo.setItems(new String[] {NONE_FOUND});

                        deploymentConfigSelected.setValue(false);

                    } else {
                        deploymentConfigCombo.setItems(configNames.toArray(new String[configNames.size()]));
                        deploymentConfigCombo.select(0);
                        deploymentConfigCombo.setEnabled(true);

                        deploymentConfigSelected.setValue(true);
                    }

                } catch (Exception e) {
                    CodeDeployPlugin.getDefault().reportException(
                            "Failed to load deployment configs.", e);
                }
            }
        });
    }

    private void createS3BucketSelection(Composite composite) {
        Group bucketGroup = newGroup(composite, "Select S3 bucket name:");
        bucketGroup.setLayout(new GridLayout(1, false));

        bucketNameCombo = newCombo(bucketGroup);
        bucketNameCombo.setEnabled(false);

        bucketNameSelected.setValue(false);

        bucketNameSelectionMessageLabel = newLink(bucketGroup,
                UrlConstants.webLinkListener,
                "The S3 bucket should be located at the same region " +
                "as the target Amazon EC2 instances. " +
                "For more information, see " +
                "<a href=\"" +
                "http://docs.aws.amazon.com/codedeploy/latest/userguide/how-to-push-revision.html" +
                "\">AWS CodeDeploy User Guide</a>.",
                1);
        setItalicFont(bucketNameSelectionMessageLabel);
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
                        bucketNameSelectionMessageLabel.setText(
                                "No S3 bucket is found. " +
                                "Please create one before making a deployment");

                        // Re-calculate UI layout
                        ((Composite)getControl()).layout();

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
                    CodeDeployPlugin.getDefault().reportException(
                            "Failed to load S3 buckets.", e);
                }
            }
        });
    }

    private Font italicFont;

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    @Override
    public void dispose() {
        if (italicFont != null)
            italicFont.dispose();
        super.dispose();
    }
}
