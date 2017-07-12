package com.amazonaws.eclipse.opsworks.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.JsonStringValidator;
import com.amazonaws.eclipse.opsworks.deploy.wizard.model.DeployProjectToOpsworksWizardDataModel;

public class DeploymentActionConfigurationPage extends WizardPageWithOnEnterHook {

    /* Data model */
    private final DeployProjectToOpsworksWizardDataModel dataModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /* UI widgets */
    private Text deployCommentText;
    private Text customChefJsonText;
    private ControlDecoration customChefJsonTextDecoration;

    private ISWTObservableValue deployCommentTextObservable;
    private ISWTObservableValue customChefJsonTextObservable;

    public DeploymentActionConfigurationPage(DeployProjectToOpsworksWizardDataModel dataModel) {
        super("Deployment Action Configuration");
        setTitle("Deployment Action Configuration");
        setDescription("");

        this.dataModel = dataModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(3, true));

        newFillingLabel(composite, "Command").setFont(JFaceResources.getBannerFont());
        newFillingLabel(composite, "Deploy", 2);

        newFillingLabel(composite, "Comment (optional)").setFont(JFaceResources.getBannerFont());
        deployCommentText = newTextArea(composite, 2, 4);

        newFillingLabel(composite, "Custom Chef JSON (optional)").setFont(JFaceResources.getBannerFont());
        customChefJsonText = newTextArea(composite, 2, 4);

        customChefJsonTextDecoration = newControlDecoration(
                customChefJsonText,
                "Enter a valid JSON String.");

        bindControls();
        initializeValidators();

        setControl(composite);
    }

    private void bindControls() {
        deployCommentTextObservable = SWTObservables
                .observeText(deployCommentText, SWT.Modify);
        bindingContext.bindValue(
                deployCommentTextObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToOpsworksWizardDataModel.DEPLOY_COMMENT));

        customChefJsonTextObservable = SWTObservables
                .observeText(customChefJsonText, SWT.Modify);
        bindingContext.bindValue(
                customChefJsonTextObservable,
                PojoObservables.observeValue(
                        dataModel,
                        DeployProjectToOpsworksWizardDataModel.CUSTOM_CHEF_JSON));
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
        ChainValidator<String> customChefJsonValidator = new ChainValidator<>(
                customChefJsonTextObservable,
                new JsonStringValidator("Please enter a valid JSON String", true));
        bindingContext.addValidationStatusProvider(customChefJsonValidator);

        new DecorationChangeListener(customChefJsonTextDecoration,
                customChefJsonValidator.getValidationStatus());
    }

    private static Text newTextArea(Composite parent, int colspan, int lines) {
        Text text = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL );
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        gridData.heightHint = lines * text.getLineHeight();
        text.setLayoutData(gridData);
        return text;
    }

    @Override
    protected void onEnterPage() {
    }


}
