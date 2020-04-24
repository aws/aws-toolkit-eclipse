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
package com.amazonaws.eclipse.dynamodb.editor;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.explorer.AwsAction;


/**
 * Opens up the custom query editor on a given domain
 */
public class OpenTableEditorAction extends AwsAction {

    private final String tableName;

    public OpenTableEditorAction(final String domainName) {
        super(AwsToolkitMetricType.EXPLORER_DYNAMODB_OPEN_TABLE_EDITOR);
        this.tableName = domainName;
        setText("Open Query Editor");
        setToolTipText("Opens the query editor to run queries against this domain");
    }

    @Override
    protected void doRun() {
        try {
            IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            workbenchPage.openEditor(new TableEditorInput(this.tableName, AwsToolkitCore.getDefault()
                    .getCurrentAccountId()), DynamoDBTableEditor.ID);
            actionSucceeded();
        } catch ( PartInitException e ) {
            actionFailed();
            AwsToolkitCore.getDefault().logError(e.getMessage(), e);
        } finally {
            actionFinished();
        }
    }
}