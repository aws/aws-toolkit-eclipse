/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.elasticbeanstalk.server.ui.configEditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateSetStrategy;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

import com.amazonaws.eclipse.databinding.ChainValidator;
import com.amazonaws.eclipse.databinding.DecorationChangeListener;
import com.amazonaws.eclipse.elasticbeanstalk.Environment;
import com.amazonaws.eclipse.elasticbeanstalk.server.ui.databinding.ConfigurationSettingValidator;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionDescription;

/**
 * Abstract base editor section that knows how to create controls for an editable option.
 */
public class EnvironmentConfigEditorSection extends ServerEditorSection {

    /** The section widget we're managing */
    protected Section section;
    protected AbstractEnvironmentConfigEditorPart parentEditor;
    protected EnvironmentConfigDataModel model;
    protected DataBindingContext bindingContext;
    protected final Environment environment;

    protected FormToolkit toolkit;

    protected String namespace;
    protected List<ConfigurationOptionDescription> options;

    /**
     * Sets the list of options that this section will present to the user, one
     * control per option. Can be set any time before the page is constructed.
     */
    public void setOptions(List<ConfigurationOptionDescription> options) {
        this.options = options;
    }

    /**
     * Constructs a new section for one namespace.
     *
     * @param parentEditor
     *            The editorPart that created this section
     * @param namespace
     *            The namespace of this section
     * @param options
     *            The options in the namespace
     */
    public EnvironmentConfigEditorSection(AbstractEnvironmentConfigEditorPart parentEditor,
            EnvironmentConfigDataModel model, Environment environment, DataBindingContext bindingContext,
            String namespace, List<ConfigurationOptionDescription> options) {
        this.parentEditor = parentEditor;
        this.bindingContext = bindingContext;
        this.environment = environment;
        this.model = model;
        this.namespace = namespace;
        this.options = options;
    }

    public int getNumControls() {
        return options.size();
    }

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);

        toolkit = getFormToolkit(parent.getDisplay());

        section = getSection(parent);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);

        section.setLayoutData(layoutData);

        Composite composite = toolkit.createComposite(section);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 5;
        layout.marginWidth = 10;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 15;
        composite.setLayout(layout);
        composite.setLayoutData(layoutData);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);
        section.setLayout(layout);
        section.setLayoutData(layoutData);

        createSectionControls(composite);
        section.setDescription(getSectionDescription());
        section.setText(getSectionName());
    }

    /**
     * Creates a section in the given composite.
     */
    protected Section getSection(Composite parent) {
        return toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | ExpandableComposite.FOCUS_TITLE);
    }

    /**
     * Creates all controls for the page using the composite given.
     */
    protected void createSectionControls(Composite composite) {
        for ( ConfigurationOptionDescription o : options ) {
            createOptionControl(composite, o);
        }
    }

    /**
     * Returns the name of this editor section.
     */
    protected String getSectionName() {
        return namespace;
    }

    /**
     * Returns the description for this editor section.
     */
    protected String getSectionDescription() {
        return null;
    }

    /**
     * Creates the appropriate control to display and change the option given.
     */
    protected void createOptionControl(Composite parent, ConfigurationOptionDescription option) {
        String valueType = option.getValueType();
        if ( valueType.equals("Scalar") ) {
            if (option.getValueOptions().isEmpty()) {
                createTextField(parent, option);
            } else {
                createCombo(parent, option);
            }
        } else if ( valueType.equals("Boolean") ) {
            createCheckbox(parent, option);
        } else if ( valueType.equals("List") ) {
            if (option.getValueOptions().isEmpty()) {
                createTextField(parent, option);
            } else {
                createList(parent, option);
            }
        } else if ( valueType.equals("CommaSeparatedList")) {
            createCommaSeparatedList(parent, option);
        } else if (valueType.equals("KeyValueList")) {
            createKeyValueList(parent, option);
        } else {
            Label label = createLabel(toolkit, parent, option);
            label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
            Label label1 = toolkit.createLabel(parent, (option.getValueOptions().toString() + "(" + valueType + ")"));
            label1.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
        }
    }

    /**
     * Creates a key value list control with the option given
     */
    private void createKeyValueList(Composite parent, ConfigurationOptionDescription option) {
        createTextField(parent, option);
    }

    /**
     * Creates a comma separated list with the option given
     */
    private void createCommaSeparatedList(Composite parent, ConfigurationOptionDescription option) {
        createTextField(parent, option);
    }

    /**
     * Creates a list of checkable options with the option given.
     */
    protected void createList(Composite parent, ConfigurationOptionDescription option) {
        GridData labelData = new GridData(SWT.LEFT, SWT.TOP, false, false);
        labelData.horizontalSpan = 2;
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(labelData);

        /*
         * This process is complicated and differs from the rest of the
         * mutliple-view data model binding in that it doesn't use the data
         * model singleton as a proxy to generate an observable. This is because
         * the observable set of values is created when the model is.
         *
         * It also requires explicit two-way wiring via listeners: one chunk to
         * update the model when the controls change, and another to update the
         * controls when the model changes. One-way listening is sufficient to
         * update the model, but not to make the two views of the model align.
         */
        final IObservableSet modelValues = (IObservableSet) model.getEntry(option);
        final IObservableSet controlValues = new WritableSet();
        controlValues.addAll(modelValues);

        final List<Button> checkboxButtons = new ArrayList<>();
        int i = 0;
        Button lastButton = null;

        /*
         * Each button needs a listener to update the observed set of model
         * values.
         */
        for ( final String valueOption : option.getValueOptions() ) {
            final Button button = toolkit.createButton(parent, valueOption, SWT.CHECK);
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (button.getSelection()) {
                        controlValues.add(valueOption);
                    } else {
                        controlValues.remove(valueOption);
                    }
                }
            });
            button.addSelectionListener(new DirtyMarker());
            checkboxButtons.add(button);
            lastButton = button;
            i++;
        }

        /*
         * Make sure we don't have an odd number of elements screwing up the
         * rest of the layout.
         */
        if ( i % 2 != 0 ) {
            GridData buttonData = new GridData(SWT.LEFT, SWT.TOP, false, false);
            buttonData.horizontalSpan = 2;
            lastButton.setLayoutData(labelData);
        }

        Binding bindSet = bindingContext.bindSet(controlValues, modelValues, new UpdateSetStrategy(UpdateSetStrategy.POLICY_UPDATE),
                new UpdateSetStrategy(UpdateSetStrategy.POLICY_UPDATE));

        /*
         * The observed set of model values needs a listener to update the
         * controls, in case the selection event came from another set of
         * controls with which we need to synchronize.
         */
        controlValues.addSetChangeListener(new ISetChangeListener() {

            @Override
            public void handleSetChange(SetChangeEvent event) {
                for ( Button button : checkboxButtons ) {
                    boolean checked = false;
                    for ( Object value : modelValues ) {
                        if (button.getText().equals(value)) {
                            checked = true;
                            break;
                        }
                    }
                    button.setSelection(checked);
                }
            }
        });

        bindSet.updateModelToTarget();
    }

    /**
     * Creates a checkbox control with the option given.
     */
    protected void createCheckbox(Composite parent, ConfigurationOptionDescription option) {
        Button button = toolkit.createButton(parent, getName(option), SWT.CHECK);
        GridData layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        button.setLayoutData(layoutData);

        IObservableValue modelv = model.observeEntry(option);
        ISWTObservableValue widget = SWTObservables.observeSelection(button);
        bindingContext.bindValue(widget, modelv,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));

        modelv.addChangeListener(new DirtyMarker());
    }

    protected String getName(ConfigurationOptionDescription option) {
        return option.getName();
    }

    /**
     * Creates a drop-down combo with the option given.
     */
    protected void createCombo(Composite parent, ConfigurationOptionDescription option) {
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        Combo combo = new Combo(parent, SWT.READ_ONLY);
        combo.setItems(option.getValueOptions().toArray(new String[option.getValueOptions().size()]));
        IObservableValue modelv = model.observeEntry(option);
        ISWTObservableValue widget = SWTObservables.observeSelection(combo);
        parentEditor.bindingContext.bindValue(widget, modelv,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));
        modelv.addChangeListener(new DirtyMarker());
    }

    /**
     * Creates a text field and label combo using the option given.
     */
    protected void createTextField(Composite parent, ConfigurationOptionDescription option) {
        Label label = createLabel(toolkit, parent, option);
        label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        Text text = toolkit.createText(parent, "");

        layoutTextField(text);

        IObservableValue modelv = model.observeEntry(option);
        ISWTObservableValue widget = SWTObservables.observeText(text, SWT.Modify);
        bindingContext.bindValue(widget, modelv,
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_UPDATE));
        modelv.addChangeListener(new DirtyMarker());

        ChainValidator<String> validationStatusProvider = new ChainValidator<>(widget,
                new ConfigurationSettingValidator(option));
        bindingContext.addValidationStatusProvider(validationStatusProvider);
        ControlDecoration decoration = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText("Invalid value");
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(
                FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        new DecorationChangeListener(decoration, validationStatusProvider.getValidationStatus());
    }

    protected void layoutTextField(Text text) {
        GridData textLayout = new GridData(SWT.LEFT, SWT.TOP, false, false);
        GC gc = new GC(text);
        FontMetrics fm = gc.getFontMetrics();
        textLayout.widthHint = text.computeSize(fm.getAverageCharWidth() * 30, SWT.DEFAULT).x;
        gc.dispose();
        text.setLayoutData(textLayout);
    }

    protected Label createLabel(FormToolkit toolkit, Composite parent, ConfigurationOptionDescription option) {

        String labelText = getName(option);
        if ( option.getChangeSeverity().equals("RestartEnvironment") )
            labelText += " **";
        else if ( option.getChangeSeverity().equals("RestartApplicationServer") )
            labelText += " *";

        if ( option.getValueType().equals("CommaSeparatedList") && option.getValueOptions().isEmpty() ) {
            labelText += "\n(comma separated)";
        } else if ( option.getValueType().equals("KeyValueList") && option.getValueOptions().isEmpty() ) {
            labelText += "\n(key-value list)";
        }

        Label label = toolkit.createLabel(parent, labelText);
        return label;
    }

    /**
     * Generic listener that marks the editor dirty.
     */
    protected final class DirtyMarker implements SelectionListener, ModifyListener, IChangeListener {

        public DirtyMarker() {
        }

        @Override
        public void modifyText(ModifyEvent e) {
            markDirty();
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            markDirty();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            markDirty();
        }

        private void markDirty() {
            EnvironmentConfigEditorSection.this.parentEditor.markDirty();
        }

        @Override
        public void handleChange(ChangeEvent event) {
            markDirty();
        }
    }
}
