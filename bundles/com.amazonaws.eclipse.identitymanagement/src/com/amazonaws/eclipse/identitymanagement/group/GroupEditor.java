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
package com.amazonaws.eclipse.identitymanagement.group;

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
import com.amazonaws.eclipse.explorer.identitymanagement.CreateGroupAction;
import com.amazonaws.eclipse.explorer.identitymanagement.EditorInput;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;

public class GroupEditor extends EditorPart implements IRefreshable {

    private EditorInput groupEditorInput;
    private GroupSummary groupSummary;
    private UsersInGroup usersInGroup;
    private GroupTable groupTable;
    private GroupPermissions groupPermissions;
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
        this.groupEditorInput = (EditorInput)input;
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
        form.setImage(AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_GROUP));
        form.getBody().setLayout(new GridLayout());

        SashForm sash = new SashForm(form.getBody(),  SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        sash.setLayout(new GridLayout());

        createTableSection(sash, toolkit);
        createTabsSection(sash, toolkit);

        form.getToolBarManager().add(new RefreshAction());
        form.getToolBarManager().add(new CreateGroupAction(iam, this));
        form.getToolBarManager().update(true);
        groupTable.setGroupSummary(groupSummary);
        groupTable.setUsersInGroup(usersInGroup);
        groupTable.setGroupPermissions(groupPermissions);

    }

    private String getFormTitle() {
        String formTitle = groupEditorInput.getName();
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
        groupSummary = new GroupSummary(iam, tabFolder, toolkit);
        summaryTab.setControl(groupSummary);

        TabItem usersTab = new TabItem(tabFolder, SWT.NONE);
        usersTab.setText("Users");
        usersInGroup = new UsersInGroup(iam, tabFolder, toolkit, groupEditorInput);
        usersTab.setControl(usersInGroup);

        TabItem permissionsTab = new TabItem(tabFolder, SWT.NONE);
        permissionsTab.setText("Permissions");
        groupPermissions = new GroupPermissions(iam, tabFolder, toolkit);
        permissionsTab.setControl(groupPermissions);
    }

    private void createTableSection(Composite parent, FormToolkit toolkit) {
        groupTable = new GroupTable(iam, parent, toolkit);
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
            groupSummary.refresh();
            groupTable.refresh();
            usersInGroup.refresh();
        }
    }

    @Override
    public void refreshData() {
        groupTable.refresh();
    }

    protected AmazonIdentityManagement getClient() {
        AWSClientFactory clientFactory = AwsToolkitCore.getClientFactory(groupEditorInput.getAccountId());
        return clientFactory.getIAMClientByEndpoint(groupEditorInput.getRegionEndpoint());
    }

}
