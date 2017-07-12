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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.lambda.LambdaPlugin;
import com.amazonaws.eclipse.lambda.invoke.logs.CloudWatchLogsUtils;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.LogStream;
import com.amazonaws.services.logs.model.OutputLogEvent;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringInputStream;

public class FunctionLogsTable extends Composite {
    private TreeViewer viewer;
    private final FunctionEditorInput functionEditorInput;

    private final class FunctionLogsContentProvider implements ITreePathContentProvider {

        private LogStream[] logStreams;

        @Override
        public void dispose() {}

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof LogStream[]) {
                logStreams = (LogStream[])newInput;
            } else {
                logStreams = new LogStream[0];
            }
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return logStreams;
        }

        @Override
        public Object[] getChildren(TreePath parentPath) {
            return null;
        }

        @Override
        public boolean hasChildren(TreePath path) {
            return false;
        }

        @Override
        public TreePath[] getParents(Object element) {
            return new TreePath[0];
        }
    }

    private final class FunctionLogsLabelProvider implements ITableLabelProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {}
        @Override
        public void removeListener(ILabelProviderListener listener) {}

        @Override
        public void dispose() {}

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof LogStream == false) return "";

            LogStream logStream = (LogStream)element;
            switch (columnIndex) {
                case 0: return logStream.getLogStreamName();
                case 1: return CloudWatchLogsUtils.longTimeToHumanReadible(logStream.getCreationTime());
                case 2: return CloudWatchLogsUtils.longTimeToHumanReadible(logStream.getLastEventTimestamp());
                case 3: return logStream.getStoredBytes().toString();
            }

            return element.toString();
        }
    }

    public FunctionLogsTable(Composite parent, FormToolkit toolkit, FunctionEditorInput functionEditorInput) {
        super(parent, SWT.NONE);
        this.functionEditorInput = functionEditorInput;

        this.setLayout(new GridLayout());

        Composite composite = toolkit.createComposite(this);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        composite.setLayout(tableColumnLayout);

        FunctionLogsContentProvider contentProvider = new FunctionLogsContentProvider();
        FunctionLogsLabelProvider labelProvider = new FunctionLogsLabelProvider();

        viewer = new TreeViewer(composite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setLabelProvider(labelProvider);
        viewer.setContentProvider(contentProvider);

        createColumns(tableColumnLayout, viewer.getTree());

        refresh();

        hookContextMenu();
    }

    public void refresh() {
        new LoadFunctionLogsThread().start();
    }

    private List<LogStream> getSelectedObjects() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        List<LogStream> streams = new LinkedList<>();
        Iterator<?> iterator = selection.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof LogStream) {
                streams.add((LogStream) next);
            }
        }
        return streams;
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(new ShowLogEventsAction());
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        menuMgr.createContextMenu(this);

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent e) {
                new ShowLogEventsAction().run();
            }
        });
    }

    private void createColumns(TreeColumnLayout columnLayout, Tree tree) {
        createColumn(tree, columnLayout, "Log Streams");
        createColumn(tree, columnLayout, "Creation Time");
        createColumn(tree, columnLayout, "Last Event Time");
        createColumn(tree, columnLayout, "Stored Bytes");
    }

    private TreeColumn createColumn(Tree tree, TreeColumnLayout columnLayout, String text) {
        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText(text);
        column.setMoveable(true);
        columnLayout.setColumnData(column, new ColumnWeightData(30));
        return column;
    }

    private AWSLogs getLogsClient() {
        return AwsToolkitCore.getClientFactory(functionEditorInput.getAccountId())
                .getLogsClientByRegion(functionEditorInput.getRegionId());
    }

    private String getLogGroupName() {
        return "/aws/lambda/" + functionEditorInput.getFunctionName();
    }

    private class LoadFunctionLogsThread extends Thread {
        @Override
        public void run() {
            try {

                AWSLogs logsClient = getLogsClient();
                String logGroupName = getLogGroupName();
                final List<LogStream> logStreams = CloudWatchLogsUtils.listLogStreams(logsClient, logGroupName);

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(logStreams.toArray(new LogStream[logStreams.size()]));
                    }
                });
            } catch (Exception e) {
                LambdaPlugin.getDefault().reportException("Unable to describe log streams for function " + functionEditorInput.getFunctionName(), e);
            }
        }
    }

    private class ShowLogEventsAction extends Action {

        public ShowLogEventsAction() {
            this.setText("Show Log Events");
        }

        @Override
        public void run() {
            AWSLogs logClient = getLogsClient();
            String logGroupName = getLogGroupName();
            List<LogStream> selectedStreams = getSelectedObjects();

            try {
                List<OutputLogEvent> events = CloudWatchLogsUtils.listLogEvents(logClient, logGroupName, selectedStreams);
                String text = CloudWatchLogsUtils.convertLogEventsToString(events);

                File file = File.createTempFile(logGroupName, ".txt");
                IOUtils.copy(new StringInputStream(text), new FileOutputStream(file));
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                IDE.openEditorOnFileStore( page, fileStore );
            } catch (Exception e) {
                MessageDialog.openError(getShell(), "Failed to open function Log Events", e.getMessage());
            }
        }
    }
}
