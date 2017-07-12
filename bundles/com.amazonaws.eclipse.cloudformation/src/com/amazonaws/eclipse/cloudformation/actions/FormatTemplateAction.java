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
package com.amazonaws.eclipse.cloudformation.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.cloudformation.CloudFormationPlugin;
import com.amazonaws.eclipse.explorer.cloudformation.JsonFormatter;

/**
 * Formats a CloudFormation template document. The document must be valid JSON
 * in order for it to be formatted.
 */
public class FormatTemplateAction extends TemplateEditorBaseAction {

    @Override
    public void run(IAction action) {
        try {
            String documentText = templateDocument.get(0, templateDocument.getLength());
            String formattedText = JsonFormatter.format(documentText);
            templateDocument.replace(0, templateDocument.getLength(), formattedText);
        } catch (Exception e) {
            String message = "Unable to format document.  Make sure the document is well-formed JSON before attempting to format.";
            IStatus status = new Status(IStatus.ERROR, CloudFormationPlugin.PLUGIN_ID, message);
            StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
        }
    }
}
