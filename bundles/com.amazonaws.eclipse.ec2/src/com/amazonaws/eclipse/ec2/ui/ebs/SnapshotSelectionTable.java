/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.ui.ebs;

import java.text.DateFormat;
import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.ec2.ui.SelectionTableComparator;
import com.amazonaws.eclipse.ec2.ui.views.instances.RefreshTimer;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.Snapshot;

/**
 * SelectionTable subclass for selecting EBS snapshots.
 */
public class SnapshotSelectionTable extends SelectionTable implements IRefreshable {

    /** The period between refreshes when snapshots are in progress */
    private static final int REFRESH_PERIOD_IN_MILLISECONDS = 10 * 1000;

    private static final int STATE_COLUMN = 0;
    private static final int SNAPSHOT_ID_COLUMN = 1;
    private static final int VOLUME_ID_COLUMN = 2;
    private static final int START_TIME_COLUMN = 3;
    private static final int TAGS_COLUMN = 4;

    /** An Action implementation that refreshes the snapshots */
    private Action refreshAction;

    /** An Action implementation that deletes the selected snapshot */
    private Action deleteAction;

    /**
     * The timer we turn on when snapshots are in progress to refresh
     * the snapshot data.
     */
    private RefreshTimer refreshTimer;

    /**
     * Comparator for sorting snapshots by creation time.
     */
    class SnapshotComparator extends SelectionTableComparator {

        public SnapshotComparator(int defaultColumn) {
            super(defaultColumn);
        }

        /* (non-Javadoc)
         * @see com.amazonaws.eclipse.ec2.ui.SelectionTableComparator#compareIgnoringDirection(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        protected int compareIgnoringDirection(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof Snapshot && e2 instanceof Snapshot)) {
                return 0;
            }

            Snapshot snapshot1 = (Snapshot)e1;
            Snapshot snapshot2 = (Snapshot)e2;
            
            switch (this.sortColumn) {
            case STATE_COLUMN:
                return snapshot1.getState().compareTo(snapshot2.getState());
            case SNAPSHOT_ID_COLUMN:
                return snapshot1.getSnapshotId().compareTo(snapshot2.getSnapshotId());
            case VOLUME_ID_COLUMN:
                return snapshot1.getVolumeId().compareTo(snapshot2.getVolumeId());
            case START_TIME_COLUMN:
                return snapshot1.getStartTime().compareTo(snapshot2.getStartTime());
            case TAGS_COLUMN:
                return TagFormatter.formatTags(snapshot1.getTags()).compareTo(
                        TagFormatter.formatTags(snapshot2.getTags()));
            }

            return 0;
        }
    }

    /**
     * Label and content provider for snapshot selection table.
     */
    private static class SnapshotTableProvider extends LabelProvider
            implements ITreeContentProvider, ITableLabelProvider {
        
        private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

        List<Object> snapshots;

        /*
         * IStructuredContentProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            if (snapshots == null) return null;

            return snapshots.toArray();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput instanceof List) {
                snapshots = (List<Object>)newInput;
            }
        }

        /*
         * ITableLabelProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
         */
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0)
                return Ec2Plugin.getDefault().getImageRegistry().get("snapshot");
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof Snapshot)) {
                return "???";
            }

            Snapshot snapshot = (Snapshot)element;

            switch (columnIndex) {
            case STATE_COLUMN:
                if (snapshot.getState().equalsIgnoreCase("pending")) {
                    if (snapshot.getProgress() != null && snapshot.getProgress().length() > 0) {
                        return "pending (" + snapshot.getProgress() + ")";
                    } else {
                        return "pending";
                    }
                }
                return snapshot.getState();
            case SNAPSHOT_ID_COLUMN:
                return snapshot.getSnapshotId();
            case VOLUME_ID_COLUMN:
                return snapshot.getVolumeId();
            case START_TIME_COLUMN:
                if (snapshot.getStartTime() == null) return "";
                return dateFormat.format(snapshot.getStartTime());
            case TAGS_COLUMN:
                return TagFormatter.formatTags(snapshot.getTags());
            }

            return "???";
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
    }


    /**
     * Creates a new snapshot selection table with the specified parent.
     *
     * @param parent
     *            The parent ui component for the new snapshot selection table.
     */
    public SnapshotSelectionTable(Composite parent) {
        super(parent);

        SnapshotTableProvider snapshotTableProvider = new SnapshotTableProvider();
        viewer.setContentProvider(snapshotTableProvider);
        viewer.setLabelProvider(snapshotTableProvider);
        
        setComparator(new SnapshotComparator(START_TIME_COLUMN));

        refreshSnapshots();

        refreshTimer = new RefreshTimer(this, REFRESH_PERIOD_IN_MILLISECONDS);
    }

    /**
     * Returns the currently selected snapshot.
     *
     * @return The currently selected snapshot.
     */
    public Snapshot getSelectedSnapshot() {
        return (Snapshot)getSelection();
    }

    /**
     * Refreshes the list of snapshots displayed by this snapshot selection
     * table.
     */
    public void refreshSnapshots() {
        new RefreshSnapshotsThread().start();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        this.refreshSnapshots();
    }


    /*
     * SelectionTable Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("State", 10);
        newColumn("Snapshot ID", 10);
        newColumn("Volume ID", 10);
        newColumn("Creation Time", 60);
        newColumn("Tags", 15);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        Snapshot selectedSnapshot = getSelectedSnapshot();

        boolean isSnapshotSelected = (selectedSnapshot != null);
        deleteAction.setEnabled(isSnapshotSelected);

        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(deleteAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        refreshAction = new Action() {
            @Override
            public void run() {
                refreshSnapshots();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh the list of snapshots.");
        refreshAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));

        deleteAction = new Action () {
            @Override
            public void run() {
                Snapshot selectedSnapshot = getSelectedSnapshot();
                new DeleteSnapshotThread(selectedSnapshot).start();
            }
        };
        deleteAction.setText("Delete Snapshot");
        deleteAction.setToolTipText("Delete the selected snapshot.");
        deleteAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));
    }

    /*
     * Private Interface
     */

    /**
     * Sets the input of this control to the specified list of snapshots and
     * also takes care of any special requirements of in progress snapshots such
     * as turning on the refresh timer and displaying progress bars for the
     * snapshot progress.
     *
     * @param snapshots
     *            The list of snapshots that should be displayed in this
     *            selection table.
     */
    private void setInput(final List<Snapshot> snapshots) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                Snapshot previouslySelectedSnapshot = getSelectedSnapshot();

                viewer.setInput(snapshots);
                packColumns();

                if (previouslySelectedSnapshot != null) {
                    Tree table = viewer.getTree();
                    for (int i = 0; i < table.getItemCount(); i++) {
                        Snapshot snapshot = (Snapshot)table.getItem(i).getData();
                        if (snapshot.getSnapshotId().equals(previouslySelectedSnapshot.getSnapshotId())) {
                            table.select(table.getItem(i));
                        }
                    }
                }

                resetRefreshTimer(snapshots);
            }
        });
    }

    /**
     * Turns on or off the refresh timer depending on whether or not any of the
     * specified snapshots are currently in progress. If snapshots are in
     * progress, the refresh timer is turned on so that the user watch as the
     * snapshot progresses, otherwise the refresh timer is turned off.
     *
     * @param snapshots
     *            The snapshots being displayed by this control.
     */
    private void resetRefreshTimer(List<Snapshot> snapshots) {
        for (Snapshot snapshot : snapshots) {
            if (snapshot.getState().equalsIgnoreCase("pending")) {
                refreshTimer.startTimer();
                return;
            }
        }

        refreshTimer.stopTimer();
    }

    /*
     * Private Threads for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to refresh the list of EBS
     * snapshots for the current account.
     */
    private class RefreshSnapshotsThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 ec2 = getAwsEc2Client();
                final List<Snapshot> snapshots = ec2.describeSnapshots().getSnapshots();
                setInput(snapshots);
            } catch (Exception e) {
                // Only log an error if the account info is valid and we
                // actually expected this call to work
                if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to refresh snapshots: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }
    }

    /**
     * Thread for making an EC2 service call to delete a snapshot.
     */
    private class DeleteSnapshotThread extends Thread {
        /** The snapshot to delete */
        private final Snapshot snapshot;

        /**
         * Creates a new thread ready to be started to delete the specified
         * snapshot.
         *
         * @param snapshot
         *            The snapshot to delete.
         */
        public DeleteSnapshotThread(Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                getAwsEc2Client().deleteSnapshot(new DeleteSnapshotRequest()
                    .withSnapshotId(snapshot.getSnapshotId()));

                refreshSnapshots();
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to delete snapshot: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

}
