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
package com.amazonaws.eclipse.identitymanagement.user;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import com.amazonaws.eclipse.core.AWSClientFactory;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.explorer.identitymanagement.CreateUserAction;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class UserEditor extends EditorPart implements IRefreshable {

    private EditorInput userEditorInput;
    private UserTable userTable;
    private UserSummary userSummary;
    private UserPermission userPermission;
    private GroupsForUser groups;
    private AmazonIdentityManagement iam;

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        this.userEditorInput = (EditorInput) input;
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
        form.setText(getFormTitle());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_USER));
        form.getBody().setLayout(new GridLayout());
        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().add(new CreateUserAction(iam, this));
        form.getToolBarManager().update(true);

        SashForm sash = new SashForm(form.getBody(), SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        sash.setLayout(new GridLayout());
        createTableSection(sash, toolkit);
        createTabsSection(sash, toolkit);
        userTable.setUserSummary(userSummary);
        userTable.setUserPermission(userPermission);
        userTable.setGroups(groups);

    }

    private String getFormTitle() {
        String formTitle = userEditorInput.getName();
        return formTitle;
    }

    private void createTabsSection(Composite parent, FormToolkit toolkit) {
        Composite tabsSection = toolkit.createComposite(parent, SWT.NONE);
        tabsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabsSection.setLayout(new FillLayout());
        TabFolder tabFolder = new TabFolder(tabsSection, SWT.BORDER);
        Rectangle clientArea = parent.getClientArea();
        tabFolder.setLocation(clientArea.x, clientArea.y);

        TabItem summaryTab = new TabItem(tabFolder, SWT.NONE);
        summaryTab.setText("Summary");
        userSummary = new UserSummary(iam, tabFolder, toolkit);
        summaryTab.setControl(userSummary);

        TabItem permissionTab = new TabItem(tabFolder, SWT.NONE);
        permissionTab.setText("Permissions");
        userPermission = new UserPermission(iam, tabFolder, toolkit);
        permissionTab.setControl(userPermission);


        TabItem groupTab = new TabItem(tabFolder, SWT.NONE);
        groupTab.setText("Groups");
        groups = new GroupsForUser(iam, tabFolder, toolkit);
        groupTab.setControl(groups);
    }

    private void createTableSection(Composite parent, FormToolkit toolkit) {
        userTable = new UserTable(iam, parent, toolkit, userEditorInput);
    }

    @Override
    public void setFocus() {
    }

    private class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            userTable.refresh();
            userSummary.refresh();
            groups.refresh();
        }
    }

    @Override
    public void refreshData() {
        userTable.refresh();
    }

    protected AmazonIdentityManagement getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(userEditorInput.getAccountId());
        return clientFactory.getIAMClientByEndpoint(userEditorInput.getRegionEndpoint());
    }


}
