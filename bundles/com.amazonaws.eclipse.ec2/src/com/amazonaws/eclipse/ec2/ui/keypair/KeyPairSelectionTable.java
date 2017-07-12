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
package com.amazonaws.eclipse.ec2.ui.keypair;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.KeyPairInfo;

/**
 * Table for selecting key pairs.
 */
public class KeyPairSelectionTable extends SelectionTable {

    private final Collection<KeyPairRefreshListener> listeners = new LinkedList<>();

    @Override
    public TreeViewer getViewer() {
        return (TreeViewer) super.getViewer();
    }

    public Action deleteKeyPairAction;
    public Action createNewKeyPairAction;
    public Action refreshAction;
    public Action registerKeyPairAction;

    private final String accountId;

    private static final KeyPairManager keyPairManager = new KeyPairManager();

    private final Image errorImage = Ec2Plugin.getDefault().getImageRegistry().getDescriptor("error").createImage();
    private final Image checkImage = Ec2Plugin.getDefault().getImageRegistry().getDescriptor("check").createImage();

    public static final String INVALID_KEYPAIR_MESSAGE =
        "The selected key pair is missing its private key.  " +
        "You can associate the private key file with this key pair if you have it, " +
        "or you can create a new key pair. ";


    /**
     * Refreshes the EC2 key pairs displayed.
     */
    public void refreshKeyPairs() {
        new RefreshKeyPairsThread().start();
    }

    public synchronized void addRefreshListener(KeyPairRefreshListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeRefreshListener(KeyPairSelectionTable listener) {
        listeners.remove(listener);
    }


    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        errorImage.dispose();
        checkImage.dispose();

        super.dispose();
    }

    public void updateActionsForSelection() {
        KeyPairInfo selectedKeyPair = getSelectedKeyPair();
        refreshAction.setEnabled(true);
        createNewKeyPairAction.setEnabled(true);
        deleteKeyPairAction.setEnabled(selectedKeyPair != null);

        // We only enable the register key pair action if the selected
        // key pair doesn't have a private key file registered yet
        registerKeyPairAction.setEnabled(false);
        if (selectedKeyPair != null) {
            String privateKeyFile = keyPairManager.lookupKeyPairPrivateKeyFile(accountId, selectedKeyPair.getKeyName());
            registerKeyPairAction.setEnabled(privateKeyFile == null);
        }
    }


    /**
     * Label and content provider for the key pair table.
     */
    class KeyPairTableProvider extends LabelProvider implements ITreeContentProvider, ITableLabelProvider {
        List<KeyPairInfo> keyPairs;

        /*
         * IStructuredContentProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object inputElement) {
            if (keyPairs == null) return null;

            return keyPairs.toArray();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            keyPairs = (List<KeyPairInfo>)newInput;
        }

        /*
         * ITableLabelProvider Interface
         */

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
         */
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex != 0) return null;

            if (!(element instanceof KeyPairInfo)) return null;
            KeyPairInfo keyPairInfo = (KeyPairInfo)element;

            if (keyPairManager.isKeyPairValid(accountId, keyPairInfo.getKeyName())) {
                return checkImage;
            } else {
                return errorImage;
            }
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof KeyPairInfo)) {
                return "???";
            }

            KeyPairInfo keyPairInfo = (KeyPairInfo)element;
            switch (columnIndex) {
            case 0:
                return keyPairInfo.getKeyName();
            }

            return "?";
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
     * Simple comparator to sort key pairs by name.
     *
     * @author Jason Fulghum <fulghum@amazon.com>
     */
    class KeyPairComparator extends ViewerComparator {
        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (!(e1 instanceof KeyPairInfo && e2 instanceof KeyPairInfo)) {
                return 0;
            }

            KeyPairInfo keyPair1 = (KeyPairInfo)e1;
            KeyPairInfo keyPair2 = (KeyPairInfo)e2;

            return keyPair1.getKeyName().compareTo(keyPair2.getKeyName());
        }
    }

    /**
     * Create a key pair table for the specified account Id.
     */
    public KeyPairSelectionTable(Composite parent, String accountId) {
        this(parent, accountId, null);
    }

    /**
     * Create a key pair table for the specified account Id and ec2 endpoint.
     */
    public KeyPairSelectionTable(Composite parent, String accountId, Region ec2RegionOverride) {
        super(parent);
        this.accountId = accountId;

        KeyPairTableProvider keyPairTableProvider = new KeyPairTableProvider();

        viewer.setContentProvider(keyPairTableProvider);
        viewer.setLabelProvider(keyPairTableProvider);
        viewer.setComparator(new KeyPairComparator());

        refreshKeyPairs();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateActionsForSelection();
            }
        });

        this.ec2RegionOverride = ec2RegionOverride;
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Name", 100);
    }

    /**
     * Sets the list of key pairs that are displayed in the key pair table.
     *
     * @param keyPairs
     *            The list of key pairs to display in the key pair table.
     */
    private void setInput(final List<KeyPairInfo> keyPairs) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                final KeyPairInfo previouslySelectedKeyPair = (KeyPairInfo)getSelection();
                viewer.setInput(keyPairs);
                if (previouslySelectedKeyPair != null) {
                    for (int i = 0; i < viewer.getTree().getItemCount(); i++) {
                        KeyPairInfo keyPair = (KeyPairInfo) viewer.getTree().getItem(i).getData();
                        if (keyPair.getKeyName().equals(previouslySelectedKeyPair.getKeyName())) {
                            viewer.getTree().select(viewer.getTree().getItem(i));
                            // Selection listeners aren't notified when we
                            // select like this, so fire an event.
                            viewer.getTree().notifyListeners(SWT.Selection, null);
                            break;
                        }
                    }
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(createNewKeyPairAction);
        manager.add(deleteKeyPairAction);
        manager.add(registerKeyPairAction);

        updateActionsForSelection();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        refreshAction = new Action() {
            @Override
            public void run() {
                refreshKeyPairs();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Refresh the list of key pairs.");
        refreshAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));

        createNewKeyPairAction = new Action() {
            @Override
            public void run() {
                CreateKeyPairDialog dialog = new CreateKeyPairDialog(Display.getCurrent().getActiveShell(), accountId);
                if (dialog.open() != Dialog.OK) return;
                new CreateKeyPairThread(dialog.getKeyPairName(), dialog.getPrivateKeyDirectory()).start();
            }
        };
        createNewKeyPairAction.setText("New Key Pair...");
        createNewKeyPairAction.setToolTipText("Create a new key pair.");
        createNewKeyPairAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("add"));

        deleteKeyPairAction = new Action() {
            @Override
            public void run() {
                KeyPairInfo keyPair = (KeyPairInfo)getSelection();
                new DeleteKeyPairThread(keyPair).start();
            }
        };
        deleteKeyPairAction.setText("Delete Key Pair");
        deleteKeyPairAction.setToolTipText("Delete the selected key pair.");
        deleteKeyPairAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));
        deleteKeyPairAction.setEnabled(false);

        registerKeyPairAction = new Action() {
            @Override
            public void run() {
                KeyPairInfo keyPair = (KeyPairInfo)getSelection();

                FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
                fileDialog.setText("Select the existing private key file for " + keyPair.getKeyName());
                fileDialog.setFilterExtensions(new String[] { "*.pem", "*.*" });
                String privateKeyFile = fileDialog.open();

                if (privateKeyFile != null) {
                    try {
                        keyPairManager.registerKeyPair(accountId, keyPair.getKeyName(), privateKeyFile);
                        refreshKeyPairs();
                    } catch (IOException e) {
                        String errorMessage = "Unable to register key pair " +
                                "(" + keyPair.getKeyName() + " => " + privateKeyFile + "): " + e.getMessage();
                        Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID, errorMessage, e);
                        StatusManager.getManager().handle(status, StatusManager.LOG | StatusManager.SHOW);
                    }
                }
            }
        };
        registerKeyPairAction.setText("Select Private Key File...");
        registerKeyPairAction.setToolTipText("Register an existing private key with this key pair.");
        registerKeyPairAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("configure"));
        registerKeyPairAction.setEnabled(false);
    }

    /**
     * Returns the user selected key pair.
     *
     * @return The user selected key pair.
     */
    public KeyPairInfo getSelectedKeyPair() {
        return (KeyPairInfo)getSelection();
    }

    /**
     * Returns true if, and only if, a valid key pair is selected, which
     * includes checking that a private key is registered for that key pair.
     *
     * @return True if and only if a valid key pair is selected, which includes
     *         checking that a private key is registered for that key pair.
     */
    public boolean isValidKeyPairSelected() {
        KeyPairInfo selectedKeyPair = getSelectedKeyPair();
        if (selectedKeyPair == null) return false;

        return keyPairManager.isKeyPairValid(accountId, selectedKeyPair.getKeyName());
    }


    /*
     * Private Thread subclasses for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to list all key pairs.
     */
    private class RefreshKeyPairsThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                AmazonEC2 ec2 = getAwsEc2Client(KeyPairSelectionTable.this.accountId);

                DescribeKeyPairsResult response = ec2.describeKeyPairs(new DescribeKeyPairsRequest());
                List<KeyPairInfo> keyPairs = response.getKeyPairs();
                setInput(keyPairs);
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        for ( KeyPairRefreshListener listener : listeners ) {
                            listener.keyPairsRefreshed();
                        }
                    }
                });
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to list key pairs: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

    /**
     * Thread for making an EC2 service call to delete a key pair.
     */
    private class DeleteKeyPairThread extends Thread {
        /** The key pair to delete */
        private final KeyPairInfo keyPair;

        /**
         * Creates a new thread ready to be started to delete the specified key
         * pair.
         *
         * @param keyPair
         *            The key pair to delete.
         */
        public DeleteKeyPairThread(final KeyPairInfo keyPair) {
            this.keyPair = keyPair;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                DeleteKeyPairRequest request = new DeleteKeyPairRequest();
                request.setKeyName(keyPair.getKeyName());
                getAwsEc2Client(accountId).deleteKeyPair(request);
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to delete key pair: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }

            refreshKeyPairs();
        }
    }

    /**
     * Thread for making an EC2 service call to create a new key pair.
     */
    private class CreateKeyPairThread extends Thread {
        /** The name for the new key pair */
        private final String name;
        /** The directory to store the private key in */
        private final String directory;

        /**
         * Creates a new thread ready to be started to create a new key pair
         * with the specified name.
         *
         * @param name
         *            The name being requested for the new key pair.
         * @param directory
         *            The directory to save the private key file in.
         */
        public CreateKeyPairThread(String name, String directory) {
            this.directory = directory;
            this.name = name;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                keyPairManager.createNewKeyPair(accountId, name, directory, ec2RegionOverride);
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to create key pair: " + e.getMessage());
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }
            refreshKeyPairs();
        }
    }

    @Override
    public void setEc2RegionOverride(Region region) {
        super.setEc2RegionOverride(region);
        refreshKeyPairs();
    }

}
