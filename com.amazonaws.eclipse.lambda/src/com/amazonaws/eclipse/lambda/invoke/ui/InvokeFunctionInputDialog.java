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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
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

import com.amazonaws.eclipse.lambda.LambdaAnalytics;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.project.metadata.LambdaFunctionProjectMetadata;

public class InvokeFunctionInputDialog extends Dialog {

    private static final boolean DEFAULT_SHOW_LIVE_LOG = false;
    public static final int INVOKE_BUTTON_ID = IDialogConstants.OK_ID;

    private static final int PREFERRED_WIDTH = 600;
    private static final int PREFERRED_HEIGHT = 400;

    private final IProject project;
    private final LambdaFunctionProjectMetadata md;

    private Combo jsonInputFileCombo;
    private String suggestedInputBoxContent;
    private Text inputBox;
    private Button showLiveLogButton;

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public InvokeFunctionInputDialog(Shell parentShell, IProject project, LambdaFunctionProjectMetadata md) {
        super(parentShell);
        this.project = project;
        this.md = md;
        if (md.getLastInvokeShowLiveLog() == null) {
            md.setLastInvokeShowLiveLog(DEFAULT_SHOW_LIVE_LOG);
        }
    }

    public boolean isInputBoxContentModified() {
        return !inputBox.equals(suggestedInputBoxContent);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);
      container.setLayout(new GridLayout(2, false));

      newFillingLabel(container, "Select one of the JSON files as input: ", 1);
      jsonInputFileCombo = newCombo(container, 1);
      jsonInputFileCombo.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
              LambdaAnalytics.trackInputJsonFileSelectionChange();
              onJsonFileSelectionChange();
          }
      });

      newFillingLabel(container, "Or enter the JSON input for your function", 2);

      inputBox = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
      GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
      gridData.horizontalSpan = 2;
      inputBox.setLayoutData(gridData);

      inputBox.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent e) {
              md.setLastInvokeInput(inputBox.getText());
          }
      });
      if (md.getLastInvokeInput() != null) {
          inputBox.setText(md.getLastInvokeInput());
      }

      showLiveLogButton = new Button(container, SWT.CHECK);
      showLiveLogButton.setText("Show live log");
      showLiveLogButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent event) {
              md.setLastInvokeShowLiveLog(showLiveLogButton.getSelection());
          }
      });
      showLiveLogButton.setSelection(md.getLastInvokeShowLiveLog());

      loadJsonFilesAsync(md.getLastInvokeInput() == null);

      return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, INVOKE_BUTTON_ID, "Invoke", true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(md.getLastDeploymentFunctionName() + " Lambda Function Input");
    }

    @Override
    protected Point getInitialSize() {
      return new Point(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    }

    private void loadJsonFilesAsync(final boolean showJsonFile) {
        Display.getDefault().syncExec(new Runnable() {

            public void run() {
                jsonInputFileCombo.setItems(new String[] {LOADING});
                jsonInputFileCombo.select(0);
                jsonInputFileCombo.setEnabled(false);
            }
        });

        Display.getDefault().asyncExec(new Runnable() {

            public void run() {
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

                } else {
                    jsonInputFileCombo.removeAll();
                    for (IFile jsonFile : jsonFiles) {
                        jsonInputFileCombo.add(jsonFile.getFullPath().toOSString());
                        jsonInputFileCombo.setData(jsonFile.getFullPath().toOSString(), jsonFile);
                    }
                    jsonInputFileCombo.select(0);
                    jsonInputFileCombo.setEnabled(true);
                    if (showJsonFile) onJsonFileSelectionChange();
                }
            }
        });
    }

    private List<IFile> findJsonFiles(IProject project) throws CoreException {
        final List<IFile> jsonFiles = new LinkedList<IFile>();

        project.accept(new IResourceVisitor() {
            public boolean visit(IResource res) throws CoreException {
                if (res instanceof IFile) {
                    IFile file = (IFile)res;
                    if (file.getName().endsWith(".json")) {
                        jsonFiles.add(file);
                    }
                }
                return true;
            }
        });
        return jsonFiles;
    }

    private void onJsonFileSelectionChange() {
        IFile file = (IFile) jsonInputFileCombo.getData(jsonInputFileCombo.getText());
        if (file == null) {
            return;
        }
        try {
            String fileContent = IOUtils.toString(file.getContents());
            inputBox.setText(fileContent);
            suggestedInputBoxContent = fileContent;
            md.setLastInvokeInput(fileContent);
        } catch (Exception ignored) {
            return;
        }
    }

}
