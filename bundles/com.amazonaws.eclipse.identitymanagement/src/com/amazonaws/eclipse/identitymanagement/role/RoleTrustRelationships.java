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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.eclipse.identitymanagement.IdentityManagementPlugin;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;

public class RoleTrustRelationships extends Composite {

    private TrustedEntityTable trustEntitiesTable;
    private Role role;
    private Button editPolicyButton;

    public RoleTrustRelationships(final AmazonIdentityManagement iam, Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        this.setLayout(new GridLayout(1, false));
        this.setBackground(toolkit.getColors().getBackground());

        Section trustedEntitiesSection = toolkit.createSection(this, Section.TITLE_BAR);
        trustedEntitiesSection.setText("Trusted Entities");
        trustedEntitiesSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite client = toolkit.createComposite(trustedEntitiesSection, SWT.WRAP);
        client.setLayoutData(new GridData(GridData.FILL_BOTH));
        client.setLayout(new GridLayout(2, false));
        trustEntitiesTable = new TrustedEntityTable(client, toolkit);
        trustEntitiesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        editPolicyButton = toolkit.createButton(client, "Edit Trust Relationship", SWT.BUTTON1);
        editPolicyButton.setEnabled(false);
        editPolicyButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        editPolicyButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                EditTrustRelationshipDialog dialog = new EditTrustRelationshipDialog(iam, Display.getCurrent().getActiveShell(), role);
                dialog.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        trustedEntitiesSection.setClient(client);
    }

    public void setRole(Role role) {

        this.role = role;
        String assumeRolePolicyDocument = null;
        if (role != null && role.getAssumeRolePolicyDocument() != null) {
            editPolicyButton.setEnabled(true);
            try {
                assumeRolePolicyDocument = URLDecoder.decode(role.getAssumeRolePolicyDocument(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                   StatusManager.getManager().handle(
                           new Status(IStatus.ERROR, IdentityManagementPlugin.PLUGIN_ID, "Error show trust relationship for role "
                           + role.getRoleName() + ": " + e.getMessage()), StatusManager.SHOW);
            }
        } else {
            editPolicyButton.setEnabled(false);
            trustEntitiesTable.setPrincipals(null);
            return;
        }
        List<Principal> principals = new LinkedList<>();

        for (Statement statement : Policy.fromJson(assumeRolePolicyDocument).getStatements()) {
            principals.addAll(statement.getPrincipals());
        }
        trustEntitiesTable.setPrincipals(principals);
    }

}
