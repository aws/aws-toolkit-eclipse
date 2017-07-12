package com.amazonaws.eclipse.codedeploy.deploy.wizard.page;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newControlDecoration;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newGroup;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newRadioButton;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newText;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateMetadataModel;
import com.amazonaws.eclipse.codedeploy.appspec.model.AppspecTemplateParameter;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.validator.ContextPathValidator;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.validator.GenericTemplateParameterValidator;
import com.amazonaws.eclipse.codedeploy.deploy.wizard.page.validator.ServerHttpPortValidator;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;

/**
 * The UI composite for displaying the configuration options for a specific
 * appspec template. The composites for all the available templates are
 * stacked onto the same stack-layout area.
 */
class AppspecTemplateConfigComposite extends Composite {

    private static final String CONTEXT_PATH_ANCHOR_TEXT = "##CONTEXT_PATH##";
    private static final String HTTP_PORT_ANCHOR_TEXT = "##HTTP_PORT##";
    private static final String DEPLOY_TO_ROOT_ANCHOR_TEXT = "##DEPLOY_TO_ROOT##";

    private static final String CONTEXT_PATH_DEFAULT = "application";
    private static final String HTTP_PORT_DEFAULT = "8080";

    /* Data model */
    private final AppspecTemplateMetadataModel templateModel;
    private final DataBindingContext bindingContext;
    private final AggregateValidationStatus aggregateValidationStatus;

    /*
     * Special UI widgets for HTTP port and context-path configuration
     */
    private Label serverUrlPreviewLabel;

    private ISWTObservableValue deployToContextPathRadioButtonObservable;
    private Text contextPathText;
    private ISWTObservableValue contextPathTextObservable;

    private Text httpPortText;
    private ISWTObservableValue httpPortTextObservable;

    /**
     * UI widgets for generic parameters
     */
    private final List<ParameterInputGroup> parameterInputGroups = new LinkedList<>();

    /**
     * @see #setValidationStatusChangeListener(IChangeListener)
     * @see #removeValidationStatusChangeListener()
     */
    private IChangeListener validationStatusChangeListener;

    public AppspecTemplateConfigComposite(Composite parent, int style,
            AppspecTemplateMetadataModel templateModel) {
        super(parent, style);

        this.templateModel = templateModel;
        this.bindingContext = new DataBindingContext();
        this.aggregateValidationStatus = new AggregateValidationStatus(
                bindingContext, AggregateValidationStatus.MAX_SEVERITY);

        setLayout(new GridLayout(2, false));
        createControls(this);
    }

    public AppspecTemplateMetadataModel getTemplateModel() {
        return templateModel;
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

    /**
     * @return a map of all the template parameter values, keyed by the anchor
     *         text of each parameter.
     */
    public Map<String, String> getAllParameterValues() {
        Map<String, String> values = new HashMap<>();

        if (templateModel.isUseDefaultContextPathParameter()) {
            values.put(DEPLOY_TO_ROOT_ANCHOR_TEXT,
                    (Boolean)deployToContextPathRadioButtonObservable.getValue()
                    ? "false" : "true");

            values.put(CONTEXT_PATH_ANCHOR_TEXT, (String)contextPathTextObservable.getValue());
        }

        if (templateModel.isUseDefaultHttpPortParameter()) {
            values.put(HTTP_PORT_ANCHOR_TEXT, (String)httpPortTextObservable.getValue());
        }

        for (ParameterInputGroup genericParamInput : parameterInputGroups) {
            values.put(
                    genericParamInput.getParameterModel().getSubstitutionAnchorText(),
                    genericParamInput.getParameterValue());
        }

        return values;
    }

    private void createControls(Composite parent) {
        Label descriptionLabel = new Label(parent, SWT.NONE);
        setItalicFont(descriptionLabel);
        descriptionLabel.setText(
                toUIString(templateModel.getTemplateDescription()));

        if (templateModel.isUseDefaultContextPathParameter()
                || templateModel.isUseDefaultHttpPortParameter()) {
            createServerUrlPreviewLabel(parent);
        }

        if (templateModel.isUseDefaultContextPathParameter()) {
            createContextPathConfigurationSection(parent);
        }

        if (templateModel.isUseDefaultHttpPortParameter()) {
            createHttpPortConfigurationSection(parent);
        }

        if (templateModel.getParameters() != null) {
            for (AppspecTemplateParameter parameter : templateModel.getParameters()) {
                ParameterInputGroup parameterInputGroup = new ParameterInputGroup(
                        parent, parameter, bindingContext);
                parameterInputGroups.add(parameterInputGroup);
            }
        }
    }

    private static String toUIString(String str) {
        return str == null ? "n/a" : str;
    }

    private void createServerUrlPreviewLabel(Composite composite) {
        Group group = newGroup(composite, "", 2);
        group.setLayout(new GridLayout(2, false));

        newLabel(group, "Server URL:");

        serverUrlPreviewLabel = newFillingLabel(group,
                String.format("http://{ec2-public-dns}:%s/", HTTP_PORT_DEFAULT));
        setBoldFont(serverUrlPreviewLabel);

        Label label = newFillingLabel(group, "Your application will be available " +
                "via this endpoint after the deployment.", 2);
        setItalicFont(label);
    }

    private void refreshServerUrlPreviewLabel() {
        boolean useContextPath = (Boolean) deployToContextPathRadioButtonObservable
                .getValue();
        String contextPath = contextPathText.getText();
        String httpPort = httpPortText.getText();

        serverUrlPreviewLabel.setText(
                useContextPath
                    ? String.format("http://{ec2-public-dns}:%s/%s/", httpPort, contextPath)
                    : String.format("http://{ec2-public-dns}:%s/", httpPort)
        );
    }

    private void createContextPathConfigurationSection(Composite composite) {
        Group group = newGroup(composite, "", 2);
        group.setLayout(new GridLayout(2, false));

        newRadioButton(group,
                "Deploy application to server root", 2, true,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        contextPathText.setEnabled(false);
                        updateValidationStatus();
                        refreshServerUrlPreviewLabel();
                    }
                }
        );

        Button deployToContextPathRadioButton = newRadioButton(group,
                "Deploy application to context path", 1, false,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        contextPathText.setEnabled(true);
                        updateValidationStatus();
                        refreshServerUrlPreviewLabel();
                    }
                }
        );

        deployToContextPathRadioButtonObservable = SWTObservables
                .observeSelection(deployToContextPathRadioButton);

        contextPathText = newText(group);
        contextPathText.setEnabled(false);
        contextPathText.setText(CONTEXT_PATH_DEFAULT);

        contextPathTextObservable = SWTObservables.observeText(contextPathText, SWT.Modify);
        bindingContext.bindValue(contextPathTextObservable, contextPathTextObservable);

        ChainValidator<String> contextPathValidationProvider = new ChainValidator<>(
                contextPathTextObservable,
                deployToContextPathRadioButtonObservable, //enabler
                new ContextPathValidator("Invalid context path."));
        bindingContext.addValidationStatusProvider(contextPathValidationProvider);

        ControlDecoration contextPathTextDecoration = newControlDecoration(
                contextPathText,
                "Enter a valid context path for the application.");
        new DecorationChangeListener(
                contextPathTextDecoration,
                contextPathValidationProvider.getValidationStatus());

        contextPathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                refreshServerUrlPreviewLabel();
            }
        });
    }

    private void createHttpPortConfigurationSection(Composite composite) {
        Group group = newGroup(composite, "", 2);
        group.setLayout(new GridLayout(2, false));

        Label nameLabel = newLabel(group, "Application server HTTP port:");
        nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        httpPortText = newText(group);
        httpPortText.setText(HTTP_PORT_DEFAULT);

        httpPortTextObservable = SWTObservables.observeText(httpPortText, SWT.Modify);
        bindingContext.bindValue(httpPortTextObservable, httpPortTextObservable);

        ChainValidator<String> httpPortValidationProvider = new ChainValidator<>(
                httpPortTextObservable,
                new ServerHttpPortValidator("Invalid HTTP port."));
        bindingContext.addValidationStatusProvider(httpPortValidationProvider);

        ControlDecoration httpPortTextDecoration = newControlDecoration(
                httpPortText,
                "Enter a valid HTTP port for the Tomcat server.");
        new DecorationChangeListener(
                httpPortTextDecoration,
                httpPortValidationProvider.getValidationStatus());

        httpPortText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                refreshServerUrlPreviewLabel();
            }
        });

        Label httpPortLabel = newFillingLabel(group,
                "You might need to setup authbind in order to " +
                "allow servlet container to listen on priviliged ports (0 - 1023).", 2);
        setItalicFont(httpPortLabel);
    }

    private static class ParameterInputGroup extends Group {

        private final AppspecTemplateParameter parameter;

        private final DataBindingContext bindingContext;

        private Text valueInputText;
        private ControlDecoration valueInputTextDecoration;
        private ISWTObservableValue valueInputTextObservable;

        public ParameterInputGroup(Composite parent,
                AppspecTemplateParameter parameter, DataBindingContext bindingContext) {
            super(parent, SWT.NONE);

            if (parameter == null) {
                throw new NullPointerException("parameter must not be null.");
            }
            if (bindingContext == null) {
                throw new NullPointerException("bindingContext must not be null.");
            }

            this.parameter = parameter;
            this.bindingContext = bindingContext;

            createControls(parent);
        }

        public AppspecTemplateParameter getParameterModel() {
            return parameter;
        }

        public String getParameterValue() {
            return (String)valueInputTextObservable.getValue();
        }

        private void createControls(Composite composite) {
            GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
            gridData.horizontalSpan = 2;
            this.setLayoutData(gridData);
            this.setLayout(new GridLayout(2, false));

            this.setText(parameter.getName());

            Label nameLabel = newLabel(this, parameter.getSubstitutionAnchorText());
            nameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

            valueInputText = newText(this);
            valueInputText.setText(parameter.getDefaultValueAsString());

            valueInputTextObservable = SWTObservables.observeText(valueInputText, SWT.Modify);
            bindingContext.bindValue(valueInputTextObservable, valueInputTextObservable);

            ChainValidator<String> paramValidationStatusProvider = new ChainValidator<>(
                    valueInputTextObservable,
                    new GenericTemplateParameterValidator(parameter));
            bindingContext.addValidationStatusProvider(paramValidationStatusProvider);

            valueInputTextDecoration = newControlDecoration(
                    valueInputText,
                    String.format("Invalid value for parameter %s (%s).",
                            parameter.getSubstitutionAnchorText(), parameter.getName()));
            new DecorationChangeListener(
                    valueInputTextDecoration,
                    paramValidationStatusProvider.getValidationStatus());

        }

        @Override
        protected void checkSubclass() {}

    }

    /*
     * Font resources
     */

    private Font italicFont;
    private Font boldFont;

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    private void setBoldFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.BOLD);
        }
        boldFont = new Font(Display.getDefault(), fontData);
        control.setFont(boldFont);
    }

    @Override
    public void dispose() {
        if (italicFont != null)
            italicFont.dispose();
        if (boldFont != null)
            boldFont.dispose();
        super.dispose();
    }
}