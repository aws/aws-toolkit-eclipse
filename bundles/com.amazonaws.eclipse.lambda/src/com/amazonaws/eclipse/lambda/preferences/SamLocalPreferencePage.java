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
package com.amazonaws.eclipse.lambda.preferences;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.amazonaws.eclipse.core.model.ImportFileDataModel;
import com.amazonaws.eclipse.core.ui.ImportFileComposite;
import com.amazonaws.eclipse.core.ui.preferences.AwsToolkitPreferencePage;
import com.amazonaws.eclipse.core.ui.wizards.WizardWidgetFactory;
import com.amazonaws.eclipse.core.validator.FilePathValidator;
import com.amazonaws.eclipse.lambda.LambdaConstants;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.launching.SamLocalConstants;

public class SamLocalPreferencePage extends AwsToolkitPreferencePage implements IWorkbenchPreferencePage {
    private static final String PAGE_NAME = SamLocalPreferencePage.class.getName();
    public static final String ID = "com.amazonaws.eclipse.lambda.preferences.SamLocalPreferencePage";

    private SamLocalPreferencesDataModel dataModel = new SamLocalPreferencesDataModel();
    private DataBindingContext dataBindingContext = new DataBindingContext();
    private AggregateValidationStatus aggregateValidationStatus =
            new AggregateValidationStatus(dataBindingContext, AggregateValidationStatus.MAX_SEVERITY);

    private ImportFileComposite samLocalExecutableComposite;

    public SamLocalPreferencePage() {
        super(PAGE_NAME);
        setDescription("AWS SAM Local Configuration");
        this.dispose();
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(LambdaPlugin.getDefault().getPreferenceStore());
        aggregateValidationStatus.addChangeListener(e -> {
            Object value = aggregateValidationStatus.getValue();
            if (value instanceof IStatus == false) return;

            IStatus status = (IStatus)value;
            boolean success = (status.getSeverity() == IStatus.OK);
            if (success) {
                setErrorMessage(null);
            } else {
                setErrorMessage(status.getMessage());
            }
            setValid(success);
        });
        initDataModel();
    }

    private void initDataModel() {
        dataModel.getSamLocalExecutable().setFilePath(
                getPreferenceStore().getString(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE));
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = WizardWidgetFactory.newComposite(parent, 1, 1);

        WizardWidgetFactory.newLink(
                composite,
                LambdaConstants.webLinkListener,
                "To install AWS SAM Local and the required dependencies, see <a href=\""
                        + SamLocalConstants.LINKS_INSTALL_SAM_LOCAL
                        + "\">Installation</a>.", 1, 100, 30);

        samLocalExecutableComposite = ImportFileComposite.builder(
                    composite, dataBindingContext, dataModel.getSamLocalExecutable())
                .buttonLabel("Browse...")
                .filePathValidator(new FilePathValidator("SAM Local Executbale"))
                .textLabel("SAM Local Executable:")
                .textMessage("Absolute path for sam command...")
                .build();

        return composite;
    }

    @Override
    protected void performDefaults() {
        if (samLocalExecutableComposite != null) {
            samLocalExecutableComposite.setFilePath(
                    getPreferenceStore().getDefaultString(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE));
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        aggregateValidationStatus.dispose();
        dataBindingContext.dispose();
        onApplyButton();
        return super.performOk();
    }

    @Override
    protected void performApply() {
        onApplyButton();
        super.performApply();
    }

    private void onApplyButton() {
        String filePath = dataModel.getSamLocalExecutable().getFilePath();
        getPreferenceStore().setValue(SamLocalConstants.P_SAM_LOCAL_EXECUTABLE, filePath);
    }

    private static class SamLocalPreferencesDataModel {
        private final ImportFileDataModel samLocalExecutable = new ImportFileDataModel();

        public ImportFileDataModel getSamLocalExecutable() {
            return samLocalExecutable;
        }
    }
}
