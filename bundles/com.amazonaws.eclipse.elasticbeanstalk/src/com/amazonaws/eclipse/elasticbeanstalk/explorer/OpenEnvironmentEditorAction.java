/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.explorer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.ui.internal.editor.ServerEditorInput;

import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.telemetry.AwsToolkitMetricType;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.explorer.AwsAction;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;

public class OpenEnvironmentEditorAction extends AwsAction {

    private final EnvironmentDescription env;
    private final Region region;

    public OpenEnvironmentEditorAction(EnvironmentDescription env, Region region) {
        super(AwsToolkitMetricType.EXPLORER_BEANSTALK_OPEN_ENVIRONMENT_EDITOR);
        this.env = env;
        this.region = region;
        this.setText("Open in WTP Server Editor");
    }

    @Override
    protected void doRun() {
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    IServer server = ElasticBeanstalkPlugin.getServer(env, region);
                    if ( server == null ) {
                        server = ElasticBeanstalkPlugin.importEnvironment(env, region, new NullProgressMonitor());
                    }
                    activeWindow.getActivePage().openEditor(new ServerEditorInput(server.getId()),
                            "org.eclipse.wst.server.ui.editor");
                    actionSucceeded();
                } catch ( Exception e ) {
                    actionFailed();
                    String errorMessage = "Unable to open the server editor: " + e.getMessage();
                    Status status = new Status(Status.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.SHOW);
                } finally {
                    actionFinished();
                }
            }
        });
    }
}
