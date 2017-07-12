/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.simpledb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.navigator.CommonActionProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.explorer.ContentProviderRegistry;
import com.amazonaws.eclipse.explorer.simpledb.SimpleDBExplorerNodes.DomainNode;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataRequest;
import com.amazonaws.services.simpledb.model.DomainMetadataResult;

public class SimpleDBNavigatorActionProvider extends CommonActionProvider {
    @Override
    public void fillContextMenu(final IMenuManager menu) {
        IStructuredSelection selection = (IStructuredSelection) getContext().getSelection();

        if (selection.getFirstElement() == SimpleDBRootElement.ROOT_ELEMENT) {
            menu.add(new CreateDomainAction());
        } else if (selection.getFirstElement() instanceof DomainNode) {

            List<String> domainNames = new ArrayList<>();
            Iterator iterator = selection.iterator();
            while (iterator.hasNext()) {
                Object next = iterator.next();
                if (next instanceof DomainNode) {
                    DomainNode domainNode = (DomainNode)next;
                    domainNames.add(domainNode.getName());
                }
            }


            menu.add(new CreateDomainAction());
            menu.add(new DeleteDomainAction(domainNames));
            menu.add(new Separator());

            menu.add(new OpenSQLScrapbookAction());
            DomainNode domainNode = (DomainNode)selection.getFirstElement();
            menu.add(new OpenDataTableEditorAction(domainNode.getName()));

            menu.add(new Separator());
            menu.add(new ShowDomainMetadataAction(domainNode.getName()));
        }
    }

    private static class ShowDomainMetadataAction extends Action {

        private final String domain;

        public ShowDomainMetadataAction(final String domain) {
            this.setText("View domain metadata");
            this.setToolTipText("View metadata for this domain");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_ICON));
            this.setAccelerator(SWT.ALT | findKeyCode("ENTER"));
            this.domain = domain;
        }

        @Override
        public void run() {
            new DomainMetadataDialog().open();
        }

        class DomainMetadataDialog extends MessageDialog {

            protected DomainMetadataDialog() {
                super(Display.getDefault().getActiveShell(), "Domain Metadata", AwsToolkitCore.getDefault()
                        .getImageRegistry().get(AwsToolkitCore.IMAGE_AWS_ICON),
                        "Domain Metadata for " + ShowDomainMetadataAction.this.domain, MessageDialog.NONE, new String[] { "OK" }, 0);
            }

            @Override
            protected Control createDialogArea(final Composite parent) {
                Composite composite = new Composite(parent, SWT.NONE);
                GridLayout layout = new GridLayout(2, false);
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                composite.setLayout(layout);
                GridData data = new GridData(GridData.FILL_BOTH);
                data.horizontalSpan = 2;
                composite.setLayoutData(data);

                Label title = new Label(composite, SWT.None);
                title.setText("Domain Metadata for " + ShowDomainMetadataAction.this.domain);
                title.setLayoutData(data);

                DomainMetadataResult domainMetadata = AwsToolkitCore.getClientFactory().getSimpleDBClient()
                        .domainMetadata(new DomainMetadataRequest().withDomainName(ShowDomainMetadataAction.this.domain));

                new Label(composite, SWT.NONE).setText("Item count: ");
                new Label(composite, SWT.NONE).setText("" + domainMetadata.getItemCount());
                new Label(composite, SWT.READ_ONLY).setText("Total item name size (bytes): ");
                new Label(composite, SWT.READ_ONLY).setText("" + domainMetadata.getItemNamesSizeBytes());
                new Label(composite, SWT.READ_ONLY).setText("Distinct attribute names: ");
                new Label(composite, SWT.READ_ONLY).setText("" + domainMetadata.getAttributeNameCount());
                new Label(composite, SWT.READ_ONLY).setText("Total attribute name size (bytes): ");
                new Label(composite, SWT.READ_ONLY).setText("" + domainMetadata.getAttributeNamesSizeBytes());
                new Label(composite, SWT.READ_ONLY).setText("Name / value pairs: ");
                new Label(composite, SWT.READ_ONLY).setText("" + domainMetadata.getAttributeValueCount());
                new Label(composite, SWT.READ_ONLY).setText("Total name / value pair size (bytes): ");
                new Label(composite, SWT.READ_ONLY).setText("" + domainMetadata.getAttributeValuesSizeBytes());

                return composite;
            }

        }
    }


    private static class CreateDomainAction extends Action {
        public CreateDomainAction() {
            this.setText("Create New Domain");
            this.setToolTipText("Create a new Amazon SimpleDB domain");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_ADD));
        }

        @Override
        public void run() {
            CreateDomainDialog createDomainDialog = new CreateDomainDialog();
            if (createDomainDialog.open() != 0) {
                return;
            }

            try {
                AmazonSimpleDB sdb = AwsToolkitCore.getClientFactory().getSimpleDBClient();
                sdb.createDomain(new CreateDomainRequest(createDomainDialog.getDomainName()));
                ContentProviderRegistry.refreshAllContentProviders();
            } catch (Exception e) {
                AwsToolkitCore.getDefault().reportException("Unable to create domain '" + createDomainDialog.getDomainName() + "'", e);
            }
        }

        private class CreateDomainDialog extends MessageDialog {

            private String domainName;

            public CreateDomainDialog() {
                super(Display.getDefault().getActiveShell(),
                        "Create New SimpleDB Domain", null, "Enter the name for your new SimpleDB domain.",
                        MessageDialog.INFORMATION, new String[] {"OK", "Cancel"}, 0);
            }

            public String getDomainName() {
                return this.domainName;
            }

            @Override
            protected Control createCustomArea(final Composite parent) {
                Composite composite = new Composite(parent, SWT.NONE);
                composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
                composite.setLayout(new GridLayout(2, false));

                new Label(composite, SWT.NONE).setText("Domain Name:");
                final Text domainNameText = new Text(composite, SWT.BORDER);
                domainNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                domainNameText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(final ModifyEvent e) {
                        CreateDomainDialog.this.domainName = domainNameText.getText();
                        updateControls();
                    }
                });

                return composite;
            }

            private void updateControls() {
                boolean isValid = (this.domainName != null && this.domainName.length() > 0);

                Button okButton = this.getButton(0);
                if (okButton != null) {
                    okButton.setEnabled(isValid);
                }
            }

            @Override
            protected void createButtonsForButtonBar(final Composite parent) {
                super.createButtonsForButtonBar(parent);
                updateControls();
            }
        }
    }

    private static class DeleteDomainAction extends Action {
        private final List<String> domainNames;

        public DeleteDomainAction(final List<String> domainNames) {
            this.domainNames = domainNames;

            this.setText("Delete Domain" + (domainNames.size() > 1 ? "s" : ""));
            this.setToolTipText("Delete the selected Amazon SimpleDB domains");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REMOVE));
        }

        @Override
        public void run() {
            Dialog dialog = newConfirmationDialog("Delete selected domains?", "Are you sure you want to delete the selected Amazon SimpleDB domains?");
            if (dialog.open() != 0) {
                return;
            }

            AmazonSimpleDB simpleDB = new AwsToolkitCore().getClientFactory().getSimpleDBClient();
            for (String domainName : this.domainNames) {
                try {
                    simpleDB.deleteDomain(new DeleteDomainRequest(domainName));
                } catch (Exception e) {
                    AwsToolkitCore.getDefault().logError("Unable to delete domain '" + domainName + "'", e);
                }
            }

            ContentProviderRegistry.refreshAllContentProviders();
        }

        private Dialog newConfirmationDialog(final String title, final String message) {
            return new MessageDialog(Display.getDefault().getActiveShell(), title, null, message, MessageDialog.WARNING, new String[] {"OK", "Cancel"}, 0);
        }
    }
}
