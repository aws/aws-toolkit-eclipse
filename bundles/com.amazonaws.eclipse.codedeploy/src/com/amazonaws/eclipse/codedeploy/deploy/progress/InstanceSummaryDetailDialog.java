package com.amazonaws.eclipse.codedeploy.deploy.progress;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.amazonaws.services.codedeploy.model.InstanceSummary;
import com.amazonaws.services.codedeploy.model.LifecycleEvent;

public class InstanceSummaryDetailDialog extends Dialog {

    private final InstanceSummary instanceSummary;

    public InstanceSummaryDetailDialog(Shell parentShell,
            InstanceSummary instanceSummary) {
        super(parentShell);

        if (instanceSummary == null) {
            throw new NullPointerException("instanceSummary must not be null.");
        }
        this.instanceSummary = instanceSummary;
    }

    /**
     * To customize the dialog title
     */
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Instance Lifecycle Events");
    }

    /**
     * To customize the dialog button
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID,
                "Close", false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);

        createControls(container, instanceSummary);

        return container;
    }

    private void createControls(Composite parent, InstanceSummary instanceSummary) {
        for (LifecycleEvent event : instanceSummary.getLifecycleEvents()) {
            String eventName = event.getLifecycleEventName();

            LifecycleEventUIGroup group = new LifecycleEventUIGroup(
                    parent, SWT.NONE, eventName);
            group.update(event);
        }
    }

    private static String toUIString(Object object) {
        return object == null ? "n/a" : object.toString();
    }

    private static class LifecycleEventUIGroup extends Group {

        private Label label_Status;
        private Label label_StartTime;
        private Label label_EndTime;
        private Link  link_Diagnostics;

        public LifecycleEventUIGroup(Composite parent, int style, String groupName) {
            super(parent, style);

            setText(groupName);
            setFont(JFaceResources.getBannerFont());

            GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            setLayoutData(gridData);
            setLayout(new GridLayout(1, false));

            label_Status = new Label(this, SWT.NONE);
            label_StartTime = new Label(this, SWT.NONE);
            label_EndTime = new Label(this, SWT.NONE);
            link_Diagnostics = new Link(this, SWT.NONE);
            link_Diagnostics.setText("<a>View diagnostics</a>");
            link_Diagnostics.setEnabled(false);
        }

        public void update(final LifecycleEvent event) {
            label_Status.setText("Status: " + toUIString(event.getStatus()));
            label_StartTime.setText("Start time: " + toUIString(event.getStartTime()));
            label_EndTime.setText("End time: " + toUIString(event.getEndTime()));

            if (event.getDiagnostics() != null) {
                link_Diagnostics.setEnabled(true);
                link_Diagnostics.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent arg0) {
                        new LifecycleEventDiagnosticsDialog(
                                getShell(), event.getDiagnostics())
                        .open();
                    }
                });
            }
        }

        @Override
        protected void checkSubclass() {
        }
    }

}
