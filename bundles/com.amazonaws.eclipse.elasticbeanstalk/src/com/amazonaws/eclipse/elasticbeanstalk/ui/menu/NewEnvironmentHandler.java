/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.ui.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.ui.internal.wizard.NewServerWizard;

public class NewEnvironmentHandler extends AbstractHandler {
	@Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        NewServerWizard wizard = new NewServerWizard();
        wizard.init(PlatformUI.getWorkbench(), new StructuredSelection());
        WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
        return dialog.open();
    }
}
