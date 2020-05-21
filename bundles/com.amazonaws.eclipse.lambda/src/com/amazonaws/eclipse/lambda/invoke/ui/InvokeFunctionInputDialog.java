/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.lambda.invoke.ui;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newRadioButton;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;
import com.amazonaws.eclipse.lambda.ui.LambdaJavaProjectUtil;
import com.amazonaws.eclipse.lambda.upload.wizard.util.UploadFunctionUtil;

public class InvokeFunctionInputDialog extends Dialog {

    public static final int INVOKE_BUTTON_ID = IDialogConstants.OK_ID;

    private static final int PREFERRED_WIDTH = 600;
    private static final int PREFERRED_HEIGHT = 400;

    private final IProject project;
    private final IJavaElement selectedJavaElement;
    private final LambdaFunctionProjectMetadata md;

    private Combo lambdaHandlerCombo;
    private Button jsonInputFileButton;
    private Combo jsonInputFileCombo;
    private String suggestedInputBoxContent;
    private Button jsonInputButton;
    private Text inputBox;
    private Button showLiveLogButton;

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public InvokeFunctionInputDialog(Shell parentShell, IJavaElement selectedJavaElement, LambdaFunctionProjectMetadata md) {
        super(parentShell);
        this.project = selectedJavaElement.getJavaProject().getProject();
        this.selectedJavaElement = selectedJavaElement;
        this.md = md;
    }

    public boolean isInputBoxContentModified() {
        return !inputBox.equals(suggestedInputBoxContent);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);

      initUI(container);
      initDefaultValue();

      return container;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private void initUI(Composite container) {
        container.setLayout(new GridLayout(2, true));

        newFillingLabel(container, "Select one of the Lambda Handlers to invoke:", 1);
        lambdaHandlerCombo = newCombo(container, 1);
        lambdaHandlerCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onLambdaHandlerComboSelected();
            }
        });

        jsonInputFileButton = newRadioButton(container, "Select one of the JSON files as input: ", 1, false, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRadioButtonSelected();
            }
        });
        jsonInputFileCombo = newCombo(container, 1);
        jsonInputFileCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onJsonFileSelectionChange();
            }
        });

        jsonInputButton = newRadioButton(container, "Enter the JSON input for your function", 2, false, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onRadioButtonSelected();
            }
        });

        inputBox = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = 2;
        inputBox.setLayoutData(gridData);

        inputBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                md.setLastInvokeInput(inputBox.getText());
            }
        });

        showLiveLogButton = new Button(container, SWT.CHECK);
        showLiveLogButton.setText("Show live log");
        showLiveLogButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                md.setLastInvokeShowLiveLog(showLiveLogButton.getSelection());
            }
        });
    }

    private void initDefaultValue() {
        loadLambdaHandlerAsync();
        loadJsonFilesAsync();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, INVOKE_BUTTON_ID, "Invoke", true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
      return new Point(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    }

    private void loadLambdaHandlerAsync() {
         Display.getDefault().syncExec(() -> {
             lambdaHandlerCombo.setItems(new String[] {LOADING});
             lambdaHandlerCombo.select(0);
             lambdaHandlerCombo.setEnabled(false);
         });

         Display.getDefault().asyncExec(() -> {
             Set<String> handlerClasses = UploadFunctionUtil.findValidHandlerClass(project);

             if (handlerClasses == null || handlerClasses.isEmpty()) {
                 lambdaHandlerCombo.setItems(new String[] {NONE_FOUND});
                 lambdaHandlerCombo.select(0);
                 disableAllTheWidegets();
             } else {
                 lambdaHandlerCombo.removeAll();
                 for (String handlerClass : handlerClasses) {
                     lambdaHandlerCombo.add(handlerClass);
                 }

                 // The current selected element has the highest priority for this invoke.
                 String defaultHandler = LambdaJavaProjectUtil.figureOutDefaultLambdaHandler(selectedJavaElement, handlerClasses);

                 // If the current selected element is not a valid handler, then we choose the last deployed handler.
                 if (defaultHandler == null || !handlerClasses.contains(defaultHandler)) {
                     defaultHandler = md.getLastDeploymentHandler();
                 }

                 // If the last deployed handler is not valid, we choose the last invoked handler.
                 if (defaultHandler == null) {
                     defaultHandler = md.getLastInvokeHandler();
                 }

                 // If the last deployed handler is not valid, we choose a random handler from the valid handler set.
                 if (defaultHandler == null || !handlerClasses.contains(defaultHandler)) {
                     defaultHandler = handlerClasses.iterator().next();
                 }
                 lambdaHandlerCombo.select(lambdaHandlerCombo.indexOf(defaultHandler));
                 onLambdaHandlerComboSelected();
                 lambdaHandlerCombo.setEnabled(true);
             }
         });
    }

    private void disableAllTheWidegets() {
        lambdaHandlerCombo.setEnabled(false);
        jsonInputFileButton.setEnabled(false);
        jsonInputButton.setEnabled(false);
        jsonInputFileCombo.setEnabled(false);
        inputBox.setEnabled(false);
        showLiveLogButton.setEnabled(false);
    }

    private void loadJsonFilesAsync() {
        Display.getDefault().syncExec(() -> {
            jsonInputFileCombo.setItems(new String[] {LOADING});
            jsonInputFileCombo.select(0);
            jsonInputFileCombo.setEnabled(false);
        });

        Display.getDefault().asyncExec(() -> {
            List<IFile> jsonFiles = null;

            try {
                jsonFiles = findJsonFiles(project);
            } catch (CoreException e) {
                LambdaPlugin.getDefault().logWarning(
                        "Failed to search for .json files in the project", e);
            }

            if (jsonFiles == null || jsonFiles.isEmpty()) {
                jsonInputFileCombo.setItems(new String[] {NONE_FOUND});
                jsonInputFileCombo.select(0);
                jsonInputFileCombo.setEnabled(false);
                jsonInputButton.setSelection(true);
                jsonInputFileButton.setSelection(false);
                onRadioButtonSelected();
            } else {
                jsonInputFileCombo.removeAll();
                for (IFile jsonFile : jsonFiles) {
                    jsonInputFileCombo.add(jsonFile.getFullPath().toOSString());
                    jsonInputFileCombo.setData(jsonFile.getFullPath().toOSString(), jsonFile);
                }
                int index = jsonInputFileCombo.indexOf(md.getLastInvokeJsonFile());
                if (index < 0) {
                    index = 0;
                }
                jsonInputFileCombo.select(index);
                onJsonFileSelectionChange();
            }
        });
    }

    private List<IFile> findJsonFiles(IProject project) throws CoreException {
        final List<IFile> jsonFiles = new LinkedList<>();

        project.accept((res) -> {
            if (res instanceof IFile) {
                IFile file = (IFile) res;
                IPath fullPath = file.getFullPath();
                if (file.getName().endsWith(".json")) {
                    // Skip hidden folder or files
                    int i = 0;
                    while (i < fullPath.segmentCount()) {
                        if (fullPath.segment(i).startsWith(".")) {
                            break;
                        }
                        ++i;
                    }
                    if (i == fullPath.segmentCount()) {
                        jsonFiles.add(file);
                    }
                }
            }
            return true;
        });
        return jsonFiles;
    }

    private void onLambdaHandlerComboSelected() {
        String handlerClass = lambdaHandlerCombo.getItem(lambdaHandlerCombo.getSelectionIndex());
        md.setLastInvokeHandler(handlerClass);
        jsonInputFileButton.setSelection(md.getLastInvokeSelectJsonFile());
        jsonInputButton.setSelection(!md.getLastInvokeSelectJsonFile());
        onRadioButtonSelected();
        showLiveLogButton.setSelection(md.getLastInvokeShowLiveLog());
    }

    private void onRadioButtonSelected() {
        jsonInputFileCombo.setEnabled(jsonInputFileButton.getSelection());
        inputBox.setEditable(jsonInputButton.getSelection());
        md.setLastInvokeSelectJsonFile(jsonInputFileButton.getSelection());
        md.setLastInvokeSelectJsonInput(jsonInputButton.getSelection());
        if (jsonInputFileButton.getSelection()) {
            onJsonFileSelectionChange();
        } else if (jsonInputButton.getSelection()) {
            inputBox.setText(md.getLastInvokeInput());
        }
    }

    private void onJsonFileSelectionChange() {
        if (jsonInputFileButton.getSelection() == false) {
            return;
        }
        IFile file = (IFile) jsonInputFileCombo.getData(jsonInputFileCombo.getText());
        if (file == null) {
            return;
        }
        try {
            String fileContent = IOUtils.toString(file.getContents());
            inputBox.setText(fileContent);
            suggestedInputBoxContent = fileContent;
            md.setLastInvokeJsonFile(jsonInputFileCombo.getText());
            md.setLastInvokeInput(fileContent);
        } catch (Exception ignored) {
            return;
        }
    }
}
