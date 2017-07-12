/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.explorer.lambda;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;

public class FunctionEditor extends EditorPart {
    private FunctionEditorInput functionEditorInput;
    private Text functionNameLabel;
    private Text arnLabel;
    private Text runtimeLabel;
    private Text handlerLabel;
    private Text roleLabel;
    private Text lastUpdatedLabel;
    private Text memorySizeLabel;
    private Text timeoutLabel;
    private Text codeSizeLabel;
    private Text descriptionLabel;

    private FunctionLogsTable functionLogsTable;
    private FunctionTagsTable functionTagsTable;
    private FunctionEnvVarsTable functionEnvVarsTable;

    private RefreshAction refreshAction;

    @Override
    public void doSave(IProgressMonitor monitor) {}

    @Override
    public void doSaveAs() {}

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
        this.functionEditorInput = (FunctionEditorInput) input;
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
        ScrolledForm form = new ScrolledForm(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        form.setExpandHorizontal(true);
        form.setExpandVertical(true);
        form.setBackground(toolkit.getColors().getBackground());
        form.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        form.setFont(JFaceResources.getHeaderFont());

        form.setText(getFormTitle());
        toolkit.decorateFormHeading(form.getForm());
        form.setImage(getDefaultImage());
        form.getBody().setLayout(new GridLayout());


        createSummarySection(form.getBody(), toolkit);
        createTabsSection(form.getBody(), toolkit);

        refreshAction = new RefreshAction();
        form.getToolBarManager().add(refreshAction);
        form.getToolBarManager().update(true);

        new LoadFunctionConfigurationThread().start();
    }

    private void createSummarySection(Composite parent, FormToolkit toolkit) {
        GridDataFactory gridDataFactory = GridDataFactory.swtDefaults()
            .align(SWT.FILL, SWT.TOP).grab(true, false).minSize(200, SWT.DEFAULT).hint(200, SWT.DEFAULT);

        Composite composite = toolkit.createComposite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        composite.setLayout(new GridLayout(4, false));

        toolkit.createLabel(composite, "Function Name:");
        functionNameLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(functionNameLabel);

        toolkit.createLabel(composite, "ARN:");
        arnLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(arnLabel);

        toolkit.createLabel(composite, "Runtime:");
        runtimeLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(runtimeLabel);

        toolkit.createLabel(composite, "Role:");
        roleLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(roleLabel);

        toolkit.createLabel(composite, "Handler:");
        handlerLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(handlerLabel);

        toolkit.createLabel(composite, "Last Updated:");
        lastUpdatedLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(lastUpdatedLabel);

        toolkit.createLabel(composite, "Memory Size (MB):");
        memorySizeLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(memorySizeLabel);

        toolkit.createLabel(composite, "Timeout (sec):");
        timeoutLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        gridDataFactory.applyTo(timeoutLabel);

        toolkit.createLabel(composite, "Code Size:");
        codeSizeLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE);
        toolkit.createLabel(composite, "");
        toolkit.createLabel(composite, "");

        Label l = toolkit.createLabel(composite, "Description:");
        gridDataFactory.copy().hint(100, SWT.DEFAULT).minSize(1, SWT.DEFAULT).align(SWT.LEFT, SWT.TOP).grab(false, false).applyTo(l);
        descriptionLabel = new Text(composite, SWT.READ_ONLY | SWT.NONE | SWT.MULTI | SWT.WRAP);
        gridDataFactory.copy().span(3, 1).applyTo(descriptionLabel);
    }

    private void createTabsSection(Composite parent, FormToolkit toolkit) {
        Composite tabsSection = toolkit.createComposite(parent, SWT.NONE);
        tabsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabsSection.setLayout(new FillLayout());

        TabFolder tabFolder = new TabFolder (tabsSection, SWT.BORDER);

        Rectangle clientArea = parent.getClientArea();
        tabFolder.setLocation(clientArea.x, clientArea.y);

        TabItem eventsTab = new TabItem(tabFolder, SWT.NONE);
        eventsTab.setText("Logs");
        functionLogsTable = new FunctionLogsTable(tabFolder, toolkit, functionEditorInput);
        eventsTab.setControl(functionLogsTable);

        TabItem tagsTab = new TabItem(tabFolder, SWT.NONE);
        tagsTab.setText("Tags");
        functionTagsTable = new FunctionTagsTable(tabFolder, toolkit, functionEditorInput);
        tagsTab.setControl(functionTagsTable);

        TabItem envVarsTab = new TabItem(tabFolder, SWT.NONE);
        envVarsTab.setText("Environment Variables");
        functionEnvVarsTable = new FunctionEnvVarsTable(tabFolder, toolkit, functionEditorInput);
        envVarsTab.setControl(functionEnvVarsTable);

        tabFolder.pack();
    }

    @Override
    public void setFocus() {}

    private String getFormTitle() {
        return functionEditorInput.getName() + "-" + RegionUtils.getRegion(functionEditorInput.getRegionId()).getName();
    }

    private class RefreshAction extends Action {
        public RefreshAction() {
            this.setText("Refresh");
            this.setToolTipText("Refresh function information");
            this.setImageDescriptor(AwsToolkitCore.getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_REFRESH));
        }

        @Override
        public void run() {
            new LoadFunctionConfigurationThread().start();
            functionLogsTable.refresh();
            functionTagsTable.refresh();
            functionEnvVarsTable.refresh();
        }
    }

    private class LoadFunctionConfigurationThread extends Thread {

        private GetFunctionResult getFunction(String functionName) {
            return functionEditorInput.getLambdaClient().getFunction(new GetFunctionRequest()
                    .withFunctionName(functionName));
        }

        @Override
        public void run() {
            try {
                final GetFunctionResult getFunctionResult = getFunction(functionEditorInput.getFunctionName());
                final FunctionConfiguration configuration = getFunctionResult.getConfiguration();

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        functionNameLabel.setText(configuration.getFunctionName());
                        arnLabel.setText(configuration.getFunctionArn());
                        runtimeLabel.setText(configuration.getRuntime());
                        handlerLabel.setText(valueOrDefault(configuration.getHandler(), "N/A"));
                        roleLabel.setText(valueOrDefault(configuration.getRole(), "N/A"));
                        lastUpdatedLabel.setText(valueOrDefault(configuration.getLastModified(), "N/A"));
                        memorySizeLabel.setText(valueOrDefault(configuration.getMemorySize().toString(), "N/A"));
                        timeoutLabel.setText(valueOrDefault(configuration.getTimeout().toString(), "N/A"));
                        codeSizeLabel.setText(valueOrDefault(configuration.getCodeSize().toString(), "N/A"));

                        descriptionLabel.setText(valueOrDefault(configuration.getDescription(), ""));

                        functionNameLabel.getParent().layout();
                        functionNameLabel.getParent().getParent().layout(true);

                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.WARNING, LambdaPlugin.PLUGIN_ID, "Unable to describe function " + functionEditorInput.getName(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
            }
        }

        private String valueOrDefault(String value, String defaultValue) {
            if (value != null) return value;
            else return defaultValue;
        }
    }

}
