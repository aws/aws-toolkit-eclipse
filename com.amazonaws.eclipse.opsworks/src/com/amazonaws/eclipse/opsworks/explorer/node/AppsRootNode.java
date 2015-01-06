package com.amazonaws.eclipse.opsworks.explorer.node;

import java.util.Collections;
import java.util.List;

import com.amazonaws.eclipse.explorer.ExplorerNode;
import com.amazonaws.eclipse.opsworks.OpsWorksPlugin;
import com.amazonaws.eclipse.opsworks.explorer.image.OpsWorksExplorerImages;
import com.amazonaws.services.opsworks.model.App;

public class AppsRootNode extends ExplorerNode {

    private final List<App> apps;

    public AppsRootNode(List<App> apps) {
        super("Apps", 2,
              OpsWorksPlugin.getDefault().getImageRegistry()
                    .get(OpsWorksExplorerImages.IMG_APP),
              null);
        this.apps = apps;
    }

    public List<App> getApps() {
        return Collections.unmodifiableList(apps);
    }
}
