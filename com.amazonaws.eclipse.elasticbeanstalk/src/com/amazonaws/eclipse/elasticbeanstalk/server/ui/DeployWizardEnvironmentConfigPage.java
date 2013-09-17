/*
- * Copyright 2010-2012 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.WebLinkListener;
import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.databinding.NotEmptyValidator;
import com.amazonaws.eclipse.ec2.databinding.ValidKeyPairValidator;
import com.amazonaws.eclipse.ec2.ui.keypair.KeyPairComposite;
import com.amazonaws.eclipse.elasticbeanstalk.ConfigurationOptionConstants;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPublishingUtils;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DefaultRole;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.NoInvalidNameCharactersValidator;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;

class DeployWizardEnvironmentConfigPage extends AbstractDeployWizardPage {

    private static final String IAM_ROLE_PERMISSIONS_DOC_URL = "http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.iam.roles.logs.html#iampolicy";

    private KeyPairComposite keyPairComposite;
    private Button usingCnameButton;
    private Text cname;
    private Button usingKeyPair;
    private Button incrementalDeploymentButton;
    private ComboViewer iamRoleComboViewer;
    private Text healthCheckText;

    private ISWTObservableValue usingKeyPairObservable;
    private ISWTObservableValue usingCnameObservable;
    private ISWTObservableValue healthCheckURLObservable;
    private ISWTObservableValue sslCertObservable;
    private ISWTObservableValue snsTopicObservable;


    public DeployWizardEnvironmentConfigPage(DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
        setComplete(true);
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        wizardHandle = handle;
        handle.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        createKeyPairComposite(composite);
        createSSLCertControls(composite);
        createCNAMEControls(composite);
        createHealthCheckURLControls(composite);
        createSNSTopicControls(composite);
        createIamRoleControls(composite);
        newLabel(composite, "");
        createIncrementalDeploymentControls(composite);

        bindControls();
        initializeDefaults();

        new LoadIamRolesJob("Loading IAM Roles").schedule();

        return composite;
    }

    private void createIamRoleControls(Composite parent) {
        newLabel(parent, "IAM role");

        iamRoleComboViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        iamRoleComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        iamRoleComboViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DefaultRole) return "Default (aws-elasticbeanstalk-ec2-role)";
                if (element instanceof Role) return ((Role)element).getRoleName();
                else return "";
            }
        });
        iamRoleComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


        Link link = new Link(parent, SWT.WRAP);
        WebLinkListener webLinkListener = new WebLinkListener();
        link.addListener(SWT.Selection, webLinkListener);

        link.setText("If you choose not to use the default role, you must grant the relevant permissions to Elastic Beanstalk. " +
                "See the <a href=\"" + IAM_ROLE_PERMISSIONS_DOC_URL + "\">AWS Elastic Beanstalk Developer Guide</a> for more details.");

        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.widthHint = 200;
        link.setLayoutData(gridData);
    }

    // Check the environment type. If it is single instance, we disable the health check URL.
    @Override
    public void enter() {
        if (wizardDataModel.getEnvironmentType() != null && wizardDataModel.getEnvironmentType().equals(ConfigurationOptionConstants.SINGLE_INSTANCE)) {
            healthCheckText.setEnabled(false);
        } else {
            healthCheckText.setEnabled(true);
        }
    }

    private void createKeyPairComposite(Composite composite) {
        usingKeyPair = newCheckbox(composite, "Deploy with a key pair", 1);
        keyPairComposite = new KeyPairComposite(composite, AwsToolkitCore.getDefault().getCurrentAccountId(), wizardDataModel.getRegion());
        wizardDataModel.setKeyPairComposite(keyPairComposite);

        usingKeyPair.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                keyPairComposite.setEnabled(usingKeyPair.getSelection());
            }
        });

        GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
        layoutData.heightHint = 140;
        keyPairComposite.setLayoutData(layoutData);
    }

    private void createSSLCertControls(Composite composite) {
        newLabel(composite, "SSL certificate Id");
        Text text = newText(composite, "");
        sslCertObservable = SWTObservables.observeText(text, SWT.Modify);
    }

    private void createCNAMEControls(Composite composite) {
        usingCnameButton = newCheckbox(composite, "Assign CNAME to new server", 1);
        cname = newText(composite);

        usingCnameButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cname.setEnabled(usingCnameButton.getSelection());
            }
        });
    }

    private void createIncrementalDeploymentControls(Composite parent) {
        incrementalDeploymentButton = newCheckbox(parent, "Use incremental deployment", 1);
        incrementalDeploymentButton.setSelection(true);
    }

    private void createHealthCheckURLControls(Composite composite) {
        newLabel(composite, "Application health check URL");
        healthCheckText = newText(composite, "");
        healthCheckURLObservable = SWTObservables.observeText(healthCheckText, SWT.Modify);
    }

    private void createSNSTopicControls(Composite composite) {
        newLabel(composite, "Email address for notifications");
        Text text = newText(composite, "");
        snsTopicObservable = SWTObservables.observeText(text, SWT.Modify);
    }

    private void initializeDefaults() {
        usingCnameObservable.setValue(false);
        usingKeyPairObservable.setValue(false);
        keyPairComposite.setEnabled(false);
        cname.setEnabled(false);
        cname.setText("");
        sslCertObservable.setValue("");
        snsTopicObservable.setValue("");
        healthCheckURLObservable.setValue("");

        // No change event is necessarily fired from the above updates, so we
        // fire one manually in order to display the appropriate button enablement
        changeListener.handleChange(null);
    }

    /**
     * Creates validation bindings for the controls on this page.
     */
    private void bindControls() {
        initializeValidators();

        // Key pair
        usingKeyPairObservable = SWTObservables.observeSelection(usingKeyPair);
        bindingContext.bindValue(usingKeyPairObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.USING_KEY_PAIR), null, null);
        IViewerObservableValue keyPairSelectionObservable = ViewersObservables.observeSingleSelection(keyPairComposite
                .getViewer());

        bindingContext.bindValue(keyPairSelectionObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.KEY_PAIR), null, null);
        ChainValidator<String> keyPairValidator = new ChainValidator<String>(
                keyPairSelectionObservable, usingKeyPairObservable,
                new ValidKeyPairValidator(AwsToolkitCore.getDefault().getCurrentAccountId()));
        bindingContext.addValidationStatusProvider(keyPairValidator);

        usingCnameObservable = SWTObservables.observeSelection(usingCnameButton);
        bindingContext.bindValue(usingCnameObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.USING_CNAME), null, null)
                .updateTargetToModel();
        bindingContext.bindValue(SWTObservables.observeText(cname, SWT.Modify),
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.CNAME), null, null)
                .updateTargetToModel();

        // SSL cert
        bindingContext.bindValue(sslCertObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.SSL_CERTIFICATE_ID));

        // CNAME
        // TODO: make CNAME conform to exact spec, check for in-use
        ChainValidator<String> chainValidator = new ChainValidator<String>(
                SWTObservables.observeText(cname, SWT.Modify), usingCnameObservable, new NotEmptyValidator(
                        "CNAME cannot be empty."), new NoInvalidNameCharactersValidator("Invalid characters in CNAME."));
        bindingContext.addValidationStatusProvider(chainValidator);
        ControlDecoration cnameDecoration = newControlDecoration(cname, "Enter a CNAME to launch your server");
        new DecorationChangeListener(cnameDecoration, chainValidator.getValidationStatus());

        // Health check URL
        bindingContext.bindValue(healthCheckURLObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.HEALTH_CHECK_URL));

        // SNS topic "email address"
        bindingContext.bindValue(snsTopicObservable,
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.SNS_ENDPOINT));

        // Incremental deployment
        bindingContext.bindValue(SWTObservables.observeSelection(incrementalDeploymentButton),
                PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.INCREMENTAL_DEPLOYMENT));

        // IAM Role
        IViewerObservableValue iamRoleComboObservable = ViewersObservables.observeSingleSelection(iamRoleComboViewer);
        bindingContext.bindValue(iamRoleComboObservable,
            PojoObservables.observeValue(wizardDataModel, DeployWizardDataModel.IAM_ROLE));


    }

    @Override
    protected void radioButtonSelected(Object sourceButton) {
    }

    @Override
    public String getPageTitle() {
        return "Advanced configuration";
    }

    @Override
    public String getPageDescription() {
        return "Specify advanced properties for your environment";
    }

    private final class LoadIamRolesJob extends Job {
        private LoadIamRolesJob(String name) {
            super(name);
            setUser(false);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                // Just grab the IAM client for the active account/region...
                // This wizard always works with the active account and IAM only has one region (except for GovCloud)
                AmazonIdentityManagement iam = AwsToolkitCore.getClientFactory().getIAMClient();

                final List<Role> roles = new LinkedList<Role>();
                final DefaultRole defaultRole = new DefaultRole();
                roles.add(defaultRole);

                ListRolesRequest request = new ListRolesRequest();
                ListRolesResult result = null;
                do {
                    result = iam.listRoles(request);
                    for (Role role : result.getRoles()) {
                        if (!role.getRoleName().equals(ElasticBeanstalkPublishingUtils.DEFAULT_ROLE_NAME)) {
                            roles.add(role);
                        }
                    }

                    if (result.isTruncated()) request.setMarker(result.getMarker());
                } while (result.isTruncated());

                Display.getDefault().asyncExec(new Runnable() {
                    public void run() {
                        iamRoleComboViewer.setInput(roles);
                        iamRoleComboViewer.setSelection(new StructuredSelection(defaultRole));
                    }
                });
                return Status.OK_STATUS;
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID,
                    "Unable to query AWS Identity and Access Management for available instance profiles", e);
                ElasticBeanstalkPlugin.getDefault().getLog().log(status);
                return status;
            }
        }
    }

}
