/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
import java.util.Iterator;
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
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.elasticbeanstalk.ElasticBeanstalkPlugin;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.NoCredentialsDialog;
import com.amazonaws.eclipse.elasticbeanstalk.Region;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
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

    /**
     * Our single page is responsible for creating the controls.
     */
    private class ImportPage extends WizardPage {

        private static final String MESSAGE = "Choose the environments to import.";

        protected ImportPage() {
            super("Import environments", MESSAGE, AwsToolkitCore
                    .getDefault().getImageRegistry().getDescriptor(AwsToolkitCore.IMAGE_AWS_LOGO));
        }

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
             * Subtract the list of existing servers from the list of
             * environment
             *
             * TODO: Need more work for regions here...
             */
            List<EnvironmentDescription> elasticBeanstalkEnvs = getExistingEnvironments();
            List<IServer> elasticBeanstalkServers = getExistingElasticBeanstalkServers();
            Iterator<EnvironmentDescription> envIter = elasticBeanstalkEnvs.iterator();
            while ( envIter.hasNext() ) {
                EnvironmentDescription env = envIter.next();
                for ( IServer server : elasticBeanstalkServers ) {
                    if ( environmentsSame(env, server) ) {
                        envIter.remove();
                        break;
                    }
                }
            }

            if ( elasticBeanstalkEnvs.isEmpty() ) {
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

                viewer.setInput(elasticBeanstalkEnvs.toArray(new EnvironmentDescription[elasticBeanstalkEnvs.size()]));
                viewer.getTree().addSelectionListener(new SelectionListener() {

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

                    public void widgetDefaultSelected(SelectionEvent e) {
                        widgetSelected(e);
                    }
                });
            }

            setControl(container);
        }

        /**
         * @return
         */
        private IStatus testConnection() {
            try {
                this.getWizard()
                        .getContainer()
                        .run(true,
                                false,
                                new CheckAccountRunnable(AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(
                                        Region.DEFAULT.getEndpoint())));
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
     * Returns whether the environment given represents the server given.
     */
    private boolean environmentsSame(EnvironmentDescription env, IServer server) {
        Environment environment = (Environment) server.getAdapter(Environment.class);
        return environment.getApplicationName().equals(env.getApplicationName())
                && environment.getEnvironmentName().equals(env.getEnvironmentName());
    }

    /**
     * Returns all AWS Elastic Beanstalk environments
     */
    private List<EnvironmentDescription> getExistingEnvironments() {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(Region.DEFAULT.getEndpoint());
        List<EnvironmentDescription> environments = client.describeEnvironments().getEnvironments();

        // Only list the active environments
        List<EnvironmentDescription> filtered = new ArrayList<EnvironmentDescription>();
        for ( EnvironmentDescription env : environments ) {
            if ( !(env.getStatus().equals(EnvironmentStatus.Terminated.toString()) || env.getStatus().equals(
                    EnvironmentStatus.Terminating.toString())) )
                filtered.add(env);
        }
        return filtered;
    }

    /**
     * Returns all AWS Elastic Beanstalk servers known to ServerCore
     */
    private List<IServer> getExistingElasticBeanstalkServers() {
        List<IServer> elasticBeanstalkServers = new ArrayList<IServer>();

        IServer[] servers = ServerCore.getServers();
        for ( IServer server : servers ) {
            if ( server.getServerType() == null) continue;
            if ( server.getServerType().getId().equals(ElasticBeanstalkPlugin.ELASTIC_BEANSTALK_SERVER_TYPE_ID) ) {
                elasticBeanstalkServers.add(server);
            }
        }

        return elasticBeanstalkServers;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        final IServerType serverType = ServerCore.findServerType(ElasticBeanstalkPlugin.ELASTIC_BEANSTALK_SERVER_TYPE_ID);

        if ( serverType == null )
            throw new RuntimeException("Couldn't find server type");

        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask("Importing servers", toImport.length * 3);
                    try {
                        for ( Object elasticBeanstalkEnv : toImport ) {
                            try {
                                IRuntimeWorkingCopy runtime = serverType.getRuntimeType().createRuntime(null, monitor);
                                IServerWorkingCopy serverWorkingCopy = serverType.createServer(null, null, runtime, monitor);
                                ServerDefaultsUtils.setDefaultHostName(serverWorkingCopy, Region.DEFAULT.getEndpoint());
                                ServerDefaultsUtils.setDefaultServerName(serverWorkingCopy, ((EnvironmentDescription)elasticBeanstalkEnv).getEnvironmentName());

                                Environment env = (Environment) serverWorkingCopy.loadAdapter(Environment.class, monitor);
                                fillInEnvironmentValues((EnvironmentDescription) elasticBeanstalkEnv, env, monitor);
                                monitor.subTask("Creating server");
                                serverWorkingCopy.save(true, monitor);
                                runtime.save(true, monitor);
                                monitor.worked(1);
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

    /**
     * Fills in the environment given with the values in the environment
     * description.
     */
    private void fillInEnvironmentValues(EnvironmentDescription elasticBeanstalkEnv, Environment env, IProgressMonitor monitor) {
        AWSElasticBeanstalk client = AwsToolkitCore.getClientFactory().getElasticBeanstalkClientByEndpoint(Region.DEFAULT.getEndpoint());

        monitor.subTask("Getting application info");
        DescribeApplicationsResult describeApplicationsResult = client
                .describeApplications(new DescribeApplicationsRequest().withApplicationNames(elasticBeanstalkEnv
                        .getApplicationName()));
        if ( describeApplicationsResult != null && !describeApplicationsResult.getApplications().isEmpty() )
            env.setApplicationDescription(describeApplicationsResult.getApplications().get(0).getDescription());
        monitor.worked(1);

        monitor.subTask("Getting environment configuration");
        DescribeConfigurationSettingsResult describeConfigurationSettingsResult = client
                .describeConfigurationSettings(new DescribeConfigurationSettingsRequest().withEnvironmentName(
                        elasticBeanstalkEnv.getEnvironmentName()).withApplicationName(elasticBeanstalkEnv.getApplicationName()));
        if ( describeApplicationsResult != null
                && !describeConfigurationSettingsResult.getConfigurationSettings().isEmpty() ) {
            for ( ConfigurationSettingsDescription settingDescription : describeConfigurationSettingsResult
                    .getConfigurationSettings() ) {
                for ( ConfigurationOptionSetting setting : settingDescription.getOptionSettings() ) {
                    if ( setting.getNamespace().equals("aws:autoscaling:launchconfiguration")
                            && setting.getOptionName().equals("EC2KeyName") ) {
                        env.setKeyPairName(setting.getValue());
                    }
                }
            }
        }
        monitor.worked(1);


        env.setApplicationName(elasticBeanstalkEnv.getApplicationName());
        env.setCname(elasticBeanstalkEnv.getCNAME());
        env.setEnvironmentName(elasticBeanstalkEnv.getEnvironmentName());
        env.setEnvironmentDescription(elasticBeanstalkEnv.getDescription());
        env.setRegionEndpoint(Region.DEFAULT.getEndpoint());
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

        public void removeListener(ILabelProviderListener listener) {
        }

        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        public void dispose() {
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public String getColumnText(Object element, int columnIndex) {
            EnvironmentDescription env = (EnvironmentDescription) element;

            switch (columnIndex) {
            case COL_ENV_NAME:
                return env.getEnvironmentName();
            case COL_APPLICATION_NAME:
                return env.getApplicationName();
            case COL_DATE:
                return env.getDateUpdated().toString();
            case COL_STATUS:
                return env.getStatus();
            case COL_REGION:
                return Region.DEFAULT.getName();
            }
            return "";
        }

        public Image getColumnImage(Object element, int columnIndex) {
            if ( columnIndex == 0 )
                return ElasticBeanstalkPlugin.getDefault().getImageRegistry().get(ElasticBeanstalkPlugin.IMG_SERVER);
            return null;
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        public boolean hasChildren(Object element) {
            return false;
        }

        public Object getParent(Object element) {
            return null;
        }

        public Object[] getElements(Object inputElement) {
            return (Object[]) inputElement;
        }

        public Object[] getChildren(Object parentElement) {
            return new Object[0];
        }
    }
}
