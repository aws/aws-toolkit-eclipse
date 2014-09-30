package com.amazonaws.eclipse.sdk.ui;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.amazonaws.eclipse.sdk.ui.AbstractSdkManager.SdkDownloadJob;

public class SdkDownloadProgressTrackingComposite extends Composite {

    private final boolean destroyAfterCompletion;
    private final Label messageLabel;
    private final ProgressBar progressBar;

    public SdkDownloadProgressTrackingComposite(Composite parent,
            final boolean destroyAfterCompletion) {
        super(parent, SWT.None);
        this.destroyAfterCompletion = destroyAfterCompletion;

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        messageLabel = new Label(this, SWT.None);
        messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        progressBar = new ProgressBar(this, SWT.INDETERMINATE);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        restartTracker();
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    @SuppressWarnings("rawtypes")
    public void restartTracker() {
        messageLabel.setText(
                "The AWS SDK for Java is currently downloading.  " +
                "Please wait while it completes.");

        // We assume that the caller will only create this tracker after
        // detecting an ongoing download job. And therefore if we cannot find
        // the job at this point, we treat it as a successful download.
        SdkDownloadJob ongoingDownload = JavaSdkManager.getInstance().getInstallationJob();

        if (ongoingDownload == null) {
            if (JavaSdkManager.getInstance().getDefaultSdkInstall() != null) {
                onDownloadComplete_internal();
            } else {
                setMessage("Cannot detect the status of the SDK download. " +
                            "Please manually start the download in " +
                            "\"AWS SDK for Java\" preference page.");
            }

        } else {
            ongoingDownload.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {

                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {
                            onDownloadComplete_internal();
                        }
                    });

                }
            });
        }
    }

    /**
     * Override this method to provide additional callbacks to execute after the
     * download completes. Note that the execution of this method is already
     * wrapped inside Display.syncExec()..
     */
    protected void onDownloadComplete() {
        // callback hook.
    }

    private void onDownloadComplete_internal() {
        if (destroyAfterCompletion) {
            SdkDownloadProgressTrackingComposite.this.dispose();
        } else {
            progressBar.dispose();
            setMessage("Download completes. " +
                    "Please proceed to the next step.");
        }

        onDownloadComplete();
    }

}
