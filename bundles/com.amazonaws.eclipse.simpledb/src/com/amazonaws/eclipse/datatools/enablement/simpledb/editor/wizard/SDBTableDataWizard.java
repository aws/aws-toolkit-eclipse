/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.datatools.enablement.simpledb.editor.wizard;

import org.eclipse.datatools.sqltools.data.internal.ui.editor.TableDataEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.amazonaws.eclipse.datatools.enablement.simpledb.Activator;
import com.amazonaws.eclipse.datatools.enablement.simpledb.editor.Messages;

public class SDBTableDataWizard extends Wizard {

    private final TableDataEditor editor;
    private SDBTableDataWizardPage page;

    public SDBTableDataWizard(final TableDataEditor editor) {
        this.editor = editor;
        setNeedsProgressMonitor(false);
        setWindowTitle(Messages.windowTitle);
    }

    @Override
    public void addPages() {
        this.page = new SDBTableDataWizardPage(this.editor);
        ImageDescriptor image = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID,
        "icons/sdb-wizard-75x66-shadow.png"); //$NON-NLS-1$
        this.page.setImageDescriptor(image);
        addPage(this.page);
    }

    @Override
    public boolean performFinish() {
        this.page.saveData();
        return true;
    }

    @Override
    public boolean canFinish() {
        return this.page.isPageComplete();
    }

}
