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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.ec2.ui.SelectionTableComparator;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;

/**
 * Selection table subclass for displaying EBS volumes.
 */
public class VolumeSelectionTable extends SelectionTable {


    private static final int VOLUME_ID_COLUMN = 0;
    private static final int STATUS_COLUMN = 1;
    private static final int CREATE_TIME_COLUMN = 2;
    private static final int ATTACHED_INSTANCE_COLUMN = 3;
    private static final int SIZE_COLUMN = 4;
    private static final int ZONE_COLUMN = 5;
    private static final int SNAPSHOT_ID_COLUMN = 6;
    private static final int TAGS_COLUMN = 7;
    
    /** An Action implementation that releases a volume */
    private Action releaseAction;

    /** An Action implementation that detaches a volume */
    private Action detachAction;

    /** An Action implementation that creates a snapshot */
    private Action createSnapshotAction;

    /** An Action implementation that refreshes the volume list */
    private Action refreshAction;

    /** An Action implementation that creates a new volume */
    private Action createAction;

    /**
     * An optional reference to a snapshot selection table that should be
     * refreshed when new snapshots are created.
     */
    private SnapshotSelectionTable snapshotSelectionTable;


    /**
     * Content provider for the volume selection table.
     */
    private static class VolumeContentProvider implements ITreeContentProvider {
        List<Volume> volumes;

        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            volumes = (List<Volume>)newInput;
        }
        @Override
        public void dispose() {
        }
        @Override
        public Object[] getElements(Object parent) {
            if (volumes == null) {
                return new Object[0];
            }

            return volumes.toArray();
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
     * Label provider for entries in the volume selection table.
     */
    private static class VolumeLabelProvider extends LabelProvider implements ITableLabelProvider {
        private final DateFormat dateFormat = DateFormat.getDateTimeInstance();

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText(Object obj, int index) {
            Volume volume = (Volume)obj;
            if (volume == null) return "";

            switch (index) {
            case VOLUME_ID_COLUMN:
                return volume.getVolumeId();
            case STATUS_COLUMN:
                return volume.getState();
            case CREATE_TIME_COLUMN:
                if (volume.getCreateTime() == null) return "";
                return dateFormat.format(volume.getCreateTime());
            case ATTACHED_INSTANCE_COLUMN:
                for (VolumeAttachment attachment : volume.getAttachments()) {
                    return attachment.getInstanceId();
                }
                return "";
            case SIZE_COLUMN:
                return volume.getSize().toString();
            case ZONE_COLUMN:
                return volume.getAvailabilityZone();
            case SNAPSHOT_ID_COLUMN:
                return volume.getSnapshotId();
            case TAGS_COLUMN:
                return TagFormatter.formatTags(volume.getTags());
            }

            return "???";
        }
        @Override
        public Image getColumnImage(Object obj, int index) {
            if (index == 0)
                return Ec2Plugin.getDefault().getImageRegistry().get("volume");
            return null;
        }
        @Override
        public Image getImage(Object obj) {
            return null;
        }
    }

    /**
     * Comparator for sorting volumes by creation time.
     */
    private static class VolumeComparator extends SelectionTableComparator {

        /**
         * @param defaultColumn
         */
        public VolumeComparator(int defaultColumn) {
            super(defaultColumn);
        }

        /* (non-Javadoc)
         * @see com.amazonaws.eclipse.ec2.ui.SelectionTableComparator#compareIgnoringDirection(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        protected int compareIgnoringDirection(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof Volume && e2 instanceof Volume)) {
                return 0;
            }

            Volume volume1 = (Volume)e1;
            Volume volume2 = (Volume)e2;

            switch (this.sortColumn) {
            case VOLUME_ID_COLUMN:
                return volume1.getVolumeId().compareTo(volume2.getVolumeId());
            case STATUS_COLUMN:
                return volume1.getState().compareTo(volume2.getState());
            case CREATE_TIME_COLUMN:
                return volume1.getCreateTime().compareTo(volume2.getCreateTime());
            case ATTACHED_INSTANCE_COLUMN:
                String instanceId1 = "";
                String instanceId2 = "";
                for (VolumeAttachment att : volume1.getAttachments())
                    instanceId1 = att.getInstanceId();
                for (VolumeAttachment att : volume2.getAttachments())
                    instanceId2 = att.getInstanceId();                
                return instanceId1.compareTo(instanceId2);
            case SIZE_COLUMN:
                return volume1.getSize().compareTo(volume2.getSize());
            case ZONE_COLUMN:
                return volume1.getAvailabilityZone().compareTo(volume2.getAvailabilityZone());
            case SNAPSHOT_ID_COLUMN:
                return volume1.getSnapshotId().compareTo(volume2.getSnapshotId());
            case TAGS_COLUMN:
                return TagFormatter.formatTags(volume1.getTags()).compareTo(TagFormatter.formatTags(volume2.getTags()));
            }
            
            return 0;            
        }
    }


    /*
     * Public Interface
     */

    /**
     * Instantiates a new volume selection table as a child of the specified
     * parent composite.
     *
     * @param parent
     *            The parent composite for this new selection table.
     */
    public VolumeSelectionTable(Composite parent) {
        super(parent);

        viewer.setContentProvider(new VolumeContentProvider());
        viewer.setLabelProvider(new VolumeLabelProvider());
        setComparator(new VolumeComparator(CREATE_TIME_COLUMN));

        refreshVolumes();
    }

    /**
     * Refreshes the volumes in this selection table.
     */
    public void refreshVolumes() {
        if (viewer == null) return;

        new RefreshVolumesThread().start();
    }

    /**
     * Sets the optional snapshot selection table that should be refreshed when
     * new snapshots are created from volumes.
     *
     * @param snapshotSelectionComposite
     *            The snapshot selection table.
     */
    public void setSnapshotSelectionTable(SnapshotSelectionTable snapshotSelectionComposite) {
        this.snapshotSelectionTable = snapshotSelectionComposite;
    }

    /**
     * Returns the currently selected volume.
     *
     * @return The currently selected volume.
     */
    public Volume getSelectedVolume() {
        return (Volume)this.getSelection();
    }


    /*
     * SelectionTable Interface
     */
    
    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Volume ID", 10);
        newColumn("Status", 10);
        newColumn("Create Time", 10);
        newColumn("Attached Instance", 10);
        newColumn("Size (GB)", 10);
        newColumn("Zone", 10);
        newColumn("Snapshot ID", 10);
        newColumn("Tags", 15);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        Volume selectedVolume = getSelectedVolume();

        boolean isVolumeSelected = (selectedVolume != null);
        boolean isAvailableVolumeSelected = isVolumeSelected && selectedVolume.getState().equalsIgnoreCase("available");
        boolean isAttachedVolumeSelected = isVolumeSelected && !(selectedVolume.getAttachments().isEmpty());

        releaseAction.setEnabled(isAvailableVolumeSelected);
        detachAction.setEnabled(isAttachedVolumeSelected);
        createSnapshotAction.setEnabled(isVolumeSelected);

        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(createAction);
        manager.add(releaseAction);
        manager.add(detachAction);
        manager.add(new Separator());
        manager.add(createSnapshotAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        refreshAction = new Action() {
            @Override
            public void run() {
                refreshVolumes();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh the volume list");
        refreshAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));

        createAction = new Action() {
            @Override
            public void run() {
                final CreateNewVolumeDialog dialog = new CreateNewVolumeDialog(Display.getCurrent().getActiveShell());
                if (dialog.open() != IDialogConstants.OK_ID) return;

                new CreateVolumeThread(dialog.getAvailabilityZone(), dialog.getSize(), dialog.getSnapshotId()).start();
            }
        };
        createAction.setText("New Volume");
        createAction.setToolTipText("Create a new EBS volume");
        createAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("add"));

        releaseAction = new Action() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                StructuredSelection selection = (StructuredSelection)viewer.getSelection();
                new ReleaseVolumesThread(selection.toList()).start();
            }
        };
        releaseAction.setText("Release Volume");
        releaseAction.setDescription("Release selected volume(s)");
        releaseAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));

        detachAction = new Action() {
            @Override
            public void run() {
                final Volume volume = getSelectedVolume();
                new DetachVolumeThread(volume).start();
            }
        };
        detachAction.setText("Detach Volume");
        detachAction.setToolTipText("Detach the selected volume from all instances.");

        createSnapshotAction = new Action() {
            @Override
            public void run() {
                new CreateSnapshotThread(getSelectedVolume()).start();
            }
        };
        createSnapshotAction.setText("Create Snapshot");
        createSnapshotAction.setToolTipText("Creates a new snapshot of this volume.");
        createSnapshotAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("snapshot"));
    }


    /*
     * Private Threads for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to refresh the list of EBS volumes.
     */
    private class RefreshVolumesThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                if (selectionTableListener != null) selectionTableListener.loadingData();

                final AmazonEC2 ec2 = getAwsEc2Client();
                final List<Volume> volumes = ec2.describeVolumes(new DescribeVolumesRequest()).getVolumes();

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(volumes);
                        packColumns();
                    }
                });
            } catch (Exception e) {
                // Only log an error if the account info is valid and we
                // actually expected this call to work
                if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to refresh EBS volumes: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            } finally {
                if (selectionTableListener != null) selectionTableListener.finishedLoadingData(-1);
            }
        }
    }

    /**
     * Thread for making an EC2 service call to detach an EBS volume.
     */
    private class DetachVolumeThread extends Thread {
        /** The volume to detach */
        private final Volume volume;

        /**
         * Creates a new thread ready to be started to detach the specified
         * volume.
         *
         * @param volume
         *            The EBS volume to detach.
         */
        public DetachVolumeThread(final Volume volume) {
            this.volume = volume;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            for (VolumeAttachment attachmentInfo : volume.getAttachments()) {
                String volumeId = attachmentInfo.getVolumeId();
                String instanceId = attachmentInfo.getInstanceId();
                String device = attachmentInfo.getDevice();

                try {
                    DetachVolumeRequest request = new DetachVolumeRequest();
                    request.setVolumeId(volumeId);
                    request.setInstanceId(instanceId);
                    request.setDevice(device);
                    request.setForce(false);
                    getAwsEc2Client().detachVolume(request);
                } catch (Exception e) {
                    Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to detach EBS volume: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
                }
            }

            refreshVolumes();
        }
    }

    /**
     * Thread for making an EC2 service call to create a new EBS volume.
     */
    private class CreateVolumeThread extends Thread {
        /** The zone to create the volume in */
        private final String zoneName;
        /** The snapshot to create the volume from (optional) */
        private final String snapshotId;
        /** The size of the new volume */
        private final int size;

        /**
         * Creates a new thread ready to be started to create a new EBS volume.
         *
         * @param zoneName
         *            The availability zone to create the new volume in.
         * @param size
         *            The size of the new volume.
         * @param snapshotId
         *            An optional snapshot id to create the volume from.
         */
        public CreateVolumeThread(String zoneName, int size, String snapshotId) {
            this.zoneName = zoneName;
            this.size = size;
            this.snapshotId = snapshotId;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                CreateVolumeRequest request = new CreateVolumeRequest();
                // Only set size if we're not using a snapshot
                if (snapshotId == null) request.setSize(size);
                request.setSnapshotId(snapshotId);
                request.setAvailabilityZone(zoneName);

                getAwsEc2Client().createVolume(request);

                refreshVolumes();
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to create EBS volume: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

    /**
     * Thread for making an EC2 service call to release EBS volumes.
     */
    private class ReleaseVolumesThread extends Thread {
        /** The volumes to release */
        private final List<Volume> volumes;

        /**
         * Creates a new thread ready to be started and release the specified
         * volumes.
         *
         * @param volumes
         *            The EBS volumes to release.
         */
        public ReleaseVolumesThread(final List<Volume> volumes) {
            this.volumes = volumes;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 ec2 = getAwsEc2Client();
                for (Volume volume : volumes) {
                    DeleteVolumeRequest request = new DeleteVolumeRequest();
                    request.setVolumeId(volume.getVolumeId());
                    ec2.deleteVolume(request);
                }
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to release EBS volume: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }

            refreshVolumes();
        }
    }

    /**
     * Thread to make an EC2 service call to create a new snapshot from an EBS
     * volume.
     */
    private class CreateSnapshotThread extends Thread {
        /** The volume to create a snapshot from */
        private final Volume volume;

        /**
         * Creates a new thread ready to be started to create a snapshot of the
         * specified volume.
         *
         * @param volume
         *            The volume to create a snapshot of.
         */
        public CreateSnapshotThread(final Volume volume) {
            this.volume = volume;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                CreateSnapshotRequest request = new CreateSnapshotRequest();
                request.setVolumeId(volume.getVolumeId());
                getAwsEc2Client().createSnapshot(request);

                if (snapshotSelectionTable != null) {
                    snapshotSelectionTable.refreshSnapshots();
                }
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to create snapshot: " + e.getMessage(), e);
                StatusManager.getManager().handle(status,
                        StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }
}
