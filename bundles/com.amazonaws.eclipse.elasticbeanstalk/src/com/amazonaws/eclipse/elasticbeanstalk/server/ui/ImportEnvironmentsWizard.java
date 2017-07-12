/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.wst.server.core.IServer;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.Region;
import com.amazonaws.eclipse.core.regions.RegionUtils;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.NoCredentialsDialog;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;


/**
 * Wizard that allows the user to select running AWS Elastic Beanstalk environments to import into
 * the servers view.
 */
public class ImportEnvironmentsWizard extends Wizard {

    private CheckboxTreeViewer viewer;

    private static final int COL_ENV_NAME = 0;
    private static final int COL_APPLICATION_NAME = 1;
    private static final int COL_DATE = 2;
    private static final int COL_STATUS = 3;
    private static final int COL_REGION = 4;
    private static final int NUM_COLS = 5;

    /*
     * Environments to import, cast as Objects to interface easily with jface.
     */
    private Object[] toImport = null;

    public ImportEnvironmentsWizard() {
        super();
        setWindowTitle("Import environments into the Servers view");
        setHelpAvailable(false);
    }

    private static final class RegionEnvironmentDescription {
        private final Region region;
        private final EnvironmentDescription environmentDescription;

        public RegionEnvironmentDescription(Region region, EnvironmentDescription environmentDescription) {
            this.region = region;
            this.environmentDescription = environmentDescription;
        }
    }
    
    /**
     * Our single page is responsible for creating the controls.
     */
    private class ImportPage extends WizardPage {

        private static final String MESSAGE = "Choose the environments to import.";

        protected ImportPage() {
            super("Import environments", MESSAGE, AwsToolkitCore
                    .getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        }

        @Override
        public void createControl(Composite container) {

            if ( !AwsToolkitCore.getDefault().getAccountInfo().isValid() ) {
                setControl(NoCredentialsDialog.createComposite(container));
                return;
            }

            IStatus status = testConnection();
            if ( !status.isOK() ) {
                setControl(new ErrorComposite(container, SWT.None, status));
                return;
            }

            Composite parent = new Composite(container, SWT.None);
            FillLayout layout = new FillLayout(SWT.VERTICAL);
            parent.setLayout(layout);

            /*
             * Determine which elastic beanstalk environments aren't already imported as servers
             */
            List<RegionEnvironmentDescription> environmentsToImport = new LinkedList<>();
            for ( Region region : RegionUtils.getRegionsForService(ServiceAbbreviations.BEANSTALK) ) {
                List<EnvironmentDescription> elasticBeanstalkEnvs = getExistingEnvironments(region);
                Collection<IServer> elasticBeanstalkServers = ElasticBeanstalkPlugin
                        .getExistingElasticBeanstalkServers();

                for ( EnvironmentDescription env : elasticBeanstalkEnvs ) {
                    boolean alreadyExists = false;
                    for ( IServer server : elasticBeanstalkServers ) {
                        if ( ElasticBeanstalkPlugin.environmentsSame(env, region, server) ) {
                            alreadyExists = true;
                            break;
                        }
                    }
                    if ( !alreadyExists ) {
                        environmentsToImport.add(new RegionEnvironmentDescription(region, env));
                    }
                }
            }

            if ( environmentsToImport.isEmpty() ) {
                new Label(parent, SWT.None).setText("There are no running environments to import.");
            } else {
                Composite treeContainer = new Composite(parent, SWT.None);
                treeContainer.setLayout(new TreeColumnLayout());

                int style = SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE;
                viewer = new CheckboxTreeViewer(treeContainer, style);
                viewer.getTree().setLinesVisible(true);
                viewer.getTree().setHeaderVisible(true);

                EnvironmentSelectionTreeProvider labelProvider = new EnvironmentSelectionTreeProvider();
                viewer.setContentProvider(labelProvider);
                viewer.setLabelProvider(labelProvider);

                for ( int i = 0; i < NUM_COLS; i++ ) {
                    switch (i) {
                    case COL_ENV_NAME:
                        newColumn("Environment name", 10);
                        break;
                    case COL_APPLICATION_NAME:
                        newColumn("Application name", 10);
                        break;
                    case COL_DATE:
                        newColumn("Last updated", 10);
                        break;
                    case COL_STATUS:
                        newColumn("Status", 10);
                        break;
                    case COL_REGION:
                        newColumn("Region", 10);
                        break;
                    }
                }

                viewer.setInput(environmentsToImport.toArray(new RegionEnvironmentDescription[environmentsToImport.size()]));
                viewer.getTree().addSelectionListener(new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if ( viewer.getCheckedElements().length > 0 ) {
                            setErrorMessage(null);
                            setMessage(MESSAGE);
                            toImport = viewer.getCheckedElements();
                        } else {
                            setErrorMessage("Select at least one environment to import");
                            toImport = null;
                        }
                        ImportEnvironmentsWizard.this.getContainer().updateButtons();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        widgetSelected(e);
                    }
                });
            }

            setControl(container);
        }

        private IStatus testConnection() {
            try {
                this.getWizard()
                        .getContainer()
                        .run(true,
                                false,
                                new CheckAccountRunnable(AwsToolkitCore.getClientFactory()
                                        .getElasticBeanstalkClientByEndpoint(
                                                RegionUtils.getRegion(ElasticBeanstalkPlugin.DEFAULT_REGION)
                                                        .getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK))));
                return Status.OK_STATUS;
            } catch ( InvocationTargetException ite ) {
                String errorMessage = "Unable to connect to AWS Elastic Beanstalk.  ";
                try {
                    throw ite.getCause();
                } catch ( AmazonServiceException ase ) {
                    errorMessage += "Make sure you've registered your AWS account for the AWS Elastic Beanstalk service.";
                } catch ( AmazonClientException ace ) {
                    errorMessage += "Make sure your computer is connected to the internet, and any network firewalls or proxys are configured appropriately.";
                } catch ( Throwable t ) {
                }

                return new Status(IStatus.ERROR, ElasticBeanstalkPlugin.PLUGIN_ID, errorMessage, ite.getCause());
            } catch ( InterruptedException e ) {
                return Status.CANCEL_STATUS;
            }
        }
    }

    @Override
    public void addPages() {
        addPage(new ImportPage());
    }

    /**
     * Returns all AWS Elastic Beanstalk environments in the region given.
     */
    private List<EnvironmentDescription> getExistingEnvironments(Region region) {
        List<EnvironmentDescription> filtered = new ArrayList<>();

        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(
                region.getServiceEndpoints().get(ServiceAbbreviations.BEANSTALK));
        List<EnvironmentDescription> environments = client.describeEnvironments().getEnvironments();

        // Only list the active environments
        for ( EnvironmentDescription env : environments ) {
            if ( !(env.getStatus().equals(EnvironmentStatus.Terminated.toString()) || env.getStatus().equals(
                    EnvironmentStatus.Terminating.toString())) )
                filtered.add(env);
        }
        return filtered;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Importing servers", toImport.length * 3);
                    try {
                        for ( Object elasticBeanstalkEnv : toImport ) {
                            try {
                                RegionEnvironmentDescription regionEnvironmentDescription = (RegionEnvironmentDescription) elasticBeanstalkEnv;
                                ElasticBeanstalkPlugin.importEnvironment(
                                        regionEnvironmentDescription.environmentDescription,
                                        regionEnvironmentDescription.region, monitor);
                            } catch ( CoreException e ) {
                                throw new RuntimeException(e);
                            }
                        }
                    } finally {
                        monitor.done();
                    }
                }
            });
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public boolean needsProgressMonitor() {
        return true;
    }


    @Override
    public boolean canFinish() {
        return toImport != null;
    }

    protected TreeColumn newColumn(String columnText, int weight) {
        Tree table = viewer.getTree();
        TreeColumn column = new TreeColumn(table, SWT.NONE);
        column.setText(columnText);

        TreeColumnLayout tableColumnLayout = (TreeColumnLayout) viewer.getTree().getParent().getLayout();
        if ( tableColumnLayout == null )
            tableColumnLayout = new TreeColumnLayout();
        tableColumnLayout.setColumnData(column, new ColumnWeightData(weight));
        viewer.getTree().getParent().setLayout(tableColumnLayout);

        return column;
    }

    private class EnvironmentSelectionTreeProvider implements ITableLabelProvider, ITreeContentProvider {

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            RegionEnvironmentDescription env = (RegionEnvironmentDescription) element;

            switch (columnIndex) {
            case COL_ENV_NAME:
                return env.environmentDescription.getEnvironmentName();
            case COL_APPLICATION_NAME:
                return env.environmentDescription.getApplicationName();
            case COL_DATE:
                return env.environmentDescription.getDateUpdated().toString();
            case COL_STATUS:
                return env.environmentDescription.getStatus();
            case COL_REGION:
                return env.region.getName();
            }
            return "";
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if ( columnIndex == 0 )
                return ElasticBeanstalkPlugin.getDefault().getImageRegistry().get(ElasticBeanstalkPlugin.IMG_SERVER);
            return null;
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Override
        public boolean hasChildren(Object element) {
            return false;
        }

        @Override
        public Object getParent(Object element) {
            return null;
        }

        @Override
        public Object[] getElements(Object inputElement) {
            return (Object[]) inputElement;
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }
    }
}
