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

package com.amazonaws.eclipse.ec2.ui.elasticip;

import java.util.List;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;

/**
 * Selection table for selecting an Elastic IP.
 */
public class ElasticIpComposite extends SelectionTable {

    private Action newAddressAction;
    private Action refreshAddressesAction;
    private Action releaseAddressAction;

    /**
     * Creates a new Elastic IP selection table parented by the specified
     * composite.
     *
     * @param parent
     *            The UI parent of this new selection table.
     */
    public ElasticIpComposite(Composite parent) {
        super(parent);

        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());

        refreshAddressList();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#getSelection()
     */
    @Override
    public Address getSelection() {
        StructuredSelection selection = (StructuredSelection)viewer.getSelection();

        return (Address)selection.getFirstElement();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#addSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    @Override
    public void addSelectionListener(SelectionListener listener) {
        viewer.getTree().addSelectionListener(listener);
    }

    public Action getNewAddressAction() {
        return newAddressAction;
    }

    public Action getRefreshAddressesAction() {
        return refreshAddressesAction;
    }

    public Action getReleaseAddressAction() {
        return releaseAddressAction;
    }


    /*
     * SelectionTable Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        manager.add(refreshAddressesAction);
        manager.add(new Separator());
        manager.add(newAddressAction);
        manager.add(releaseAddressAction);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        newAddressAction = new Action() {
            @Override
            public void run() {
                new RequestElasticIpThread().start();
            }
        };
        newAddressAction.setText("New Elastic IP");
        newAddressAction.setToolTipText("Requests a new Elastic IP address");
        newAddressAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("add"));

        refreshAddressesAction = new Action() {
            @Override
            public void run() {
                refreshAddressList();
            }
        };
        refreshAddressesAction.setText("Refresh");
        refreshAddressesAction.setToolTipText("Refresh the Elastic IP address list");
        refreshAddressesAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));

        releaseAddressAction = new Action() {
            @Override
            public void run() {
                StructuredSelection selection = (StructuredSelection)viewer.getSelection();
                Address addressInfo = (Address) selection.getFirstElement();

                new ReleaseElasticIpThread(addressInfo).start();
            }
        };
        releaseAddressAction.setText("Release");
        releaseAddressAction.setToolTipText("Release this Elastic IP");
        releaseAddressAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Elastic IP", 40);
        newColumn("Attached Instance", 60);
    }


    /*
     * Private Interface
     */

    private void refreshAddressList() {
        new RefreshAddressListThread().start();
    }


    /*
     * Content and Label Providers
     */

    private class ViewContentProvider implements ITreeContentProvider {
        List<Address> addresses;

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            addresses = (List<Address>)newInput;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        @Override
        public void dispose() {
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        @Override
        public Object[] getElements(Object parent) {
            if (addresses == null) {
                return new Object[0];
            }

            return addresses.toArray();
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

    private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
         */
        @Override
        public String getColumnText(Object obj, int index) {
            if (!Address.class.isInstance(obj)) {
                return "???";
            }

            Address addressInfo = (Address)obj;

            switch(index) {
            case 0:
                return addressInfo.getPublicIp();
            case 1:
                return addressInfo.getInstanceId();
            }

            return getText(obj);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
         */
        @Override
        public Image getColumnImage(Object obj, int index) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
         */
        @Override
        public Image getImage(Object obj) {
            return PlatformUI.getWorkbench().
                    getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
    }


    /*
     * Private Threads for making EC2 service calls
     */

    /**
     * Thread for making an EC2 service call to list all Elastic IPs for the
     * current account.
     */
    private class RefreshAddressListThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                final List<Address> addresses = getAwsEc2Client().describeAddresses().getAddresses();

                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.setInput(addresses);
                        packColumns();
                    }
                });
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to list Elastic IPs: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

    /**
     * Thread for making an EC2 service call to release an Elastic IP.
     */
    private class ReleaseElasticIpThread extends Thread {
        /** The Elastic IP to release */
        private final Address addressInfo;

        /**
         * Creates a new thread ready to be started to release the specified
         * Elastic IP.
         *
         * @param addressInfo
         *            The Elastic IP to release.
         */
        public ReleaseElasticIpThread(final Address addressInfo) {
            this.addressInfo = addressInfo;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                ReleaseAddressRequest request = new ReleaseAddressRequest();
                request.setPublicIp(addressInfo.getPublicIp());
                getAwsEc2Client().releaseAddress(request);
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to release Elastic IP: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }

            refreshAddressList();
        }
    }

    /**
     * Thread for making an EC2 service call to request a new Elastic IP.
     */
    private class RequestElasticIpThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                getAwsEc2Client().allocateAddress(new AllocateAddressRequest());
            } catch (Exception e) {
                Status status = new Status(Status.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to create new Elastic IP: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }

            refreshAddressList();
        }
    }

}
