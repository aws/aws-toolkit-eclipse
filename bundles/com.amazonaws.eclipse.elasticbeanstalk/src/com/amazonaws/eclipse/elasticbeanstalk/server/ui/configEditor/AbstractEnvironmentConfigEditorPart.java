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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;

import com.amazonaws.eclipse.core.ui.CancelableThread;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.explorer.AwsAction;

/**
 * Abstract base class for editor editor parts that edit an environment
 * configuration.
 */
public abstract class AbstractEnvironmentConfigEditorPart extends ServerEditorPart {

    protected static final String RESTART_NOTICE = "Save the editor to apply your changes to your AWS Elastic Beanstalk environment.  "
            + "Changing a configuration value marked with * will cause your application server to restart.  "
            + "Changing a value marked with ** will cause your environment to restart.  "
            + "Your environment will become unavailable for several minutes.";

    // SWT editor glue
    protected ManagedForm managedForm;
    protected Environment environment;
    protected boolean dirty = false;
    protected AwsAction refreshAction;

    // Data model and binding context
    protected EnvironmentConfigDataModel model;
    protected DataBindingContext bindingContext = new DataBindingContext();
    AggregateValidationStatus aggregateValidationStatus = new AggregateValidationStatus(bindingContext,
            AggregateValidationStatus.MAX_SEVERITY);

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);
        environment = (Environment) server.loadAdapter(Environment.class, null);

        model = EnvironmentConfigDataModel.getInstance(environment);

        aggregateValidationStatus.addChangeListener(new IChangeListener() {

            @Override
            public void handleChange(ChangeEvent event) {
                Object value = aggregateValidationStatus.getValue();
                if ( value instanceof IStatus == false )
                    return;

                IStatus status = (IStatus) value;
                if ( status.getSeverity() == IStatus.OK ) {
                    setErrorMessage(null);
                } else {
                    if ( status.getSeverity() == IStatus.ERROR ) {
                        setErrorMessage(status.getMessage());
                    }
                }
            }
        });

    }

    @Override
    public void setFocus() {
        managedForm.getForm().setFocus();
    }

    public void markDirty() {
        if (!dirty) {
            dirty = true;
            execute(new NullOperation());
        }
    }



    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Cancels the thread given if it's running.
     */
    protected void cancelThread(CancelableThread thread) {
        if ( thread != null ) {
            synchronized (thread) {
                if ( thread.isRunning() ) {
                    thread.cancel();
                }
            }
        }
    }

    public DataBindingContext getDataBindingContext() {
           return bindingContext;
    }

    /**
     * Refreshes the editor with the latest values
     */
    public abstract void refresh(String templateName);

    /**
     * Destroy the controls to let refresh method to redraw them.
     * Sometimes we do not need to destroy these controls before refresh
     */
    public abstract void destroyOldLayouts();


}
