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
package com.amazonaws.eclipse.core.ui;

import static com.amazonaws.eclipse.core.model.ImportFileDataModel.P_FILE_PATH;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newPushButton;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.validator.NoopValidator;
import com.amazonaws.eclipse.core.widget.TextComplex;

/**
 * A reusable File import widget composite.
 */
public class ImportFileComposite extends Composite {
    private TextComplex filePathComplex;
    private Button browseButton;

    private ImportFileComposite(Composite parent, DataBindingContext context,
            ImportFileDataModel dataModel, IValidator validator, String textLabel,
            String buttonLabel, ModifyListener modifyListener, String textMessage) {
        super(parent, SWT.NONE);
        setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        setLayout(new GridLayout(3, false));
        createControl(context, dataModel, validator, textLabel, buttonLabel, modifyListener, textMessage);
    }

    @Override
    public void setEnabled(boolean enabled) {
        filePathComplex.setEnabled(enabled);
        browseButton.setEnabled(enabled);
    }

    @NonNull
    public static ImportFileCompositeBuilder builder(
            @NonNull Composite parent,
            @NonNull DataBindingContext dataBindingContext,
            @NonNull ImportFileDataModel dataModel) {
        return new ImportFileCompositeBuilder(parent, dataBindingContext, dataModel);
    }

    private void createControl(DataBindingContext context, ImportFileDataModel dataModel,
            IValidator filePathValidator, String textLabel, String buttonLabel,
            ModifyListener modifyListener, String textMessage) {

        filePathComplex = TextComplex.builder(this, context, PojoProperties.value(P_FILE_PATH).observe(dataModel))
                .labelValue(textLabel)
                .addValidator(filePathValidator)
                .defaultValue(dataModel.getFilePath())
                .modifyListener(modifyListener)
                .textMessage(textMessage)
                .build();

        browseButton = newPushButton(this, buttonLabel);
    }

    public void setFilePath(String filePath) {
        filePathComplex.setText(filePath);
    }

    /**
     * The browse Button opens a general file selection dialog.
     */
    private void setButtonListenerToBrowseFile() {
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.SINGLE);
                String path = dialog.open();
                if (path != null) {
                    filePathComplex.setText(path);
                }
            }
        });
    }

    /**
     * The browse Button opens a dialog to only allow to select folders under the workspace.
     */
    private void setButtonListenerToBrowseWorkspaceDir() {
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(),
                        ResourcesPlugin.getWorkspace().getRoot(), false, "Choose Base Directory");
                dialog.showClosedProjects(false);

                int buttonId = dialog.open();
                if (buttonId == IDialogConstants.OK_ID) {
                    Object[] resource = dialog.getResult();
                    if (resource != null && resource.length > 0) {
                        String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
                                .generateVariableExpression("workspace_loc", ((IPath) resource[0]).toString());
                        filePathComplex.setText(fileLoc);
                    }
                }
            }
        });
    }

    /**
     * The browse Button opens a dialog to only allow to select files under the workspace.
     */
    private void setButtonListenerToBrowseWorkspaceFile() {
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
                        new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
                dialog.setTitle("Choose File");
                dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());

                int buttonId = dialog.open();
                if (buttonId == IDialogConstants.OK_ID) {
                    Object[] resource = dialog.getResult();
                    if (resource != null && resource.length > 0) {
                        String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
                                .generateVariableExpression("workspace_loc",
                                        ((IResource) resource[0]).getFullPath().toString());
                        filePathComplex.setText(fileLoc);
                    }
                }
            }
        });
    }

    /**
     * The browse Button opens a dialog to only allow to select project under the workspace.
     */
    private void setButtonListenerToBrowseWorkspaceProject() {
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
                ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
                dialog.setTitle("Choose project");
                dialog.setMessage("Choose the project for the job");
                try {
                    dialog.setElements(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects());
                } catch (JavaModelException jme) {
                    AwsToolkitCore.getDefault().logError(jme.getMessage(), jme);
                }
                if (dialog.open() == Window.OK) {
                    filePathComplex.setText(((IJavaProject) dialog.getFirstResult()).getElementName());
                }
            }
        });
    }

    public static final class ImportFileCompositeBuilder {
        private final Composite parent;
        private final DataBindingContext context;
        private final ImportFileDataModel dataModel;

        // Optional parameters
        private IValidator filePathValidator = new NoopValidator();
        private ModifyListener modifyListener;
        private String textLabel = "Import:";
        private String buttonLabel = "Browse";
        private String textMessage = "";

        private ImportFileCompositeBuilder(
                @NonNull Composite parent,
                @NonNull DataBindingContext context,
                @NonNull ImportFileDataModel dataModel) {
            this.parent = parent;
            this.context = context;
            this.dataModel = dataModel;
        }

        public ImportFileCompositeBuilder filePathValidator(IValidator filePathValidator) {
            this.filePathValidator = filePathValidator;
            return this;
        }

        public ImportFileCompositeBuilder textLabel(String textLabel) {
            this.textLabel = textLabel;
            return this;
        }

        public ImportFileCompositeBuilder buttonLabel(String buttonLabel) {
            this.buttonLabel = buttonLabel;
            return this;
        }

        public ImportFileCompositeBuilder modifyListener(ModifyListener modifyListener) {
            this.modifyListener = modifyListener;
            return this;
        }

        public ImportFileCompositeBuilder textMessage(String textMessage) {
            this.textMessage = textMessage;
            return this;
        }

        /**
         * Build a general file importer component.
         */
        public ImportFileComposite build() {
            ImportFileComposite composite = new ImportFileComposite(
                    parent, context, dataModel,
                    filePathValidator, textLabel, buttonLabel, modifyListener, textMessage);
            composite.setButtonListenerToBrowseFile();
            return composite;
        }

        public ImportFileComposite buildWorkspaceDirBrowser() {
            ImportFileComposite composite = new ImportFileComposite(
                    parent, context, dataModel,
                    filePathValidator, textLabel, buttonLabel, modifyListener, textMessage);
            composite.setButtonListenerToBrowseWorkspaceDir();
            return composite;
        }

        public ImportFileComposite buildWorkspaceFileBrowser() {
            ImportFileComposite composite = new ImportFileComposite(
                    parent, context, dataModel,
                    filePathValidator, textLabel, buttonLabel, modifyListener, textMessage);
            composite.setButtonListenerToBrowseWorkspaceFile();
            return composite;
        }

        public ImportFileComposite buildWorkspaceProjectBrowser() {
            ImportFileComposite composite = new ImportFileComposite(
                    parent, context, dataModel,
                    filePathValidator, textLabel, buttonLabel, modifyListener, textMessage);
            composite.setButtonListenerToBrowseWorkspaceProject();
            return composite;
        }
    }
}
