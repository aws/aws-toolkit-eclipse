package com.amazonaws.eclipse.codedeploy;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;

import com.amazonaws.eclipse.codedeploy.explorer.image.CodeDeployExplorerImages;

public class CodeDeployPlugin extends AbstractUIPlugin implements IStartup {

    public static final String PLUGIN_ID = "com.amazonaws.eclipse.codedeploy";

    public static final String DEFAULT_REGION = "us-east-1";

    private static CodeDeployPlugin plugin;

    @Override
    public void earlyStartup() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        plugin = this;
    }

    @Override
    protected ImageRegistry createImageRegistry() {
        return CodeDeployExplorerImages.createImageRegistry();
    }

    /**
     * Returns the shared plugin instance.
     *
     * @return the shared plugin instance.
     */
    public static CodeDeployPlugin getDefault() {
        return plugin;
    }

    /**
     * Convenience method for reporting the exception to StatusManager
     */
    public void reportException(String errorMessage, Throwable e) {
        StatusManager.getManager().handle(
                new Status(IStatus.ERROR, PLUGIN_ID,
                        errorMessage, e),
                        StatusManager.SHOW | StatusManager.LOG);
    }

    /**
     * Convenience method for logging a debug message at INFO level.
     */
    public void logInfo(String debugMessage) {
        getLog().log(new Status(Status.INFO, PLUGIN_ID, debugMessage, null));
    }

    public void warn(String message, Throwable e) {
        getLog().log(new Status(Status.WARNING, PLUGIN_ID, message, e));
    }

}
