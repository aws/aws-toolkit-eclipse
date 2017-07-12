package com.amazonaws.eclipse.codedeploy.deploy.progress;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.eclipse.codedeploy.ServiceAPIUtils;
import com.amazonaws.eclipse.codedeploy.explorer.image.CodeDeployExplorerImages;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.DeploymentStatus;
import com.amazonaws.services.codedeploy.model.GetDeploymentInstanceRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
import com.amazonaws.services.codedeploy.model.InstanceStatus;
import com.amazonaws.services.codedeploy.model.InstanceSummary;
import com.amazonaws.services.codedeploy.model.LifecycleEvent;
import com.amazonaws.services.codedeploy.model.LifecycleEventStatus;

public class DeploymentProgressTrackerDialog extends Dialog {

    private final String deploymentId;
    private final String deploymentGroupName;
    private final String applicationName;
    private final AmazonCodeDeploy client;

    /**
     * Used as the direct input for the instance table view
     */
    private InstanceSummary[] instanceSummaries;

    /*
     * UI widgets
     */
    private Label titleLabel;
    private Text latestEventMessageText;
    private ProgressIndicator progressIndicator;
    private TableViewer instancesTableViewer;
    private Label viewDiagnosticLabel;

    private static final String[] EVENTS = new String[] {
        "ApplicationStop",
        "DownloadBundle",
        "BeforeInstall",
        "Install",
        "AfterInstall",
        "ApplicationStart",
        "ValidateService"
    };

    private static final int INSTANCE_ID_COL_WIDTH = 150;
    private static final int EVENT_COL_WIDTH = 120;
    private static final int INSTANCE_TABLE_VIEWER_HEIGHT_HINT = 200;

    private static final int REFRESH_INTERVAL_MS = 5 * 1000;

    public DeploymentProgressTrackerDialog(Shell parentShell,
            String deploymentId, String deploymentGroupName,
            String applicationName, Region region) {
        super(parentShell);

        this.deploymentId = deploymentId;
        this.deploymentGroupName = deploymentGroupName;
        this.applicationName = applicationName;

        String endpoint = region.getServiceEndpoints()
                .get(ServiceAbbreviations.CODE_DEPLOY);
        this.client = AwsToolkitCore.getClientFactory()
                .getCodeDeployClientByEndpoint(endpoint);
    }

    /**
     * To customize the dialog title
     */
    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText(String.format(
              "Deploying to %s [%s]", applicationName, deploymentGroupName));
    }

    /**
     * To customize the dialog button
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // only create the close button
        createButton(parent, IDialogConstants.CANCEL_ID,
                "Close", false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 15;
        layout.marginHeight = 15;
        container.setLayout(layout);

        titleLabel = new Label(container, SWT.NONE);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleLabel.setText("Waiting deployment to complete...");
        titleLabel.setFont(JFaceResources.getBannerFont());

        Group deploymentInfoGroup = new Group(container, SWT.NONE);
        deploymentInfoGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        deploymentInfoGroup.setLayout(new GridLayout(1, false));

        GridData labelGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        Label label_Application = new Label(deploymentInfoGroup, SWT.NONE);
        label_Application.setText("Application Name: " + applicationName);
        label_Application.setLayoutData(labelGridData);
        Label label_DeploymentGroup = new Label(deploymentInfoGroup, SWT.NONE);
        label_DeploymentGroup.setText("Deployment Group Name: " + deploymentGroupName);
        label_DeploymentGroup.setLayoutData(labelGridData);
        Label label_DeploymentId = new Label(deploymentInfoGroup, SWT.NONE);
        label_DeploymentId.setText("Deployment ID: " + deploymentId);
        label_DeploymentId.setLayoutData(labelGridData);

        progressIndicator = new ProgressIndicator(container);
        progressIndicator.beginAnimatedTask();
        progressIndicator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        latestEventMessageText = new Text(container, SWT.NONE | SWT.WRAP | SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.heightHint = 2 * latestEventMessageText.getLineHeight();
        latestEventMessageText.setLayoutData(gridData);
        latestEventMessageText.setEnabled(false); // gray out the background
        setItalicFont(latestEventMessageText);

        createDeploymentInstancesTable(container);

        loadAndStartUpdatingDeploymentInstancesAsync();

        return container;
    }

    private void updateTitleLabel(final String message) {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                if (!titleLabel.isDisposed()) {
                    titleLabel.setText(message);
                }
            }
        });
    }

    private void updateLatestEventLabel(final String message) {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                if (!latestEventMessageText.isDisposed()) {
                    latestEventMessageText.setText(message);
                }
            }
        });
    }

    /**
     * @return true if the deployment has transitioned into the given status.
     */
    private boolean waitTillDeploymentReachState(DeploymentStatus expectedState) {
        try {
            while ( this.getContents() != null
                    && !this.getContents().isDisposed() ) {

                boolean isFinalStatus = false;

                DeploymentInfo deploymentInfo = client
                        .getDeployment(new GetDeploymentRequest()
                                .withDeploymentId(deploymentId))
                        .getDeploymentInfo();
                String deploymentStatus = deploymentInfo.getStatus();

                if (DeploymentStatus.Created.toString().equals(deploymentStatus)
                        || DeploymentStatus.Queued.toString().equals(deploymentStatus)
                        || DeploymentStatus.InProgress.toString().equals(deploymentStatus)) {
                    updateLatestEventLabel("Deployment status - " + deploymentStatus);
                }

                if (DeploymentStatus.Succeeded.toString().equals(deploymentStatus)
                        || DeploymentStatus.Stopped.toString().equals(deploymentStatus)
                        || DeploymentStatus.Failed.toString().equals(deploymentStatus)) {
                    isFinalStatus = true;

                    StringBuilder sb = new StringBuilder("Deployment " + deploymentStatus);
                    if (deploymentInfo.getErrorInformation() != null) {
                        sb.append(String.format(
                                " (Error Code: %s, Error Message: %s)",
                                deploymentInfo.getErrorInformation().getCode(),
                                deploymentInfo.getErrorInformation().getMessage()));
                    }
                    updateLatestEventLabel(sb.toString());

                    if (DeploymentStatus.Succeeded.toString().equals(deploymentStatus)) {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                progressIndicator.done();
                                updateTitleLabel("Deployment complete");
                            }
                        });
                    } else {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                progressIndicator.showError();
                                updateTitleLabel("Deployment didn't finish successfully.");
                            }
                        });
                    }
                }

                // return true if the expected status
                if (expectedState.toString().equals(deploymentStatus)) {
                    return true;

                } else if (isFinalStatus) {
                    CodeDeployPlugin.getDefault()
                        .logInfo("Deployment reached final status: " + deploymentStatus);
                    return false;
                }

                // Otherwise keep polling

                try {
                    Thread.sleep(REFRESH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    CodeDeployPlugin.getDefault()
                        .logInfo("Interrupted when polling deployment status");
                    return false;
                }
            }

        } catch (Exception e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Error when polling deployment status.", e);
        }

        return false;
    }

    private void loadAndStartUpdatingDeploymentInstancesAsync() {
        new Thread(new Runnable() {

            @Override
            public void run() {

                // A short pause before requesting deployment status
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }

                // Start to load deployment status, and Wait till the deployment
                // is "InProgress"
                boolean pollInstances = waitTillDeploymentReachState(
                        DeploymentStatus.InProgress);

                if (!pollInstances) {
                    updateTitleLabel("Deployment didn't finish successfully.");
                    return;
                }

                updateLatestEventLabel("Loading all the deployment instances...");

                List<InstanceSummary> instances = ServiceAPIUtils
                        .getAllDeploymentInstances(client, deploymentId);
                CodeDeployPlugin.getDefault().logInfo(
                        instances.size() + " instances are being deployed.");

                synchronized (DeploymentProgressTrackerDialog.this) {
                    instanceSummaries = instances.toArray(
                            new InstanceSummary[instances.size()]);

                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            instancesTableViewer.setInput(instanceSummaries);
                            instancesTableViewer.refresh();

                            if ( !viewDiagnosticLabel.isDisposed() ) {
                                viewDiagnosticLabel.setVisible(true);
                            }
                        }
                    });
                }

                updateLatestEventLabel("Updating deployment lifecycle events...");

                updateInstanceLifecycleEvents();

                updateLatestEventLabel("All instances have reached final status... " +
                        "Waiting for the deployment to finish...");

                waitTillDeploymentReachState(DeploymentStatus.Succeeded);

            }
        }).start();
    }

    private void updateInstanceLifecycleEvents() {
        try {
            while ( this.getContents() != null
                    && !this.getContents().isDisposed() ) {

                int pendingInstances = 0;

                synchronized (DeploymentProgressTrackerDialog.this) {
                    if (instanceSummaries == null) {
                        continue;
                    }

                    for (int i = 0; i < instanceSummaries.length; i++) {
                        InstanceSummary instance = instanceSummaries[i];

                        if (InstanceStatus.InProgress.toString().equals(instance.getStatus())
                                || InstanceStatus.Pending.toString().equals(instance.getStatus())) {
                            InstanceSummary latestSummary = client.getDeploymentInstance(
                                    new GetDeploymentInstanceRequest()
                                            .withDeploymentId(deploymentId)
                                            .withInstanceId(instance.getInstanceId()))
                                    .getInstanceSummary();
                            instanceSummaries[i] = latestSummary;
                            pendingInstances++;
                        }
                    }
                }

                updateLatestEventLabel(String.format(
                        "Waiting for %d instances to complete...(%d done)",
                        pendingInstances, instanceSummaries.length - pendingInstances));

                if (pendingInstances > 0) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            if ( !instancesTableViewer.getTable().isDisposed() ) {
                                instancesTableViewer.refresh();
                            }
                        }
                    });
                } else {
                    // All instances have reached the final states
                    return;
                }

                try {
                    Thread.sleep(REFRESH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted when polling " +
                            "lifecycle events from deployment instances.");
                }
            }

        } catch (Exception e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Error when polling lifecycle events from deployment instances.", e);
        }

    }

    private void createDeploymentInstancesTable(Composite container) {
        instancesTableViewer = new TableViewer(container,
                SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGridData.heightHint = INSTANCE_TABLE_VIEWER_HEIGHT_HINT;
        instancesTableViewer.getTable().setLayoutData(tableGridData);

        final Table table = instancesTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableViewerColumn instanceIdColumn = new TableViewerColumn(instancesTableViewer, SWT.CENTER);
        instanceIdColumn.getColumn().setWidth(INSTANCE_ID_COL_WIDTH);
        instanceIdColumn.getColumn().setText("Instance ID");
        instanceIdColumn.setLabelProvider(new ColumnLabelProvider() {
              @Override
              public String getText(Object element) {
                  InstanceSummary instanceSummary = (InstanceSummary)element;
                  // The service returns the full ARN of the instance
                  String instanceArn = instanceSummary.getInstanceId();
                  int lastSlashIndex = instanceArn.lastIndexOf("/");
                  String instanceId = lastSlashIndex == -1 ?
                          instanceArn : instanceArn.substring(lastSlashIndex + 1);
                  return instanceId;
              }

              @Override
              public Font getFont(Object element) {
                  // bold font
                  return JFaceResources.getBannerFont();
              }
        });

        for (String eventName : EVENTS) {
            TableViewerColumn eventColumn = new TableViewerColumn(instancesTableViewer, SWT.CENTER);
            eventColumn.getColumn().setWidth(EVENT_COL_WIDTH);
            eventColumn.getColumn().setText(eventName);

            final String eventName_local = eventName;
            eventColumn.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    InstanceSummary instanceSummary = (InstanceSummary)element;
                    LifecycleEvent event = ServiceAPIUtils.findLifecycleEventByEventName(
                            instanceSummary, eventName_local);
                    return event.getStatus();
                }

                @Override
                public Image getImage(Object element) {
                    InstanceSummary instanceSummary = (InstanceSummary)element;
                    LifecycleEvent event = ServiceAPIUtils.findLifecycleEventByEventName(
                            instanceSummary, eventName_local);
                    String statusString = event.getStatus();

                    if (LifecycleEventStatus.Succeeded.toString().equals(statusString)) {
                        return CodeDeployPlugin.getDefault().getImageRegistry()
                                .get(CodeDeployExplorerImages.IMG_CHECK_ICON);
                    }
                    return null;
                }

                @Override
                public Color getForeground(Object element) {
                    InstanceSummary instanceSummary = (InstanceSummary)element;
                    LifecycleEvent event = ServiceAPIUtils.findLifecycleEventByEventName(
                            instanceSummary, eventName_local);
                    String statusString = event.getStatus();

                    if (LifecycleEventStatus.Pending.toString().equals(statusString)
                            || LifecycleEventStatus.Unknown.toString().equals(statusString)) {
                        return Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);
                    }
                    if (LifecycleEventStatus.InProgress.toString().equals(statusString)) {
                        return Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
                    }
                    if (LifecycleEventStatus.Failed.toString().equals(statusString)) {
                        return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
                    }
                    return null;
                }

            });
        }

        instancesTableViewer.setContentProvider(ArrayContentProvider.getInstance());

        instancesTableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                StructuredSelection selection = (StructuredSelection) event.getSelection();
                Object element = selection.getFirstElement();

                if (element instanceof InstanceSummary) {
                    new InstanceSummaryDetailDialog(
                            getShell(), (InstanceSummary)element)
                    .open();
                }
            }
        });

        viewDiagnosticLabel = new Label(container, SWT.NONE);
        viewDiagnosticLabel.setText("Double-click instance-ID to view the detailed diagnostic information");
        setItalicFont(viewDiagnosticLabel);
        viewDiagnosticLabel.setVisible(false);
    }

    private Font italicFont;

    private void setItalicFont(Control control) {
        FontData[] fontData = control.getFont()
                .getFontData();
        for (FontData fd : fontData) {
            fd.setStyle(SWT.ITALIC);
        }
        italicFont = new Font(Display.getDefault(), fontData);
        control.setFont(italicFont);
    }

    @Override
    public boolean close() {
        if (italicFont != null)
            italicFont.dispose();
        return super.close();
    }

}
