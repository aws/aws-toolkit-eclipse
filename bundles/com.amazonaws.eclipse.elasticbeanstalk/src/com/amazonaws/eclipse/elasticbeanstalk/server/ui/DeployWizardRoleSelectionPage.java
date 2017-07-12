/*
 * Copyright 2015 Amazon Technologies, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.deploy.DeployWizardDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.LoadIamRolesJob;
import com.amazonaws.eclipse.elasticbeanstalk.jobs.LoadResourcesCallback;
import com.amazonaws.eclipse.elasticbeanstalk.util.BeanstalkConstants;
import com.amazonaws.eclipse.elasticbeanstalk.util.OnUiThreadProxyFactory;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.util.StringUtils;

public class DeployWizardRoleSelectionPage extends AbstractDeployWizardPage {

    public final RoleWidgetBuilder instanceRoleWidgetBuilder = new RoleWidgetBuilder()
            .withDefaultRole(BeanstalkConstants.DEFAULT_INSTANCE_ROLE_NAME)
            .withDataBindingFieldName(DeployWizardDataModel.INSTANCE_ROLE_NAME).withTrustEntity("ec2.amazonaws.com");

    public final RoleWidgetBuilder serviceRoleWidgetBuilder = new RoleWidgetBuilder()
            .withDefaultRole(BeanstalkConstants.DEFAULT_SERVICE_ROLE_NAME)
            .withDataBindingFieldName(DeployWizardDataModel.SERVICE_ROLE_NAME)
            .withTrustEntity("beanstalk.amazonaws.com");

    private static final String SERVICE_ROLE_LABEL_TEXT = "Service Role";
    private static final String INSTANCE_PROFILE_ROLE_LABEL_TEXT = "Instance Profile Role";
    private static final String SERVICE_ROLE_PERMISSIONS_DOC_URL = "https://docs.aws.amazon.com/console/elasticbeanstalk/roles";
    private static final String IAM_ROLE_PERMISSIONS_DOC_URL = "http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.iam.roles.logs.html#iampolicy";

    private Composite wizardPageRoot;
    private final LoadResourcesCallback<Role> loadIamRoleCallback;
    /** Only accessed through the UI thread. No need for synchronization **/
    private boolean hasInsufficientIamPermissionDialogBeenShown;

    protected DeployWizardRoleSelectionPage(DeployWizardDataModel wizardDataModel) {
        super(wizardDataModel);
        // At this point the user can finish the wizard. All remaining pages are optional
        setComplete(true);
        this.loadIamRoleCallback = OnUiThreadProxyFactory.getProxy(LoadResourcesCallback.class, new LoadIamRolesCallback());
    }

    @Override
    public String getPageTitle() {
        return "Permissions";
    }

    @Override
    public String getPageDescription() {
        return "Select an instance profile and service role for your AWS Elastic Beanstalk environment";
    }

    @Override
    public Composite createComposite(Composite parent, IWizardHandle handle) {
        this.hasInsufficientIamPermissionDialogBeenShown = false;
        wizardHandle = handle;

        setDefaultsInDataModel();

        handle.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry()
                .getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));

        this.wizardPageRoot = new Composite(parent, SWT.NONE);
        wizardPageRoot.setLayout(new GridLayout(1, false));

        initializeValidators();
        new LoadIamRolesJob(loadIamRoleCallback).schedule();

        return wizardPageRoot;
    }

    private class LoadIamRolesCallback implements LoadResourcesCallback<Role> {

        @Override
        public void onSuccess(List<Role> roles) {
            createRoleComboBoxControls(roles);
        }

        @Override
        public void onInsufficientPermissions() {
            if (!hasInsufficientIamPermissionDialogBeenShown) {
                hasInsufficientIamPermissionDialogBeenShown = true;
                IAMOperationNotAllowedErrorDialog dialog = new IAMOperationNotAllowedErrorDialog(
                        Display.getDefault().getActiveShell());
                int code = dialog.open();

                if (code == IAMOperationNotAllowedErrorDialog.OK_BUTTON_CODE
                        || code == IAMOperationNotAllowedErrorDialog.CLOSE) {
                    new LoadIamRolesJob(loadIamRoleCallback).schedule();
                }
            } else {
                createManualRoleControls();
            }
        }

        // TODO When we fail to load IAM roles for reasons other then permissions issue then we should
        // probably throw an error dialog up. It may be that our logic for determining a service error
        // is a permissions failure has gotten stale and needs to be updated. Not entirely sure what the
        // experience for this should look like, hence the todo
        @Override
        public void onFailure() {
            onInsufficientPermissions();
        }

        /**
         * If we do have IAM permissions we display the users roles in a dropdown to choose from
         */
        private void createRoleComboBoxControls(List<Role> roles) {
            wizardDataModel.setSkipIamRoleAndInstanceProfileCreation(false);

            createInstanceProfileRoleLabel(wizardPageRoot);
            instanceRoleWidgetBuilder.setupComboViewer(wizardPageRoot, roles);
            newInstanceRoleDescLink(wizardPageRoot);

            createServiceRoleLabel(wizardPageRoot);
            serviceRoleWidgetBuilder.setupComboViewer(wizardPageRoot, roles);
            newServiceRoleDescLink(wizardPageRoot);

            // Redraw
            wizardPageRoot.layout(true);
        }

        /**
         * If we don't have IAM permissions to list roles then we display manual Text views that allow
         * users to manually specify the role.
         */
        private void createManualRoleControls() {
            wizardDataModel.setSkipIamRoleAndInstanceProfileCreation(true);

            createInstanceProfileRoleLabel(wizardPageRoot);
            instanceRoleWidgetBuilder.setupManualControls(wizardPageRoot);
            newInstanceRoleDescLink(wizardPageRoot);

            createServiceRoleLabel(wizardPageRoot);
            serviceRoleWidgetBuilder.setupManualControls(wizardPageRoot);
            newServiceRoleDescLink(wizardPageRoot);

            // Redraw
            wizardPageRoot.layout(true);
        }

        private void createInstanceProfileRoleLabel(Composite composite) {
            newFillingLabel(composite, INSTANCE_PROFILE_ROLE_LABEL_TEXT);
        }

        private void createServiceRoleLabel(Composite composite) {
            newFillingLabel(composite, SERVICE_ROLE_LABEL_TEXT);
        }

        /**
         * Description and hyperlink for what the Instance Profile role is needed for
         */
        private void newInstanceRoleDescLink(Composite composite) {
            adjustLinkLayout(newLink(composite,
                    "If you choose not to use the default role, you must grant the relevant permissions to Elastic Beanstalk. "
                            + "See the <a href=\"" + IAM_ROLE_PERMISSIONS_DOC_URL
                            + "\">AWS Elastic Beanstalk Developer Guide</a> for more details."));
        }

        /**
         * Description and hyperlink for what the Service role is needed for
         */
        private void newServiceRoleDescLink(Composite composite) {
            adjustLinkLayout(newLink(composite,
                    "A service role allows the Elastic Beanstalk service to monitor environment resources on your behalf. "
                            + "See <a href=\""
                            + SERVICE_ROLE_PERMISSIONS_DOC_URL
                            + "\">Service Roles, Instance Profiles, and User Policies</a> in the Elastic Beanstalk developer guide for details."));
        }

    }

    /**
     * Set the default values for the roles and vpc in the data model to be reflected in the UI when the
     * model is bound to a control
     */
    private void setDefaultsInDataModel() {
        if (StringUtils.isNullOrEmpty(wizardDataModel.getInstanceRoleName())) {
            wizardDataModel.setInstanceRoleName(BeanstalkConstants.DEFAULT_INSTANCE_ROLE_NAME);
        }
        if (StringUtils.isNullOrEmpty(wizardDataModel.getServiceRoleName())) {
            wizardDataModel.setServiceRoleName(BeanstalkConstants.DEFAULT_SERVICE_ROLE_NAME);
        }
    }

    /**
     * Class to hold data that differs between different role types (i.e. service role vs instance
     * role) and build appropriate widgets based on those differences
     */
    private class RoleWidgetBuilder {

        private String defaultRole;
        private String dataBindingFieldName;
        private String trustEntity;

        public RoleWidgetBuilder withDefaultRole(String defaultRoleName) {
            this.defaultRole = defaultRoleName;
            return this;
        }

        public RoleWidgetBuilder withDataBindingFieldName(String dataBindingFieldName) {
            this.dataBindingFieldName = dataBindingFieldName;
            return this;
        }

        public RoleWidgetBuilder withTrustEntity(String trustEntity) {
            this.trustEntity = trustEntity;
            return this;
        }

        /**
         * Create the ComboViewer, setup databinding for it, and select the default role
         *
         * @param roles
         *            List of IAM roles in the user's account
         */
        public void setupComboViewer(Composite composite, List<Role> roles) {
            bindRoleComboView(newRoleComboView(composite, transformRoleList(roles)), dataBindingFieldName);
        }

        /**
         * Setup manual text controls when we don't have sufficient IAM permisisons to list roles so
         * user can still explicitly specify a role
         */
        public void setupManualControls(Composite composite) {
            IObservableValue instanceRoleObservable = SWTObservables.observeText(newText(composite), SWT.Modify);
            getBindingContext().bindValue(instanceRoleObservable,
                    PojoObservables.observeValue(wizardDataModel, dataBindingFieldName));
        }

        /**
         * Setup data-binding for the ComboViewer
         *
         * @param comboViewer
         * @param fieldName
         *            Field name in Data Model to bind to
         */
        private void bindRoleComboView(ComboViewer comboViewer, String fieldName) {
            IObservableValue roleObservable = ViewersObservables.observeSingleSelection(comboViewer);
            IObservableValue observable = PojoObservables.observeValue(wizardDataModel, fieldName);
            getBindingContext().bindValue(roleObservable, observable);
        }

        private ComboViewer newRoleComboView(Composite composite, List<String> roles) {
            ComboViewer roleComboViewer = new ComboViewer(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
            roleComboViewer.setContentProvider(ArrayContentProvider.getInstance());
            roleComboViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
            roleComboViewer.setInput(roles);
            // Custom Label provider to clearly indicate the default role in the ComboViewer
            roleComboViewer.setLabelProvider(new LabelProvider() {
                @Override
                public String getText(Object element) {
                    if (isDefaultRoleName(element)) {
                        return "(Default) " + element;
                    }
                    return super.getText(element);
                }

                private boolean isDefaultRoleName(Object element) {
                    return element instanceof String && defaultRole.equals(element);
                }
            });

            return roleComboViewer;
        }

        /**
         * Transform the list of Role objects to a list of role names and filter out any roles that
         * don't have the required trust entity. Default role is always appended to the beginning of
         * the list (injected if it doesn't exist yet)
         *
         * @param roles
         *            List of {@link Role} objects to transform
         * @return List of strings containing all role names that are appropriate for this role type
         */
        private List<String> transformRoleList(List<Role> roles) {
            List<String> stringRoles = new ArrayList<>(roles.size() + 1);
            stringRoles.add(defaultRole);
            for (Role role : roles) {
                if (!isDefaultRole(role) && hasRequiredTrustEntity(role)) {
                    stringRoles.add(role.getRoleName());
                }
            }
            return stringRoles;
        }

        /**
         * We only display those roles that can be assumed by the appropriate entity. For instance
         * profile roles this is EC2, for the service role this is Beanstalk itself
         */
        private boolean hasRequiredTrustEntity(Role role) {
            return role.getAssumeRolePolicyDocument().contains(trustEntity);
        }

        private boolean isDefaultRole(Role role) {
            return defaultRole.equals(role.getRoleName());
        }

        /**
         * DataBindingContext is setup in {@link AbstractDeployWizardPage}
         *
         * @return The current data binding context
         */
        private DataBindingContext getBindingContext() {
            return DeployWizardRoleSelectionPage.this.bindingContext;
        }
    }

    private void adjustLinkLayout(Link link) {
        adjustLinkLayout(link, 1);
    }

}
