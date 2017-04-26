/*
 * Copyright 2008-2012 Amazon Technologies, Inc.
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

package com.amazonaws.eclipse.core.ui.preferences;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

/**
 * Abstract base class containing common logic for all AWS Toolkit preference
 * pages to share.
 */
public abstract class AwsToolkitPreferencePage extends PreferencePage {

    /** The layout column width for this page's field editors */
    protected static final int LAYOUT_COLUMN_WIDTH = 3;

    /**
     * Constructs a new AwsToolkitPreferencePage.
     *
     * @param name
     *            The title of this preference page.
     */
    public AwsToolkitPreferencePage(String name) {
        super(name);
    }

    /**
     * Convenience method for creating a new Group with the specified label and
     * parent.
     *
     * @param groupText
     *            The label for this new group.
     * @param parent
     *            The parent for the new UI widget.
     *
     * @return The new Group widget.
     */
    protected static Group newGroup(String groupText, Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        group.setText(groupText);

        return group;
    }

    /**
     * Creates a new label with the specified text.
     *
     * @param labelText
     *            The text for the label.
     * @param composite
     *            The parent composite.
     *
     * @return The new Label.
     */
    protected static Label newLabel(String labelText, Composite composite) {
        Label label = new Label(composite, SWT.WRAP);
        label.setText(labelText);
        GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
        data.horizontalSpan = LAYOUT_COLUMN_WIDTH;
        data.widthHint = 500; // SWT won't wrap without a widthHint
        label.setLayoutData(data);

        return label;
    }

    /**
     * Convenience method for creating a new Link widget.
     *
     * @param linkListener
     *            The lister to add to the new Link.
     * @param linkText
     *            The text for the new Link.
     * @param composite
     *            The parent for the new Link.
     */
    protected static Link newLink(Listener linkListener, String linkText, Composite composite) {
        Link link = new Link(composite, SWT.WRAP);
        link.setText(linkText);
        link.addListener(SWT.Selection, linkListener);
        GridData data = new GridData(SWT.FILL, SWT.FILL, false, false);
        data.horizontalSpan = LAYOUT_COLUMN_WIDTH;
        data.widthHint = 500;
        link.setLayoutData(data);

        return link;
    }

    /**
     * Creates a thin, empty composite to help space components vertically.
     *
     * @param parent
     *            The composite this spacer is being added to.
     */
    protected static void createSpacer(Composite parent) {
        Composite spacer = new Composite(parent, SWT.NONE);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = 5;
        data.horizontalSpan = LAYOUT_COLUMN_WIDTH;
        spacer.setLayoutData(data);
    }

    /**
     * Tweaks the specified GridLayout to restore various settings. This method
     * is intended to be run after FieldEditors have been added to the parent
     * component so that anything they changed in the layout can be fixed.
     *
     * @param layout
     *            The layout to tweak.
     */
    protected static void tweakLayout(GridLayout layout) {
        layout.numColumns = LAYOUT_COLUMN_WIDTH;
        layout.marginWidth = 10;
        layout.marginHeight = 8;
    }

    /**
     * Creates a new ObfuscatingStringFieldEditor based on the specified
     * parameters.
     *
     * @param preferenceKey
     *            The key for the preference managed by this field editor.
     * @param label
     *            The label for this field editor.
     * @param parent
     *            The parent for this field editor.
     *
     * @return The new FieldEditor.
     */
    protected ObfuscatingStringFieldEditor newStringFieldEditor(String preferenceKey, String label, Composite parent) {
        ObfuscatingStringFieldEditor fieldEditor = new ObfuscatingStringFieldEditor(preferenceKey, label, parent);
        fieldEditor.setPage(this);
        fieldEditor.setPreferenceStore(this.getPreferenceStore());
        fieldEditor.load();
        fieldEditor.fillIntoGrid(parent, LAYOUT_COLUMN_WIDTH);

        return fieldEditor;
    }

    /**
     * Creates a new FileFieldEditor based on the specified parameters.
     *
     * @param preferenceKey
     *            The key for the preference managed by this field editor.
     * @param label
     *            The label for this field editor.
     * @param parent
     *            The parent for this field editor.
     *
     * @return The new FieldEditor.
     */
    protected FileFieldEditor newFileFieldEditor(String preferenceKey, String label, Composite parent) {
        FileFieldEditor fieldEditor = new FileFieldEditor(preferenceKey, label, parent);
        fieldEditor.setPage(this);
        fieldEditor.setPreferenceStore(this.getPreferenceStore());
        fieldEditor.load();
        fieldEditor.fillIntoGrid(parent, LAYOUT_COLUMN_WIDTH);

        return fieldEditor;
    }

}
