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
package com.amazonaws.eclipse.dynamodb.testtool;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.dynamodb.DynamoDBPlugin;
import com.amazonaws.eclipse.dynamodb.testtool.TestToolVersion.InstallState;
import com.amazonaws.eclipse.explorer.AwsAction;

/**
 * A table that displays the set of available DynamoDB Local Test Tool
 * versions and allows you to install or uninstall them.
 */
public class TestToolVersionTable extends Composite {

    /**
     * Background job that checks for changes to the list of test tool
     * versions and updates the table.
     */
    private final Job updateJob = new Job("DynamoDB Local Test Tool Update") {
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            setInput(TestToolManager.INSTANCE.getAllVersions());
            return Status.OK_STATUS;
        }
    };

    /**
     * Background job that installs a particular version of the test tool.
     */
    private class InstallJob extends Job {

        private final TestToolVersion version;

        /**
         * @param version The version of the test tool to install.
         */
        public InstallJob(final TestToolVersion version) {
            super("DynamoDB Local Test Tool Install");
            this.version = version;
        }

        /** {@inheritDoc} */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            // Mark the version as currently being installed so we disable
            // the 'install' button.
            TestToolManager.INSTANCE.markInstalling(version);
            checkForUpdates();

            // Actually do the install, updating the progress bar as we go.
            try {
                TestToolManager.INSTANCE
                    .installVersion(version, progressMonitor);
            } finally {
                checkForUpdates();
            }

            return Status.OK_STATUS;
        }
    }

    /**
     * A background job that uninstalls a particular version of the test tool.
     */
    private class UninstallJob extends Job {
        private final TestToolVersion version;

        /**
         * @param version The version of the test tool to uninstall.
         */
        public UninstallJob(final TestToolVersion version) {
            super("DynamoDB Local Test Tool Uninstall");
            this.version = version;
        }

        /** {@inheritDoc} */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            TestToolManager.INSTANCE.uninstallVersion(version);
            checkForUpdates();
            return Status.OK_STATUS;
        }
    }

    private final Collection<Font> fonts = new LinkedList<>();

    private TreeViewer viewer;
    private ToolBarManager toolbar;
    private CustomProgressMonitor progressMonitor;

    private Group detailsGroup;
    private Label versionText;
    private Label descriptionText;

    private Action installAction;
    private Action uninstallAction;

    /**
     * @param parent The parent composite to attach to.
     */
    public TestToolVersionTable(final Composite parent) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(1, false);
        setLayout(layout);

        createToolbar();
        createViewer();
        createDetailsGroup();

        createActions();
        hookToolbar();
        hookContextMenu();
        checkForUpdates();
    }

    /**
     * @return The currently selected test tool version (or null).
     */
    public TestToolVersion getSelection() {
        StructuredSelection selection =
            (StructuredSelection) viewer.getSelection();

        return (TestToolVersion) selection.getFirstElement();
    }

    /**
     * @return  an observable that tracks the current selection
     */
    public IObservableValue getObservableSelection() {
        return ViewersObservables.observeSingleSelection(viewer);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        for (Font font : fonts) {
            font.dispose();
        }
        super.dispose();
    }

    /**
     * Create the toolbar, with install and uninstall buttons and an initially
     * hidden progress bar to track install status.
     */
    private void createToolbar() {

        // Bind to the top of wherever we end up.
        Composite wrapper = new Composite(this, SWT.NONE);
        wrapper.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        wrapper.setLayout(new GridLayout(2, false));

        toolbar = new CustomToolBarManager();

        // Toolbar on the left.
        Control control = toolbar.createControl(wrapper);
        control.setLayoutData(
            new GridData(SWT.LEFT, SWT.CENTER, false, false)
        );

        // Progress bar on the right.
        ProgressBar progressBar = new ProgressBar(wrapper, SWT.NONE);
        progressBar.setLayoutData(
            new GridData(SWT.RIGHT, SWT.CENTER, true, false)
        );
        progressBar.setVisible(false);

        progressMonitor = new CustomProgressMonitor(progressBar);
    }

    /**
     * Create the actual table view displaying the list of selectable test
     * tool versions.
     */
    private void createViewer() {

        // Grab any leftover space in the middle.
        Composite wrapper = new Composite(this, SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
        data.heightHint = 200;
        wrapper.setLayoutData(data);

        TreeColumnLayout layout = new TreeColumnLayout();
        wrapper.setLayout(layout);

        viewer = new TreeViewer(
            wrapper,
            SWT.H_SCROLL
             | SWT.V_SCROLL
             | SWT.FULL_SELECTION
             | SWT.BORDER
             | SWT.SINGLE
        );

        TestToolVersionProvider provider = new TestToolVersionProvider();
        viewer.setContentProvider(provider);
        viewer.setLabelProvider(provider);
        viewer.setComparator(new VersionComparator());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onSelectionChanged();
            }
        });

        Tree tree = viewer.getTree();
        tree.setHeaderVisible(true);

        TreeColumn versionColumn = new TreeColumn(tree, SWT.LEFT);
        versionColumn.setText("Version");
        layout.setColumnData(versionColumn, new ColumnWeightData(75));

        TreeColumn installColumn = new TreeColumn(tree, SWT.CENTER);
        installColumn.setText("Installed?");
        layout.setColumnData(installColumn, new ColumnWeightData(25));
    }

    /**
     * Create the (initially hidden) details group, which shows a longer text
     * description of the currently-selected version.
     */
    private void createDetailsGroup() {

        detailsGroup = new Group(this, SWT.NONE);
        detailsGroup.setText("Details");
        detailsGroup.setLayout(new GridLayout(2, false));
        detailsGroup.setVisible(false);

        // Pin to the bottom to allow the table control to have as much space
        // as it wants.
        GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.widthHint = 300;
        detailsGroup.setLayoutData(data);

        Label version = new Label(detailsGroup, SWT.TOP);
        version.setText("Version: ");
        makeBold(version);

        versionText = new Label(detailsGroup, SWT.WRAP);

        Label description = new Label(detailsGroup, SWT.TOP);
        description.setText("Description: ");
        makeBold(description);

        descriptionText = new Label(detailsGroup, SWT.WRAP);

        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.widthHint = 300;
        descriptionText.setLayoutData(data);
    }

    /**
     * Make the given label bold.
     *
     * @param label The label to embolden.
     */
    private void makeBold(final Label label) {
        FontData[] fontData = label.getFont().getFontData();
        fontData[0].setStyle(SWT.BOLD);

        Font font = new Font(getDisplay(), fontData[0]);
        fonts.add(font);

        label.setFont(font);
    }

    /**
     * Create the install and uninstall actions for the toolbar and context
     * menu.
     */
    private void createActions() {
        installAction = new AwsAction(AwsToolkitMetricType.DYNAMODB_INSTALL_TEST_TOOL) {
            @Override
            protected void doRun() {
                new InstallJob(getSelection()).schedule();
                actionFinished();
            }
        };
        installAction.setText("Install");
        installAction.setToolTipText("Install the selected version.");
        installAction.setImageDescriptor(
            DynamoDBPlugin.getDefault()
                .getImageRegistry()
                .getDescriptor("add")
        );
        installAction.setEnabled(false);

        uninstallAction = new AwsAction(AwsToolkitMetricType.DYNAMODB_UNINSTALL_TEST_TOOL) {
            @Override
            protected void doRun() {
                new UninstallJob(getSelection()).schedule();
                actionFinished();
            }
        };
        uninstallAction.setText("Uninstall");
        uninstallAction.setToolTipText("Uninstall the selected version.");
        uninstallAction.setImageDescriptor(
            DynamoDBPlugin.getDefault()
                .getImageRegistry()
                .getDescriptor("remove")
        );
        uninstallAction.setEnabled(false);
    }

    /**
     * Hook the actions created above into the toolbar.
     */
    private void hookToolbar() {
        toolbar.add(installAction);
        toolbar.add(new Separator());
        toolbar.add(uninstallAction);
        toolbar.update(true);
    }

    /**
     * Hook the actions created above into the context menu for the table.
     */
    private void hookContextMenu() {
        MenuManager manager = new MenuManager("#PopupMenu");
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.add(installAction);
                manager.add(uninstallAction);
            }
        });

        Menu menu = manager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);

        manager.createContextMenu(viewer.getControl());
    }

    /**
     * Check for changes to the list of test tool versions.
     */
    private void checkForUpdates() {
        updateJob.schedule();
    }

    /**
     * Handler callback for when the selected version changes. Enable or
     * disable the install/uninstall actions as appropriate, and update the
     * details group.
     */
    private void onSelectionChanged() {
        TestToolVersion version = getSelection();
        if (version == null) {
            installAction.setEnabled(false);
            uninstallAction.setEnabled(false);
            detailsGroup.setVisible(false);
        } else {
            installAction.setEnabled(
                version.getInstallState() == InstallState.NOT_INSTALLED
            );
            uninstallAction.setEnabled(
                version.getInstallState() == InstallState.INSTALLED
            );

            versionText.setText(version.getName());
            descriptionText.setText(version.getDescription());
            detailsGroup.setVisible(true);
            detailsGroup.pack();
        }
        toolbar.update(false);
    }

    /**
     * Set the list of test tool versions to display in the table.
     *
     * @param versions The list of versions.
     */
    private void setInput(final List<TestToolVersion> versions) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                _setInput(versions);
            }
        });
    }

    /**
     * Helper for setInput() that MUST be run on the UI thread.
     */
    private void _setInput(final List<TestToolVersion> versions) {
        TestToolVersion previous = getSelection();

        viewer.setInput(versions);

        if (previous != null) {
            Tree tree = viewer.getTree();

            for (int i = 0; i < tree.getItemCount(); ++i) {
                TreeItem item = tree.getItem(i);
                TestToolVersion version = (TestToolVersion) item.getData();

                if (previous.getName().equals(version.getName())) {
                    tree.select(item);
                    tree.notifyListeners(SWT.Selection, null);
                    break;
                }
            }
        }
    }

    /**
     * A content and label provider for the test tool table.
     */
    private class TestToolVersionProvider extends LabelProvider
            implements ITreeContentProvider, ITableLabelProvider {

        private List<TestToolVersion> versions;

        /** {@inheritDoc} */
        @Override
        public Object[] getElements(final Object inputElement) {
            if (versions == null) {
                return null;
            }
            return versions.toArray();
        }

        /** {@inheritDoc} */
        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(final Viewer viewer,
                                 final Object oldInput,
                                 final Object newInput) {

            versions = (List<TestToolVersion>) newInput;
        }

        /** {@inheritDoc} */
        @Override
        public Image getColumnImage(final Object element,
                                    final int columnIndex) {

            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getColumnText(final Object element,
                                    final int columnIndex) {

            if (!(element instanceof TestToolVersion)) {
                return "???";
            }

            TestToolVersion version = (TestToolVersion) element;

            switch (columnIndex) {
            case 0:
                return version.getName();

            case 1:
                if (version.isRunning()) {
                    return "Running...";
                }
                if (version.isInstalled()) {
                    return "Yes";
                }
                if (version.isInstalling()) {
                    return "Installing...";
                }
                return "";

            default:
                return "???";
            }
        }

        /** {@inheritDoc} */
        @Override
        public Object[] getChildren(final Object parentElement) {
            return new Object[0];
        }

        /** {@inheritDoc} */
        @Override
        public Object getParent(final Object element) {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasChildren(final Object element) {
            return false;
        }
    }

    /**
     * Sort versions in reverse alphabetical order (which, since version
     * numbers are dates, is also conveniently reverse chronological order).
     */
    private class VersionComparator extends ViewerComparator {
        /** {@inheritDoc} */
        @Override
        public int compare(final Viewer viewer,
                           final Object e1,
                           final Object e2) {

            if (!(e1 instanceof TestToolVersion)) {
                return 0;
            }
            if (!(e2 instanceof TestToolVersion)) {
                return 0;
            }

            TestToolVersion v1 = (TestToolVersion) e1;
            TestToolVersion v2 = (TestToolVersion) e2;

            return v2.getName().compareTo(v1.getName());
        }
    }

    /**
     * A Progress Monitor that updates a ProgressBar as the test tool is
     * installed.
     */
    private static class CustomProgressMonitor implements IProgressMonitor {

        private final ProgressBar progressBar;

        /**
         * @param progressBar The progress bar to update.
         */
        public CustomProgressMonitor(final ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        /** {@inheritDoc} */
        @Override
        public void beginTask(final String name, final int totalWork) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    progressBar.setMinimum(0);
                    progressBar.setMaximum(totalWork);
                    progressBar.setSelection(0);
                    progressBar.setToolTipText(name);
                    progressBar.setVisible(true);
                }
            });
        }

        /** {@inheritDoc} */
        @Override
        public void worked(final int work) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    progressBar.setSelection(
                        progressBar.getSelection() + work
                    );
                }
            });
        }

        /** {@inheritDoc} */
        @Override
        public void done() {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisible(false);
                }
            });
        }

        /** {@inheritDoc} */
        @Override
        public void internalWorked(double arg0) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public boolean isCanceled() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public void setCanceled(boolean arg0) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void setTaskName(String arg0) {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void subTask(String arg0) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A toolbar manager that forces text mode on all actions added to it.
     */
    private static class CustomToolBarManager extends ToolBarManager {
        public CustomToolBarManager() {
            super(SWT.FLAT | SWT.RIGHT);
        }

        /** {@inheritDoc} */
        @Override
        public void add(final IAction action) {
            ActionContributionItem item = new ActionContributionItem(action);
            item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
            super.add(item);
        }
    }

}
