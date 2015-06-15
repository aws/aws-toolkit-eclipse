package com.amazonaws.eclipse.lambda.invoke.ui;

import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newFillingLabel;
import static com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory.newCombo;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.ServiceApiUtils;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.Role;

public class InvokeFunctionInputDialog extends Dialog {

    public static final int INVOKE_BUTTON_ID = IDialogConstants.OK_ID;

    private static final int PREFERRED_WIDTH = 600;
    private static final int PREFERRED_HEIGHT = 400;

    private final IProject project;

    private Combo jsonInputFileCombo;
    private String inputBoxContent;
    private Text inputBox;

    private static final String LOADING = "Loading...";
    private static final String NONE_FOUND = "None found";

    public InvokeFunctionInputDialog(Shell parentShell, IProject project) {
        super(parentShell);
        this.project = project;
    }

    public String getInputBoxContent() {
        return inputBoxContent;
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
              inputBoxContent = inputBox.getText();
          }
      });

      loadJsonFilesAsync();

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
      newShell.setText("Lambda Function Input");
    }

    @Override
    protected Point getInitialSize() {
      return new Point(PREFERRED_WIDTH, PREFERRED_HEIGHT);
    }

    private void loadJsonFilesAsync() {
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
                    LambdaPlugin.getDefault().warn(
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
                    onJsonFileSelectionChange();

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
            inputBoxContent = fileContent;
        } catch (Exception ignored) {
            return;
        }
    }

}
