/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AccountInfo;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.core.ui.IRefreshable;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.eclipse.ec2.InstanceType;
import com.amazonaws.eclipse.ec2.InstanceTypes;
import com.amazonaws.eclipse.ec2.keypairs.KeyPairManager;
import com.amazonaws.eclipse.ec2.ui.SelectionTable;
import com.amazonaws.eclipse.ec2.ui.ebs.CreateNewVolumeDialog;
import com.amazonaws.eclipse.ec2.utils.DynamicMenuAction;
import com.amazonaws.eclipse.ec2.utils.IMenu;
import com.amazonaws.eclipse.ec2.utils.MenuAction;
import com.amazonaws.eclipse.ec2.utils.MenuHandler;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

/**
 * Table displaying EC2 instances and a context menu with actions like opening
 * remote shells, terminating instances, rebooting instances, attaching EBS
 * volumes, etc.
 */
public class InstanceSelectionTable extends SelectionTable implements IRefreshable, IMenu {

    /* Menu Actions */
    private Action refreshAction;
    private Action rebootAction;
    private Action terminateAction;
    private Action openShellAction;
    private Action openShellDialogAction;
    private Action createAmiAction;
    private Action copyPublicDnsNameAction;
    private Action attachNewVolumeAction;
    private Action startInstancesAction;
    private Action stopInstancesAction;

    /** Dropdown filter menu for Instance State */
    private IAction instanceStateFilterDropDownAction;

    /** DropDown menu handler for Instance State */
    private MenuHandler instanceStateDropDownMenuHandler;

    /** Dropdown filter menu for Security Group*/
    private IAction securityGroupFilterDropDownAction;

    /** DropDown menu handler for Security Group */
    private MenuHandler securityGroupDropDownMenuHandler;

    /** Holds the ALL option for Security Group Filter Item */
    private MenuItem allSecurityGroupFilterItem;

    /** The timer we use to have this table automatically refreshed */
    private RefreshTimer refreshInstanceListTimer;

    /** Shared account info */
    final static AccountInfo accountInfo = AwsToolkitCore.getDefault().getAccountInfo();

    /** Content and label provider for this selection table */
    ViewContentAndLabelProvider contentAndLabelProvider;

    private KeyPairManager keyPairManager = new KeyPairManager();


    /**
     * An optional field containing a list of Amazon EC2 instances to display in
     * this selection table
     */
    private List<String> instancesToDisplay;

    /** Stores the no of instances that are displayed */
    private int noOfInstances;

    /*
     * Public Interface
     */

    /**
     * Creates a new instance selection table within the specified composite.
     *
     * @param parent
     *            The parent composite for this new instance selection table.
     */
    public InstanceSelectionTable(Composite parent) {
        super(parent, true, false);

        contentAndLabelProvider = new ViewContentAndLabelProvider();
        viewer.setContentProvider(contentAndLabelProvider);
        viewer.setLabelProvider(contentAndLabelProvider);

        setComparator(new InstanceComparator(this, ViewContentAndLabelProvider.LAUNCH_TIME_COLUMN));

        refreshInstanceListTimer = new RefreshTimer(this);
        refreshInstanceListTimer.startTimer();
    }

    /**
     * Creates a new instance selection table within the specified composite,
     * displaying only the Amazon EC2 instance IDs specified.
     *
     * @param parent
     *            The parent composite for this new instance selection table.
     * @param instancesToDisplay
     *            A list of Amazon EC2 instance IDs to limit this selection
     *            table to showing.
     */
    public InstanceSelectionTable(Composite parent, List<String> instancesToDisplay) {
        this(parent);

        setInstancesToList(instancesToDisplay);
    }

    /**
     * Sets the period, in milliseconds, between automatic refreshes of the data
     * displayed in this instance selection table.
     *
     * @param refreshPeriodInMilliseconds
     *            The period, in milliseconds, between automatic refreshes of
     *            the data displayed in this table.
     */
    public void setRefreshPeriod(int refreshPeriodInMilliseconds) {
        refreshInstanceListTimer.setRefreshPeriod(refreshPeriodInMilliseconds);
    }

    /**
     * Refreshes the list of a user's current instances.
     */
    public void refreshInstances() {
        new RefreshInstancesThread().start();
    }

    /**
     * Returns the Action object that refreshes this selection table.
     *
     * @return The IAction object that refreshes this selection table.
     */
    public Action getRefreshAction() {
        return refreshAction;
    }

    /**
     * Returns the Action object that shows the Instance State filter dropdown menus
     *
     * @return The IAction object that shows the Instance State filter dropdown menus
     */
    public IAction getInstanceStateFilterDropDownAction() {
        return instanceStateFilterDropDownAction;
    }

    /**
     * Returns the Action object that shows the Security Group filter dropdown menus
     *
     * @return The Action object that shows the Security Group filter dropdown menus
     */
    public IAction getSecurityGroupFilterAction() {
        return securityGroupFilterDropDownAction;
    }
    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        refreshInstanceListTimer.stopTimer();

        super.dispose();
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.IRefreshable#refreshData()
     */
    @Override
    public void refreshData() {
        refreshInstances();
    }

    /**
     * Sets the Amazon EC2 instances which should be displayed in this selection
     * table. Specifying an empty list or a null list will both result in
     * displaying no instances in this list. The selection table will be
     * refreshed by this method so that the specified instances are being
     * displayed.
     *
     * @param serviceInstances
     *            A list of Amazon EC2 instance IDs to display in this selection
     *            table.
     */
    public void setInstancesToList(List<String> serviceInstances) {
        if (serviceInstances == null) {
            serviceInstances = new ArrayList<>();
        }
        this.instancesToDisplay = serviceInstances;
        this.refreshInstances();
    }


    /*
     * SelectionTable Interface
     */

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#createColumns()
     */
    @Override
    protected void createColumns() {
        newColumn("Instance ID", 10);
        newColumn("Instance Name", 10);
        newColumn("Public DNS Name", 15);
        newColumn("Image ID", 10);
        newColumn("Root Device Type", 10);
        newColumn("State", 10);
        newColumn("Type", 10);
        newColumn("Availability Zone", 10);
        newColumn("Key Pair", 10);
        newColumn("Launch Time", 15);
        newColumn("Security Groups", 15);
        newColumn("Tags", 15);
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    protected void fillContextMenu(IMenuManager manager) {
        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(rebootAction);
        manager.add(terminateAction);
        manager.add(startInstancesAction);
        manager.add(stopInstancesAction);
        manager.add(new Separator());
        manager.add(openShellAction);
        manager.add(openShellDialogAction);
        manager.add(new Separator());
        manager.add(createAmiAction);
        manager.add(new Separator());
        manager.add(copyPublicDnsNameAction);

        final boolean isRunningInstanceSelected = isRunningInstanceSelected();
        final boolean exactlyOneInstanceSelected =
            isRunningInstanceSelected && (getAllSelectedInstances().size() == 1);

        if (exactlyOneInstanceSelected) {
            manager.add(new Separator());
            manager.add(createEbsMenu());
        }

        rebootAction.setEnabled(isRunningInstanceSelected);
        terminateAction.setEnabled(isRunningInstanceSelected);

        boolean instanceLaunchedWithKey = getSelectedInstance().getKeyName() != null;
        boolean knownPrivateKey = keyPairManager.isKeyPairValid(AwsToolkitCore.getDefault().getCurrentAccountId(), getSelectedInstance().getKeyName());
        boolean canOpenShell = isRunningInstanceSelected && instanceLaunchedWithKey && knownPrivateKey;
        openShellAction.setEnabled(canOpenShell);
        openShellDialogAction.setEnabled(canOpenShell);

        copyPublicDnsNameAction.setEnabled(exactlyOneInstanceSelected);
        createAmiAction.setEnabled(exactlyOneInstanceSelected && instanceLaunchedWithKey);

        // These calls seem like a no-op, but it basically forces a refresh of the enablement state
        startInstancesAction.setEnabled(startInstancesAction.isEnabled());
        stopInstancesAction.setEnabled(stopInstancesAction.isEnabled());
    }

    /* (non-Javadoc)
     * @see com.amazonaws.eclipse.ec2.ui.SelectionTable#makeActions()
     */
    @Override
    protected void makeActions() {
        refreshAction = new Action("Refresh", Ec2Plugin.getDefault().getImageRegistry().getDescriptor("refresh")) {
            @Override
            public void run() {
                refreshInstances();
            }
            @Override
            public String getToolTipText() {
                return "Refresh instances";
            }
        };

        rebootAction = new RebootInstancesAction(this);
        terminateAction = new TerminateInstancesAction(this);
        openShellAction = new OpenShellAction(this);
        openShellDialogAction = new OpenShellDialogAction(this);
        createAmiAction = new CreateAmiAction(this);

        copyPublicDnsNameAction = new AwsAction(
                AwsToolkitMetricType.EXPLORER_EC2_COPY_PUBLIC_DNS_NAME_ACTION,
                "Copy Public DNS Name",
                Ec2Plugin.getDefault().getImageRegistry().getDescriptor("clipboard")) {
            @Override
            public void doRun() {
                copyPublicDnsNameToClipboard(getSelectedInstance());
                actionFinished();
            }
            @Override
            public String getToolTipText() {
                return "Copies this instance's public DNS name to the clipboard.";
            }
        };

        attachNewVolumeAction = new AwsAction(
                AwsToolkitMetricType.EXPLORER_EC2_ATTACH_NEW_VOLUME_ACTION,
                "Attach New Volume...") {
            @Override
            public void doRun() {
                Instance instance = getSelectedInstance();

                CreateNewVolumeDialog dialog = new CreateNewVolumeDialog(Display.getCurrent().getActiveShell(), instance);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    actionCanceled();
                } else {
                    new AttachNewVolumeThread(instance, dialog.getSize(), dialog.getSnapshotId(), dialog.getDevice()).start();
                    actionSucceeded();
                }
                actionFinished();
            }
            @Override
            public String getToolTipText() {
                return "Attaches a new Elastic Block Storage volume to this instance.";
            }
        };

        startInstancesAction = new StartInstancesAction(this);
        stopInstancesAction = new StopInstancesAction(this);

        instanceStateDropDownMenuHandler = new MenuHandler();
        instanceStateDropDownMenuHandler.addListener(this);
        instanceStateDropDownMenuHandler.add("ALL", "All Instances", true);
        for (InstanceType instanceType : InstanceTypes.getInstanceTypes()) {
            instanceStateDropDownMenuHandler.add(instanceType.id, instanceType.name + " Instances");
        }
        instanceStateDropDownMenuHandler.add("windows", "Windows Instances");
        instanceStateFilterDropDownAction = new MenuAction("Status Filter", "Filter by instance state", "filter", instanceStateDropDownMenuHandler);

        securityGroupDropDownMenuHandler = new MenuHandler();
        securityGroupDropDownMenuHandler.addListener(this);
        allSecurityGroupFilterItem = securityGroupDropDownMenuHandler.add("ALL", "All Security Groups", true);
        securityGroupFilterDropDownAction = new DynamicMenuAction("Security Groups Filter", "Filter by security group", "filter", securityGroupDropDownMenuHandler);
    }


    /*
     * Private Interface
     */



    protected void copyPublicDnsNameToClipboard(Instance instance) {
        Clipboard clipboard = new Clipboard(Display.getCurrent());

        String textData = instance.getPublicDnsName();
        TextTransfer textTransfer = TextTransfer.getInstance();
        Transfer[] transfers = new Transfer[]{textTransfer};
        Object[] data = new Object[]{textData};
        clipboard.setContents(data, transfers);
        clipboard.dispose();
    }

    /**
     * Returns true if and only if at least one instance is selected and all of
     * the selected instances are in a running state.
     *
     * @return True if and only if at least one instance is selected and all of
     *         the selected instances are in a running state, otherwise returns
     *         false.
     */
    private boolean isRunningInstanceSelected() {
        boolean instanceSelected = viewer.getTree().getSelectionCount() > 0;

        if (!instanceSelected) {
            return false;
        }

        for (TreeItem data : viewer.getTree().getSelection()) {
            Instance instance = (Instance)data.getData();

            if (!instance.getState().getName().equalsIgnoreCase("running")) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    List<Instance> getAllSelectedInstances() {
        StructuredSelection selection = (StructuredSelection)viewer.getSelection();
        return selection.toList();
    }

    private Instance getSelectedInstance() {
        StructuredSelection selection = (StructuredSelection)viewer.getSelection();
        return (Instance)selection.getFirstElement();
    }

    private IMenuManager createEbsMenu() {
        final MenuManager subMenu = new MenuManager("Elastic Block Storage");
        subMenu.add(attachNewVolumeAction);
        subMenu.add(new Separator());

        final Instance instance = getSelectedInstance();

        new PopulateEbsMenuThread(instance, subMenu).start();

        return subMenu;
    }

    /**
     * Sets the list of instances to be displayed in the instance table.
     *
     * @param instances
     *            The list of instances to be displayed in the instance table.
     * @param securityGroupMap
     *            A map of instance IDs to a list of security groups in which
     *            those instances were launched.
     */
    private void setInput(final List<Instance> instances, final Map<String, List<String>> securityGroupMap) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                /*
                 * Sometimes we see cases where the content provider for a table
                 * viewer is null, so we check for that here to avoid causing an
                 * unhandled event loop exception. All InstanceSelectionTables
                 * should always have a content provider set in the constructor,
                 * but for some reason we occasionally still see this happen.
                 */
                if (viewer.getContentProvider() == null) {
                    return;
                }

                StructuredSelection currentSelection = (StructuredSelection) viewer.getSelection();
                viewer.setInput(new InstancesViewInput(instances, securityGroupMap));

                packColumns();

                Set<String> instanceIds = new HashSet<>();
                for (Instance instance : (List<Instance>) currentSelection.toList()) {
                    instanceIds.add(instance.getInstanceId());
                }

                List<Instance> newSelectedInstances = new ArrayList<>();
                for (TreeItem treeItem : viewer.getTree().getItems()) {
                    Instance instance = (Instance) treeItem.getData();

                    if (instanceIds.contains(instance.getInstanceId())) {
                        newSelectedInstances.add(instance);
                    }
                }

                viewer.setSelection(new StructuredSelection(newSelectedInstances));
            }
        });
    }


    /*
     * Private Classes
     */

    /**
     * Thread for making a service call to EC2 to list current instances.
     */
    private class RefreshInstancesThread extends Thread {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            synchronized (RefreshInstancesThread.class) {
                if (selectionTableListener != null) {
                    selectionTableListener.loadingData();
                    enableDropDowns(false);
                }

                List<Reservation> reservations = null;
                try {
                    boolean needsToDescribeInstances = true;
                    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
                    if (instancesToDisplay != null) {
                        /*
                         * If the caller explicitly asked for a list of zero
                         * instances to be displayed, don't even bother querying for
                         * anything.
                         */
                        if (instancesToDisplay.size() == 0) {
                            needsToDescribeInstances = false;
                        };

                        describeInstancesRequest.setInstanceIds(instancesToDisplay);
                    }

                    final List<Instance> allInstances = new ArrayList<>();
                    final Map<String, List<String>> securityGroupsByInstanceId = new HashMap<>();

                    if (needsToDescribeInstances) {
                        DescribeInstancesResult response = getAwsEc2Client().describeInstances(describeInstancesRequest);
                        reservations = response.getReservations();

                        noOfInstances = -1;    //Reset the value

                        Set<String> allSecurityGroups = new TreeSet<>();

                        for (Reservation reservation : reservations) {
                            List<Instance> instances = reservation.getInstances();

                            List<String> groupNames = reservation.getGroupNames();
                            Collections.sort(groupNames);
                            allSecurityGroups.addAll(groupNames);

                            //Filter Security Groups
                            if (securityGroupDropDownMenuHandler.getCurrentSelection().getMenuId().equals("ALL")  || groupNames.contains(securityGroupDropDownMenuHandler.getCurrentSelection().getMenuId())) {

                                for (Instance instance : instances) {
                                    //Filter Instances
                                    if (!instanceStateDropDownMenuHandler.getCurrentSelection().getMenuId().equals("ALL")) {
                                        if (instanceStateDropDownMenuHandler.getCurrentSelection().getMenuId().equalsIgnoreCase("windows")) {
                                            if (instance.getPlatform() == null || !instance.getPlatform().equals("windows"))
                                                continue;
                                        } else if (!instance.getInstanceType().equalsIgnoreCase(instanceStateDropDownMenuHandler.getCurrentSelection().getMenuId())) {
                                            continue;
                                        }
                                    }

                                    allInstances.add(instance);

                                    // Populate the map of instance IDs -> security groups
                                    securityGroupsByInstanceId.put(instance.getInstanceId(), groupNames);
                                }
                            }
                        }

                        //Populate all Security Groups dynamically
                        securityGroupDropDownMenuHandler.clear();
                        securityGroupDropDownMenuHandler.add(allSecurityGroupFilterItem);
                        for(String securityGroup : allSecurityGroups) {
                            securityGroupDropDownMenuHandler.add(new MenuItem(securityGroup, securityGroup));
                        }
                    }

                    noOfInstances = allInstances.size();
                    setInput(allInstances, securityGroupsByInstanceId);
                } catch (Exception e) {
                    // Only log an error if the account info is valid and we
                    // actually expected this call to work
                    if (AwsToolkitCore.getDefault().getAccountInfo().isValid()) {
                        Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID,
                                "Unable to list instances: " + e.getMessage(), e);
                        StatusManager.getManager().handle(status, StatusManager.LOG);
                    }
                } finally {
                    if (selectionTableListener != null) {
                        selectionTableListener.finishedLoadingData(noOfInstances);
                        enableDropDowns(true);
                    }
                }
            }
        }
    }

    /**
     * Callback function. Is called from the DropdownMenuHandler when a menu option is clicked
     *
     *  @param itemSelected The selected MenuItem
     *
     *  @see com.amazonaws.eclipse.ec2.utils.IMenu#menuClicked(com.amazonaws.eclipse.ec2.utils.IMenu.MenuItem)
     */
    @Override
    public void menuClicked(MenuItem menuItemSelected) {
        refreshData();
    }

    /**
     * Enables/Disables dropdown filters
     *
     * @param checked A TRUE value will imply the dropdown filter is enabled; FALSE value will make it disabled
     */
    private void enableDropDowns(boolean checked) {
        instanceStateFilterDropDownAction.setEnabled(checked);
        securityGroupFilterDropDownAction.setEnabled(checked);
    }
}
