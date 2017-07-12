/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.identitymanagement.role;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.explorer.identitymanagement.CreateRoleAction;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class RoleEditor extends EditorPart implements IRefreshable  {

    private EditorInput roleEditorInput;
    private RoleSummary roleSummary;
    private RolePermissions rolePermissions;
    private RoleTable roleTable;
    private RoleTrustRelationships roleTrustRelationships;
    private AmazonIdentityManagement iam;

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        this.roleEditorInput = (EditorInput)input;
        iam = getClient();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(Composite parent) {

        FormToolkit toolkit = new FormToolkit(Display.getDefault());
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(getFormTitle());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_ROLE));
        form.getBody().setLayout(new GridLayout());

        SashForm sash = new SashForm(form.getBody(),  SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        sash.setLayout(new GridLayout());
        createTablesSection(sash, toolkit);
        createTabsSection(sash, toolkit);
        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().add(new CreateRoleAction(iam, this));
        form.getToolBarManager().update(true);
        roleTable.setRoleSummary(roleSummary);
        roleTable.setRolePermissions(rolePermissions);
        roleTable.setRoleTrustRelationships(roleTrustRelationships);
    }

    private String getFormTitle() {
        String formTitle = roleEditorInput.getName();
        return formTitle;
    }

    private void createTabsSection(Composite parent, FormToolkit toolkit) {

        Composite tabsSection = toolkit.createComposite(parent, SWT.NONE);
        tabsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabsSection.setLayout(new FillLayout());

        TabFolder tabFolder = new TabFolder (tabsSection, SWT.BORDER);

        Rectangle clientArea = parent.getClientArea();
        tabFolder.setLocation(clientArea.x, clientArea.y);

         TabItem summaryTab = new TabItem(tabFolder, SWT.NONE);
         summaryTab.setText("Summary");
         roleSummary = new RoleSummary(iam, tabFolder, toolkit);
         summaryTab.setControl(roleSummary);

         TabItem permissionTab = new TabItem(tabFolder, SWT.NONE);
         permissionTab.setText("Permissions");
         rolePermissions = new RolePermissions(iam, tabFolder, toolkit);
         permissionTab.setControl(rolePermissions);

         TabItem trustRelationshipsTab = new TabItem(tabFolder, SWT.NONE);
         trustRelationshipsTab.setText("Trust Relationships");
         roleTrustRelationships = new RoleTrustRelationships(iam, tabFolder, toolkit);
         trustRelationshipsTab.setControl(roleTrustRelationships);
    }

    private void createTablesSection(Composite parent, FormToolkit toolkit) {
        roleTable = new RoleTable(iam, parent, toolkit);
    }

    @Override
    public void setFocus() {}

    private class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            roleTable.refresh();
            roleSummary.refresh();
        }
    }

    @Override
    public void refreshData() {
        roleTable.refresh();
    }

    protected AmazonIdentityManagement getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(roleEditorInput.getAccountId());
        return clientFactory.getIAMClientByEndpoint(roleEditorInput.getRegionEndpoint());
    }
}
