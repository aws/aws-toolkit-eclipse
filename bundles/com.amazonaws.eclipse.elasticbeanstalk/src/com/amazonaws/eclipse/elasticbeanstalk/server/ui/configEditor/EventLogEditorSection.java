/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.amazonaws.services.elasticbeanstalk.model.EventSeverity;

/**
 * Editor part which displays the event log.
 */
public class EventLogEditorSection extends ServerEditorSection {

    /** The section widget we're managing */
    private Section section;
    private FormToolkit toolkit;
    private TreeViewer viewer;
    private boolean tableDataLoaded = false;

    private static final Object JOB_FAMILY = new Object();
    private AutoRefreshListener autoRefreshListener;

    private volatile boolean disposed;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        toolkit = getFormToolkit(parent.getDisplay());

        section = toolkit.createSection(parent,
                Section.TITLE_BAR | Section.DESCRIPTION );
        section.setText("Environment Events");
        section.setDescription("Events recorded to your Elastic Beanstalk environment");

        Composite composite = toolkit.createComposite(section);
        FillLayout layout = new FillLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        layout.type = SWT.VERTICAL;
        composite.setLayout(layout);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        section.setLayout(layout);

        createEventsTable(composite);
        configureAutoRefresh();
        refresh();
    }

    private void configureAutoRefresh() {
        autoRefreshListener = new AutoRefreshListener();
        Environment environment = (Environment) server.loadAdapter(Environment.class, null);
        environment.getServer().addServerListener(autoRefreshListener);

        // Go ahead and start auto refreshing if the server is already
        // starting up, since we won't see the starting event
        if (environment.getServer().getServerState() == IServer.STATE_STARTING) {
            EventLogRefreshManager.getInstance().startAutoRefresh(this);
        }
    }

    protected TreeColumn newColumn(String columnText, int weight) {
        Tree table = viewer.getTree();
        TreeColumn column = new TreeColumn(table, SWT.NONE);
        column.setText(columnText);

        TreeColumnLayout tableColumnLayout = (TreeColumnLayout) viewer.getTree().getParent().getLayout();
        if ( tableColumnLayout == null ) {
            tableColumnLayout = new TreeColumnLayout();
        }
        tableColumnLayout.setColumnData(column, new ColumnWeightData(weight));

        return column;
    }

    private final class AutoRefreshListener implements IServerListener {
        @Override
        public void serverChanged(ServerEvent event) {
            if ((event.getKind() & ServerEvent.SERVER_CHANGE) == 0) {
                return;
            }

            switch (event.getState()) {
            case IServer.STATE_STARTING:
                EventLogRefreshManager.getInstance().startAutoRefresh(EventLogEditorSection.this);
                break;
            default:
                EventLogRefreshManager.getInstance().stopAutoRefresh(EventLogEditorSection.this);
                break;
            }
        }

        public void dispose() {
            EventLogRefreshManager.getInstance().stopAutoRefresh(EventLogEditorSection.this);
        }
    }

    /** Populates the Event Log context menu with actions. */
    private final class EventLogMenuListener implements IMenuListener {

        private Action copyToClipboardAction = new Action("Copy to Clipboard") {
            @Override
            public ImageDescriptor getImageDescriptor() {
                return ElasticBeanstalkPlugin.getDefault().getImageRegistry().getDescriptor(ElasticBeanstalkPlugin.IMG_CLIPBOARD);
            }

            @Override
            public void run() {
                final Clipboard clipboard = new Clipboard(Display.getDefault());

                String eventText = "";
                for (TreeItem treeItem : viewer.getTree().getSelection()) {
                    if (eventText == null) {
                        eventText = treeItem.getData().toString();
                    } else {
                        eventText += "\n" + treeItem.getData().toString();
                    }
                }

                TextTransfer textTransfer = TextTransfer.getInstance();
                clipboard.setContents(new Object[]{eventText}, new Transfer[]{textTransfer});
            }
        };

        @Override
        public void menuAboutToShow(IMenuManager manager) {
            TreeItem[] selection = viewer.getTree().getSelection();
            copyToClipboardAction.setEnabled(selection != null && selection.length > 0);
            manager.add(copyToClipboardAction);
        }
    }

    private class LoadEnvironmentEventsJob extends Job {
        private final Environment environment;

        public LoadEnvironmentEventsJob(Environment environment) {
            super("Loading events for environment " + environment.getEnvironmentName());
            this.environment = environment;
            this.setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory(environment.getAccountId())
                    .getElasticBeanstalkClientByEndpoint(environment.getRegionEndpoint());
             final List<EventDescription> events = client.describeEvents(new DescribeEventsRequest()
                 .withEnvironmentName(environment.getEnvironmentName())).getEvents();

             Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (disposed || viewer.getTree().isDisposed()) {
                        return;
                    }

                    // Preserve the current column widths
                    int[] colWidth = new int[viewer.getTree().getColumns().length];
                    int i = 0;
                    for (TreeColumn col : viewer.getTree().getColumns()) {
                        colWidth[i++] = col.getWidth();
                    }

                    viewer.setInput(events);

                    // If this is the first time loading the table data, don't
                    // set the column widths -- this will make them zero on
                    // windows.
                    if ( tableDataLoaded ) {
                        i = 0;
                        for ( TreeColumn col : viewer.getTree().getColumns() ) {
                            col.setWidth(colWidth[i++]);
                        }
                    } else {
                        tableDataLoaded = true;
                    }
                }
             });

            return Status.OK_STATUS;
        }

        @Override
        public boolean belongsTo(Object family) {
            return family == JOB_FAMILY;
        }
    }

    private void addContextMenu() {
        MenuManager menuManager = new MenuManager("#PopupMenu");
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new EventLogMenuListener());
        Menu menu = menuManager.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
    }

    private void createEventsTable(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
        composite.setLayout(treeColumnLayout);

        int style = SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI;
        viewer = new TreeViewer(composite, style);
        viewer.getTree().setLinesVisible(true);
        viewer.getTree().setHeaderVisible(true);
        addContextMenu();

        newColumn("Message", 75);
        newColumn("Version", 10);
        newColumn("Date", 15);

        viewer.setContentProvider(new ITreeContentProvider() {
            private List<EventDescription> events;

            @Override
            @SuppressWarnings("unchecked")
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                if (newInput == null) {
                    events = new ArrayList<>();
                } else {
                    events = (List<EventDescription>)newInput;
                }
            }

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                return events.toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return new Object[0];
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return false;
            }
        });

        viewer.setLabelProvider(new ITableLabelProvider() {

            @Override
            public void removeListener(ILabelProviderListener listener) {
            }

            @Override
            public boolean isLabelProperty(Object element, String property) {
                return false;
            }

            @Override
            public void dispose() {
            }

            @Override
            public void addListener(ILabelProviderListener listener) {
            }

            @Override
            public String getColumnText(Object element, int columnIndex) {
                EventDescription event = (EventDescription) element;
                switch (columnIndex) {
                case 0:
                    return event.getMessage();
                case 1:
                    return event.getVersionLabel();
                case 2:
                    return event.getEventDate().toString();

                default:
                    return "";
                }
            }

            @Override
            public Image getColumnImage(Object element, int columnIndex) {
                if (element == null) {
                    return null;
                }
                if (columnIndex != 0) {
                    return null;
                }

                EventSeverity eventSeverity = null;
                try {
                    EventDescription event = (EventDescription)element;
                    eventSeverity = EventSeverity.fromValue(event.getSeverity());
                } catch (IllegalArgumentException e) {
                    return null;
                }

                switch (eventSeverity) {
                case ERROR:
                case FATAL:
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
                case WARN:
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
                case INFO:
                case DEBUG:
                case TRACE:
                    return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
                }

                return null;
            }
        });
    }

    @Override
    public void dispose() {
        disposed = true;

        if (autoRefreshListener != null) {
            Environment environment = (Environment) server.loadAdapter(Environment.class, null);
            environment.getServer().removeServerListener(autoRefreshListener);
            autoRefreshListener.dispose();
        }

        super.dispose();
    }

    public String getServerName() {
        return server.getName();
    }

    /**
     * Refreshes the events in the table.
     */
    void refresh() {
        /*
         * There's a race condition here, but the consequences are trivial.
         */
        if ( Job.getJobManager().find(JOB_FAMILY).length == 0 ) {
            Environment environment = (Environment) server.loadAdapter(Environment.class, null);
            new LoadEnvironmentEventsJob(environment).schedule();
        }
    }

}
