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
package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.amazonaws.eclipse.codedeploy.explorer.editor.DeploymentGroupEditorInput;

/**
 * S3 object listing with virtual directory support.
 */
public class DeploymentsTableView extends Composite {

    static final int DEPLOYMENT_ID_COL = 0;
    static final int INSTANCE_ID_COL = 1;
    static final int LIFECYCLE_EVENT_COL = 2;
    static final int STATUS_COL = 3;
    static final int START_TIME_COL = 4;
    static final int END_TIME_COL = 5;
    static final int REVISION_LOCATION_COL = 6;
    static final int LOGS_COL = 7;

    private final TreeViewer viewer;
    private final DeploymentsTableViewTreePathContentCache contentCache;
    private final LoadingContentProvider loadingContentProvider;

    private Label tableTitleLabel;

    public DeploymentsTableView(DeploymentGroupEditorInput editorInput,
            Composite composite, FormToolkit toolkit, int style) {
        super(composite, style);

        contentCache = new DeploymentsTableViewTreePathContentCache(editorInput);
        loadingContentProvider = new LoadingContentProvider();

        viewer = createControls(toolkit);
    }

    private TreeViewer createControls(FormToolkit toolkit) {
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        setLayout(gridLayout);

        Composite sectionComp = toolkit.createComposite(this, SWT.None);
        sectionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sectionComp.setLayout(new GridLayout(1, false));

        Composite headingComp = toolkit.createComposite(sectionComp, SWT.None);
        headingComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        headingComp.setLayout(new GridLayout());
        tableTitleLabel = toolkit.createLabel(headingComp, "Deployments");
        tableTitleLabel.setFont(JFaceResources.getHeaderFont());
        tableTitleLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

        Composite tableHolder = toolkit.createComposite(sectionComp, SWT.None);
        tableHolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 10;
        layout.type = SWT.VERTICAL;
        tableHolder.setLayout(layout);

        Composite tableComp = toolkit.createComposite(tableHolder, SWT.None);
        final TreeColumnLayout tableColumnLayout = new TreeColumnLayout();
        tableComp.setLayout(tableColumnLayout);

        final TreeViewer viewer = new TreeViewer(tableComp, SWT.BORDER | SWT.VIRTUAL | SWT.MULTI | SWT.H_SCROLL);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        viewer.setUseHashlookup(true);
        viewer.setLabelProvider(new DeploymentsTableViewLabelProvider());
        viewer.setContentProvider(new DeploymentsTableViewContentProvider(this.viewer, this.contentCache));

        Tree tree = viewer.getTree();

        createColumns(tableColumnLayout, tree);

        viewer.setInput(loadingContentProvider);
        updateRefreshProgress(0, false);

        // Async load top-level data
        new Thread(new Runnable() {

            @Override
            public void run() {
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        // Preserve the current column widths
                        int[] colWidth = new int[viewer.getTree().getColumns().length];
                        int i = 0;
                        for ( TreeColumn col : viewer.getTree().getColumns() ) {
                            colWidth[i++] = col.getWidth();
                        }

                        i = 0;
                        for ( TreeColumn col : viewer.getTree().getColumns() ) {
                            tableColumnLayout.setColumnData(col, new ColumnPixelData(colWidth[i]));
                        }
                    }
                });

                // Cache the children of all the top-level elements before
                // updating the tree view
                loadAllTopLevelElements();

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(contentCache);
                    }
                });
            }
        }).start();

        return viewer;
    }

    private void createColumns(TreeColumnLayout tableColumnLayout, Tree tree) {

        TreeColumn column = new TreeColumn(tree, SWT.NONE);
        column.setText("Deployment ID");
        ColumnWeightData cwd_column = new ColumnWeightData(10);
        cwd_column.minimumWidth = 1;
        tableColumnLayout.setColumnData(column, cwd_column);

        TreeColumn column_1 = new TreeColumn(tree, SWT.NONE);
        column_1.setText("EC2 Instance");
        tableColumnLayout.setColumnData(column_1, new ColumnWeightData(10));

        TreeColumn column_2 = new TreeColumn(tree, SWT.NONE);
        column_2.setText("Lifecycle Event");
        tableColumnLayout.setColumnData(column_2, new ColumnWeightData(8));

        TreeColumn column_3 = new TreeColumn(tree, SWT.NONE);
        column_3.setText("Status");
        tableColumnLayout.setColumnData(column_3, new ColumnWeightData(6));

        TreeColumn column_4 = new TreeColumn(tree, SWT.NONE);
        column_4.setText("Start Time");
        tableColumnLayout.setColumnData(column_4, new ColumnWeightData(12));

        TreeColumn column_5 = new TreeColumn(tree, SWT.NONE);
        column_5.setText("End Time");
        tableColumnLayout.setColumnData(column_5, new ColumnWeightData(12));

        TreeColumn column_6 = new TreeColumn(tree, SWT.NONE);
        column_6.setText("Revision");
        tableColumnLayout.setColumnData(column_6, new ColumnWeightData(20));

        TreeColumn column_7 = new TreeColumn(tree, SWT.NONE);
        column_7.setText("Logs");
        tableColumnLayout.setColumnData(column_7, new ColumnWeightData(15));
    }

    private volatile boolean isRefreshing = false;

    public void refreshAsync() {
        if (isRefreshing) {
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {

                isRefreshing = true;
                updateRefreshProgress(0, true);

                // Cache the children of all the top-level elements before
                // updating the tree view
                contentCache.refresh();
                loadAllTopLevelElements();

                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(contentCache);
                    }
                });

                isRefreshing = false;

            }

        }).start();
    }

    private void loadAllTopLevelElements() {

        Object[] topLevelElements = contentCache
                .getChildren(new TreePath(new Object[0]));

        int progressPerElement = 100 / (topLevelElements.length + 1);
        int loadedElements = 1;
        updateRefreshProgress(loadedElements++ * progressPerElement, true);

        for (Object topLevelElement : topLevelElements) {
            contentCache.getChildren(new TreePath(
                    new Object[] { topLevelElement }));
            updateRefreshProgress(loadedElements++ * progressPerElement, true);
        }

        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                tableTitleLabel.setText("Deployments");
                tableTitleLabel.pack();
            }
        });
    }

    private void updateRefreshProgress(final int progress, boolean inDisplayThread) {
        if (inDisplayThread) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    updateRefreshProgress(progress, false);
                }
            });

        } else {
            tableTitleLabel.setText(String.format(
                    "Deployments (loading... %d%%)", Math.min(100, progress)));
            tableTitleLabel.pack();
        }

    }
}
