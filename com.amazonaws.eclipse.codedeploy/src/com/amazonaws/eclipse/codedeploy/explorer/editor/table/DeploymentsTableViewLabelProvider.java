package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.DEPLOYMENT_ID_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.END_TIME_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.INSTANCE_ID_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.LIFECYCLE_EVENT_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.LOGS_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.REVISION_LOCATION_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.START_TIME_COL;
import static com.amazonaws.eclipse.codedeploy.explorer.editor.table.DeploymentsTableView.STATUS_COL;

import java.util.Date;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.amazonaws.eclipse.codedeploy.CodeDeployPlugin;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.DeploymentOverview;
import com.amazonaws.services.codedeploy.model.ErrorInformation;
import com.amazonaws.services.codedeploy.model.InstanceSummary;
import com.amazonaws.services.codedeploy.model.LifecycleEvent;
import com.amazonaws.services.codedeploy.model.RevisionLocation;

class DeploymentsTableViewLabelProvider extends StyledCellLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        String text = getColumnText(cell.getElement(), cell.getColumnIndex());
        cell.setText(text);

        if (cell.getColumnIndex() == STATUS_COL) {
            if ("Succeeded".equals(text)) {
                cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
            } else if ("Failed".equals(text)) {
                cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
            } else if (cell.getElement() != LoadingContentProvider.LOADING ){
                cell.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
            }
        }

        super.update(cell); // calls 'repaint' to trigger the paint listener
    }

    private String getColumnText(Object element, int columnIndex) {
        if ( element == LoadingContentProvider.LOADING ) {
            return "Loading...";
        }

        try {
            if (element instanceof DeploymentInfo) {
                DeploymentInfo deployment = (DeploymentInfo) element;

                switch (columnIndex) {
                case DEPLOYMENT_ID_COL:
                    return deployment.getDeploymentId();
                case INSTANCE_ID_COL:
                    return getInstancesOverviewString(deployment);
                case START_TIME_COL:
                    return getDateString(deployment.getCreateTime());
                case END_TIME_COL:
                    return getDateString(deployment.getCompleteTime());
                case STATUS_COL:
                    return deployment.getStatus();
                case REVISION_LOCATION_COL:
                    RevisionLocation revision = deployment.getRevision();
                    if ("S3".equals(revision.getRevisionType())) {
                        return String.format("s3://%s/%s",
                                revision.getS3Location().getBucket(),
                                revision.getS3Location().getKey());
                    } else if ("GitHub".equals(revision.getRevisionType())) {
                        return String.format("github://%s/%s",
                                revision.getGitHubLocation().getRepository(),
                                revision.getGitHubLocation().getCommitId());
                    }
                    return "";
                case LOGS_COL:
                    ErrorInformation error = deployment.getErrorInformation();
                    return error == null
                            ? ""
                            : String.format("[%s] %s", error.getCode(), error.getMessage());
                }
            }

            if (element instanceof InstanceSummary) {
                InstanceSummary instance = (InstanceSummary) element;

                switch (columnIndex) {
                case INSTANCE_ID_COL:
                    return extractInstanceId(instance.getInstanceId());
                case START_TIME_COL:
                    if (instance.getLifecycleEvents() != null
                            && !instance.getLifecycleEvents().isEmpty()) {
                        return getDateString(instance.getLifecycleEvents().get(0).getStartTime());
                    } else {
                        return "";
                    }
                case END_TIME_COL:
                    if (instance.getLifecycleEvents() != null
                            && !instance.getLifecycleEvents().isEmpty()) {
                        return getDateString(instance.getLifecycleEvents()
                                .get(instance.getLifecycleEvents().size() - 1)
                                .getEndTime());
                    } else {
                        return "";
                    }
                case STATUS_COL:
                    return instance.getStatus();
                }
            }

            if (element instanceof LifecycleEvent) {
                LifecycleEvent event = (LifecycleEvent) element;

                switch (columnIndex) {
                case LIFECYCLE_EVENT_COL:
                    return event.getLifecycleEventName();
                case START_TIME_COL:
                    return getDateString(event.getStartTime());
                case END_TIME_COL:
                    return getDateString(event.getEndTime());
                case STATUS_COL:
                    return event.getStatus();
                case LOGS_COL:
                    return event.getDiagnostics() == null ?
                            "" : event.getDiagnostics().getErrorCode();
                }
            }

        } catch (Exception e) {
            CodeDeployPlugin.getDefault().reportException(
                    "Unable to display " + element.getClass().getName() +
                    " in the Deployment Group Editor.", e);
        }

        return "";
    }

    private static String getDateString(Date date) {
        return date == null ? "" : date.toGMTString();
    }

    private static String extractInstanceId(String instanceArn) {
        int index = instanceArn.lastIndexOf("/");
        return instanceArn.substring(index + 1);
    }

    private static String getInstancesOverviewString(DeploymentInfo deployment) {
        DeploymentOverview overview = deployment.getDeploymentOverview();
        if (overview == null) {
            return "";
        }

        long total = overview.getFailed() + overview.getInProgress()
                + overview.getPending() + overview.getSkipped()
                + overview.getSucceeded();
        return String.format("(%d of %d completed)",
                overview.getSucceeded(), total);
    }
}
