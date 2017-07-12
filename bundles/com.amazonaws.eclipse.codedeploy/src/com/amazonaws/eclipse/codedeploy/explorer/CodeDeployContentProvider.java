package com.amazonaws.eclipse.codedeploy.explorer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.codedeploy.ServiceAPIUtils;
import com.amazonaws.eclipse.codedeploy.explorer.action.OpenDeploymentGroupEditorAction;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.codedeploy.AmazonCodeDeploy;
import com.amazonaws.services.codedeploy.model.ApplicationInfo;
import com.amazonaws.services.codedeploy.model.DeploymentGroupInfo;
import com.amazonaws.services.codedeploy.model.DeploymentInfo;

public class CodeDeployContentProvider extends AbstractContentProvider {

    private static CodeDeployContentProvider instance;

    public CodeDeployContentProvider() {
        instance = this;
    }

    /*
     * Abstract methods of AbstractContentProvider
     */

    @Override
    public boolean hasChildren(Object element) {
        return (element instanceof AWSResourcesRootElement ||
                element instanceof CodeDeployRootElement ||
                element instanceof ApplicationInfo);
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if (parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { CodeDeployRootElement.ROOT_ELEMENT };
        }

        if (parentElement instanceof CodeDeployRootElement) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonCodeDeploy client = AwsToolkitCore.getClientFactory()
                            .getCodeDeployClient();
                    return ServiceAPIUtils.getAllApplicationInfos(client).toArray();
                }
            }.start();
        }

        if (parentElement instanceof ApplicationInfo) {
            final String appName = ((ApplicationInfo) parentElement).getApplicationName();

            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonCodeDeploy client = AwsToolkitCore.getClientFactory()
                            .getCodeDeployClient();
                    List<DeploymentGroupInfo> groups = ServiceAPIUtils
                            .getAllDeploymentGroupInfos(client, appName);

                    // Wrap the DeploymentGroupInfo objects into DeploymentGroupNodes
                    List<DeploymentGroupNode> nodes = new LinkedList<>();

                    for (DeploymentGroupInfo group : groups) {
                        DeploymentInfo mostRecentDeployment = ServiceAPIUtils
                                .getMostRecentDeployment(client, appName,
                                        group.getDeploymentGroupName());

                        nodes.add(new DeploymentGroupNode(group, mostRecentDeployment));
                    }

                    return nodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.CODE_DEPLOY;
    }

    @Override
    public void dispose() {
        viewer.removeOpenListener(listener);
        super.dispose();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);
        this.viewer.addOpenListener(listener);
    }

    public static CodeDeployContentProvider getInstance() {
        return instance;
    }

    private final IOpenListener listener = new IOpenListener() {

        @Override
        public void open(OpenEvent event) {
            StructuredSelection selection = (StructuredSelection)event.getSelection();

            Iterator<?> i = selection.iterator();
            while ( i.hasNext() ) {
                Object obj = i.next();
                if ( obj instanceof DeploymentGroupNode ) {
                    DeploymentGroupInfo group = ((DeploymentGroupNode) obj)
                            .getDeploymentGroup();
                    OpenDeploymentGroupEditorAction action = new OpenDeploymentGroupEditorAction(
                            group.getApplicationName(),
                            group.getDeploymentGroupName(),
                            RegionUtils.getCurrentRegion());
                    action.run();
                }
            }
        }
    };
}
