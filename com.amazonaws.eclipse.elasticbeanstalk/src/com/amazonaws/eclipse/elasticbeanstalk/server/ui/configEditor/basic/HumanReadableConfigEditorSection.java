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
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.basic;

import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.AbstractEnvironmentConfigEditorPart;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigDataModel;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor.EnvironmentConfigEditorSection;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Base class for human-readable config editor sections that perform a simple
 * string translation.
 */
public abstract class HumanReadableConfigEditorSection extends EnvironmentConfigEditorSection {

    protected Composite parentComposite;

    public Composite getParentComposite() {
        return parentComposite;
    }

    public void setParentComposite(Composite parentComposite) {
        this.parentComposite = parentComposite;
    }

    public HumanReadableConfigEditorSection(AbstractEnvironmentConfigEditorPart parentEditor,
            EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext) {
        super(parentEditor, model, environment, bindingContext, null, null);
    }

    @Override
    protected String getName(ConfigurationOptionDescription option) {
        if ( getHumanReadableNames().containsKey(option.getName()) )
            return getHumanReadableNames().get(option.getName());
        return super.getName(option);
    }

    @Override
    protected void createSectionControls(Composite composite) {
        for ( String field : getFieldOrder() ) {
            for ( ConfigurationOptionDescription o : options ) {
                if ( field.equals(o.getName()) ) {
                    createOptionControl(composite, o);
                }
            }
        }
    }

    /**
     * Creates a section in the given composite.
     */
    @Override
    protected Section getSection(Composite parent) {
        return toolkit.createSection(parent, Section.EXPANDED | Section.DESCRIPTION);
    }

    /**
     * Returns a map of ConfigurationOptionDescription name to human-readable
     * name.
     */
    protected abstract Map<String, String> getHumanReadableNames();

    /**
     * Returns the display order of the fields, as given by their
     * ConfigurationOptionDescription names.
     */
    protected abstract String[] getFieldOrder();
}
