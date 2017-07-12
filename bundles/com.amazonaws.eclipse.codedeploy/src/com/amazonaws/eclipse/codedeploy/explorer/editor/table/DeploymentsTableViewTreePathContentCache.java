package com.amazonaws.eclipse.codedeploy.explorer.editor.table;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.viewers.TreePath;

import com.amazonaws.eclipse.codedeploy.ServiceAPIUtils;
import com.amazonaws.eclipse.codedeploy.explorer.editor.DeploymentGroupEditorInput;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;
import com.amazonaws.services.codedeploy.model.InstanceSummary;
import com.amazonaws.services.codedeploy.model.LifecycleEvent;

/**
 * @ThreadSafe
 */
class DeploymentsTableViewTreePathContentCache implements TreePathContentProvider {

    private final Map<TreePath, Object[]> cache;
    private final DeploymentGroupEditorInput editorInput;

    public DeploymentsTableViewTreePathContentCache(DeploymentGroupEditorInput editorInput) {
        this.editorInput = editorInput;

        cache = new ConcurrentHashMap<>();
    }

    @Override
    public Object[] getChildren(TreePath parent) {
        if ( !cache.containsKey(parent) ) {
            cache.put(parent, loadChildren(parent));
        }
        return cache.get(parent);
    }

    @Override
    public void refresh() {
        cache.clear();
    }

    private Object[] loadChildren(TreePath parent) {
        if (parent.getSegmentCount() == 0) {
            // root
            List<DeploymentInfo> deployments = ServiceAPIUtils.getAllDeployments(
                    editorInput.getCodeDeployClient(),
                    editorInput.getApplicationName(),
                    editorInput.getDeploymentGroupName());

            // Sort by creation data
            Collections.sort(deployments, new Comparator<DeploymentInfo>() {
                @Override
                public int compare(DeploymentInfo a, DeploymentInfo b) {
                    int a_to_b = a.getCreateTime().compareTo(b.getCreateTime());
                    // In descending order
                    return - a_to_b;
                }
            });

            return deployments.toArray(new DeploymentInfo[deployments.size()]);

        } else {
            Object lastSegment = parent.getLastSegment();

            if (lastSegment instanceof DeploymentInfo) {
                DeploymentInfo deployment = (DeploymentInfo) lastSegment;

                List<InstanceSummary> instances = ServiceAPIUtils.getAllDeploymentInstances(
                        editorInput.getCodeDeployClient(),
                        deployment.getDeploymentId());
                return instances.toArray(new InstanceSummary[instances.size()]);

            } else if (lastSegment instanceof InstanceSummary) {
                InstanceSummary deploymentInstance = (InstanceSummary) lastSegment;

                List<LifecycleEvent> events = deploymentInstance.getLifecycleEvents();
                if (events == null) {
                    events = new LinkedList<>();
                }
                return events.toArray(new LifecycleEvent[events.size()]);

            }
        }

        return new Object[0];
    }
}
