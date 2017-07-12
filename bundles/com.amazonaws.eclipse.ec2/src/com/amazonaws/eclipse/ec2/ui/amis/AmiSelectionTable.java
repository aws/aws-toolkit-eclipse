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

package com.amazonaws.eclipse.ec2.ui.amis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.TagFormatter;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.ec2.ui.launchwizard.LaunchWizard;
import com.amazonaws.eclipse.ec2.utils.IMenu;
import com.amazonaws.eclipse.ec2.utils.MenuAction;
import com.amazonaws.eclipse.ec2.utils.MenuHandler;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;

/**
 * Selection table for AMIs.
 */
public class AmiSelectionTable extends SelectionTable implements IMenu {

    /** Dropdown filter menu for AMIs */
    private IAction amiFilterDropDownAction;

    /** DropDown menu handler for AMI Filter Items*/
    private MenuHandler amiDropDownMenuHandler;

    /** Dropdown filter menu for Platforms */
    private IAction platformFilterDropDownAction;

    /** DropDown menu handler for Platform Filter Items*/
    private MenuHandler platformDropDownMenuHandler;

    /** Holds the number of AMIs currently display */
    private int noOfAMIs;

    /** An action object to launch instances of the selected AMI */
    private Action launchAction;

    /** An action object to refresh the AMI list */
    private Action refreshAction;

    /** An action object to delete a selected AMI */
    private Action deleteAmiAction;

    /** The user's account info */
    private static AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountInfo();

    /** The content provider for the data displayed in this selection table */
    private ViewContentProvider contentProvider = new ViewContentProvider();

    private LoadImageDescriptionsThread loadImageThread;

    /* Column identifiers */
    private static final int IMAGE_ID_COLUMN = 0;
    private static final int IMAGE_MANIFEST_COLUMN = 1;
    private static final int IMAGE_STATE_COLUMN = 2;
    private static final int IMAGE_OWNER_COLUMN = 3;
    private static final int IMAGE_TAGS_COLUMN = 4;

    /**
     * Creates a new AMI selection table with the specified parent.
     *
     * @param parent
     *            The parent of this new selection table.
     * @param listener
     *            The selection table listener object that should be notified
     *            when this selection table loads data.
     */
    public AmiSelectionTable(Composite parent, SelectionTableListener listener) {
        super(parent, false, true);

        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new ViewLabelProvider());

        this.setListener(listener);

        createToolbarActions();

        refreshAmis();

        viewer.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                Image image = getSelectedImage();
                if (image == null) return;

                launchAction.run();
            }
        });
    }

    /**
     * Refreshes the set of Amis asynchronously.
     */
    private void refreshAmis() {
        cancelLoadAmisThread();
        loadImageThread = new LoadImageDescriptionsThread();
        loadImageThread.start();
    }

    private void cancelLoadAmisThread() {
        if ( loadImageThread != null ) {
            synchronized (loadImageThread) {
                if ( !loadImageThread.canceled ) {
                    loadImageThread.cancel();
                }
            }
        }
    }

        @Override
    public void dispose() {
        cancelLoadAmisThread();
        super.dispose();
    }

    private void createToolbarActions() {
        amiDropDownMenuHandler = new MenuHandler();
        amiDropDownMenuHandler.addListener(this);
        amiDropDownMenuHandler.add("ALL", "All Images");
        amiDropDownMenuHandler.add("amazon", "Amazon Images", true);
        amiDropDownMenuHandler.add("Public", "Public Images");
        amiDropDownMenuHandler.add("Private", "Private Images");
        amiDropDownMenuHandler.add("ByMe", "Owned By Me");
        amiDropDownMenuHandler.add("32-bit", "32-bit");
        amiDropDownMenuHandler.add("64-bit", "64-bit");
        amiFilterDropDownAction = new MenuAction("AMI Filter", "Filter AMIs", "filter", amiDropDownMenuHandler);

        platformDropDownMenuHandler = new MenuHandler();
        platformDropDownMenuHandler.addListener(this);
        platformDropDownMenuHandler.add("ALL", "All Platforms", true);
        platformDropDownMenuHandler.add("windows", "Windows");
        platformFilterDropDownAction = new MenuAction("Platform Filter", "Filter by platform", "filter", platformDropDownMenuHandler);

        refreshAction = new Action() {
            @Override
            public void run() {
                refreshAmis();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setDescription("Refresh the list of images");
        refreshAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh"));
    }

    /**
     * Returns an action object that refreshes the AMI selection table.
     *
     * @return An action object that refreshes the AMI selection table.
     */
    public Action getRefreshAction() {
        return refreshAction;
    }

    /**
     * Returns the Action object that shows the AMI filter dropdown menus
     *
     * @return The IAction object that shows the AMI filter dropdown menus
     */
    public IAction getAmiFilterDropDownAction() {
        return amiFilterDropDownAction;
    }

    /**
     * Returns the Action object that shows the Platform filter dropdown menus
     *
     * @return The IAction object that shows the Platform filter dropdown menus
     */
    public IAction getPlatformFilterDropDownAction() {
        return platformFilterDropDownAction;
    }

    /**
     * Filters the AMI list to those matching the specified string.
     *
     * @param searchText The text on which to filter.
     */
    public void filterImages(String searchText) {
        contentProvider.setFilter(searchText);
        viewer.refresh();
    }

    /**
     * Returns the selected AMI.
     *
     * @return The currently selected AMI, or null if none is selected.
     */
    public Image getSelectedImage() {
        return (Image)getSelection();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("AMI ID", 10);
        newColumn("Manifest", 20);
        newColumn("State", 10);
        newColumn("Owner", 10);
        newColumn("Tags", 15);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        Image selectedImage = getSelectedImage();

        launchAction.setEnabled(selectedImage != null);
        deleteAmiAction.setEnabled(doesUserHavePermissionToDelete(selectedImage));

        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(launchAction);
        manager.add(new Separator());
        manager.add(deleteAmiAction);
    }

    private boolean doesUserHavePermissionToDelete(Image ami) {
        String userId = accountInfo.getUserId();
        if (ami == null) return false;
        if (userId == null) return false;
        return ami.getOwnerId().equals(userId);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        launchAction = new Action() {
            @Override
            public void run() {
                Image image = getSelectedImage();
                new WizardDialog(Display.getCurrent().getActiveShell(), new LaunchWizard(image)).open();
            }
        };
        launchAction.setText("Launch...");
        launchAction.setToolTipText("Launch this image");
        launchAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("launch"));

        deleteAmiAction = new Action() {
            @Override
            public void run() {
                MessageBox messageBox = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);

                messageBox.setText("Delete selected AMI?");
                messageBox.setMessage("If you continue, you won't be able to use this AMI anymore.");

                // Bail out if the user cancels...
                if (messageBox.open() == SWT.CANCEL) return;

                final Image image = getSelectedImage();
                new DeleteAmiThread(image).start();
            }
        };
        deleteAmiAction.setText("Delete AMI");
        deleteAmiAction.setToolTipText("Delete the selected AMI");
        deleteAmiAction.setImageDescriptor(Ec2Plugin.getDefault().getImageRegistry().getDescriptor("remove"));
    }

    private class ViewContentProvider implements ILazyTreeContentProvider {
        private List<Image> unfilteredImages;
        private List<Image> filteredImages;

        private String filter;

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            filterImages();
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        @Override
        public void dispose() {}

        protected void applyFilters() {
            filterImages();
        }

        private void filterImages() {
            noOfAMIs = 0;   //Resets no of AMIs
            synchronized (this) {
                if (unfilteredImages == null) unfilteredImages = new ArrayList<>();
            }

            // We filter based on Text filter and Drop down Filters
            String[] searchTerms = (filter == null ? null : filter.split(" "));

            List<Image> tempFilteredImages = new ArrayList<>(unfilteredImages.size());
            for (Image image : unfilteredImages) {

                boolean containsAllTerms = true;
                if(searchTerms != null) {
                    for (String searchTerm : searchTerms) {
                        String imageDescription = image.getImageId() + " " + image.getImageLocation() + image.getOwnerId() + image.getState();
                        if (!imageDescription.toLowerCase().contains(searchTerm.toLowerCase())) {
                            containsAllTerms = false;
                        }
                    }
                }

                if (containsAllTerms) {
                    tempFilteredImages.add(image);
                }
            }

            filteredImages = tempFilteredImages;

            noOfAMIs = filteredImages.size();
            viewer.getTree().setItemCount(filteredImages.size());

            if (selectionTableListener != null) selectionTableListener.finishedLoadingData(noOfAMIs);
        }

        /**
         * Sets the filter used to control what content is returned.
         *
         * @param filter The filter to be applied when returning content.
         */
        public void setFilter(String filter) {
            this.filter = filter;
            applyFilters();
        }

        @Override
        public void updateChildCount(Object element, int currentChildCount) {
            if (element instanceof Image){
                viewer.setChildCount(element, 0);
            } else {
                viewer.setChildCount(element, filteredImages.size());
            }
        }

        @Override
        public void updateElement(Object parent, int index) {
            Object element = filteredImages.get(index);
            viewer.replace(parent, index, element);
            updateChildCount(element, -1);
        }

        public void setUnfilteredImages(List<Image> unfilteredImages) {
            this.unfilteredImages = unfilteredImages;
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }
    }

    private class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText(Object obj, int index) {
            if (obj == null) {
                return "??";
            }

            Image image = (Image)obj;
            switch (index) {
            case IMAGE_ID_COLUMN:
                return image.getImageId();
            case IMAGE_MANIFEST_COLUMN:
                return image.getImageLocation();
            case IMAGE_STATE_COLUMN:
                return image.getState();
            case IMAGE_OWNER_COLUMN:
                return image.getOwnerId();
            case IMAGE_TAGS_COLUMN:
                return TagFormatter.formatTags(image.getTags());
            }

            return "???";
        }

        @Override
        public org.eclipse.swt.graphics.Image getColumnImage(Object obj, int index) {
            if (index == 0)
                return Ec2Plugin.getDefault().getImageRegistry().get("ami");
            return null;
        }
        @Override
        public org.eclipse.swt.graphics.Image getImage(Object obj) {
            return null;
        }
    }


    /*
     * Private Thread subclasses for making EC2 service calls.
     */

    /**
     * Thread subclass for making an EC2 service call to delete an AMI.
     */
    private class DeleteAmiThread extends Thread {
        /** The AMI to delete */
        private final Image image;

        /**
         * Creates a new thread ready to be started to delete the specified AMI.
         *
         * @param image
         *            The AMI to delete.
         */
        public DeleteAmiThread(Image image) {
            this.image = image;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                DeregisterImageRequest request = new DeregisterImageRequest();
                request.setImageId(image.getImageId());
                getAwsEc2Client().deregisterImage(request);

                refreshAmis();
            } catch (Exception e) {
                Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                        "Unable to delete AMI: " + e.getMessage(), e);
                StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
            }
        }
    }

    /**
     * Thread subclass for making EC2 service calls to load a list of AMIs.
     */
    private class LoadImageDescriptionsThread extends Thread {

        private boolean canceled = false;

        private synchronized void cancel() {
            canceled = true;
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            enableActions(false);
            if (selectionTableListener != null) selectionTableListener.loadingData();

            try {

                final List<Image> images = getImages();

                synchronized (this) {
                    if ( !canceled ) {
                        noOfAMIs = images.size();
                        Display.getDefault().syncExec(new Runnable() {

                            @Override
                            public void run() {
                                if ( viewer != null ) {
                                    // There appears to be a bug in SWT virtual
                                    // trees (at least on some platforms) that
                                    // can lead to a stack overflow when trying
                                    // to preserve selection on an input change.
                                    viewer.getTree().deselectAll();
                                    contentProvider.setUnfilteredImages(images);
                                    viewer.setInput(images);
                                    contentProvider.applyFilters();
                                }
                            }
                        });
                        if (selectionTableListener != null) selectionTableListener.finishedLoadingData(noOfAMIs);
                        enableActions(true);
                    }
                }

            } catch (Exception e) {
                // Only log an error if the account info is valid and we
                // actually expected this call to work
                if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                    Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                            "Unable to query list of AMIs: " + e.getMessage(), e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }

                if (selectionTableListener != null) selectionTableListener.finishedLoadingData(noOfAMIs);
                enableActions(true);
            }
        }

        /**
         * Gets a list of images, filtering them according to the current filter
         * control settings.
         */
        public List<Image> getImages() {

            DescribeImagesRequest request = new DescribeImagesRequest().withFilters(new LinkedList<Filter>());

            request.getFilters().add(new Filter().withName("image-type").withValues("machine"));

            String menuId = amiDropDownMenuHandler.getCurrentSelection().getMenuId();
            if (!menuId.equals("ALL")) {
                if (menuId.equals("amazon")) {
                    List<String> owners = new LinkedList<>();
                    owners.add("amazon");
                    request.setOwners(owners);
                } else if (menuId.equals("Public")) {
                    request.getFilters().add(new Filter().withName("is-public").withValues("true"));
                } else if (menuId.equals("Private")) {
                    request.getFilters().add(new Filter().withName("is-public").withValues("false"));
                } else if (menuId.equals("ByMe")) {
                    List<String> owners = new LinkedList<>();
                    owners.add("self");
                    request.setOwners(owners);
                } else if (menuId.equals("32-bit")) {
                    request.getFilters().add(new Filter().withName("architecture").withValues("i386"));
                } else if (menuId.equals("64-bit")) {
                    request.getFilters().add(new Filter().withName("architecture").withValues("x86_64"));
                }
            }

            if (!platformDropDownMenuHandler.getCurrentSelection().getMenuId().equals("ALL")) {
                if (platformDropDownMenuHandler.getCurrentSelection().getMenuId().equals("windows")) {
                    request.getFilters().add(new Filter().withName("platform").withValues("windows"));
                }
            }

            return getAwsEc2Client().describeImages(request).getImages();
        }
    }

    /**
     * Callback function. Is called from the DropdownMenuHandler when a menu
     * option is clicked
     *
     * @see com.amazonaws.eclipse.ec2.utils.IMenu#menuClicked(com.amazonaws.eclipse.ec2.utils.IMenu.MenuItem)
     */
    @Override
    public void menuClicked(MenuItem itemSelected) {
        refreshAmis();
    }

    /**
     * Enables/Disables dropdown filters
     */
    private void enableActions(boolean enabled) {
        refreshAction.setEnabled(enabled);
        amiFilterDropDownAction.setEnabled(enabled);
        platformFilterDropDownAction.setEnabled(enabled);
    }
}
