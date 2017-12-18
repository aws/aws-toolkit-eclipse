package com.amazonaws.eclipse.core.ui.wizards;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

public class WizardWidgetFactory {

    public static ControlDecoration newControlDecoration(Control control, String message) {
        ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
        decoration.setDescriptionText(message);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        decoration.setImage(fieldDecoration.getImage());
        return decoration;
    }

    public static Group newGroup(Composite parent, String text) {
        return newGroup(parent, text, 1);
    }

    public static Group newGroup(Composite parent, String text, int colspan) {
        return newGroup(parent, text, colspan, 1);
    }

    public static Group newGroup(Composite parent, String text, int colspan, int cols) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(text);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        group.setLayoutData(gridData);
        group.setLayout(new GridLayout(cols, false));
        return group;
    }

    public static Composite newComposite(Composite parent, int colspan, int cols) {
        return newComposite(parent, colspan, cols, false);
    }

    public static Composite newComposite(Composite parent, int colspan, int cols, boolean columnsEqualWidth) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        composite.setLayoutData(gridData);
        composite.setLayout(new GridLayout(cols, columnsEqualWidth));
        return composite;
    }

    public static SashForm newSashForm(Composite parent, int colspan, int cols) {
        SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.horizontalSpan = colspan;
        gridData.widthHint = 300;
        gridData.heightHint = 200;
        sashForm.setLayoutData(gridData);
        sashForm.setLayout(new GridLayout(cols, false));
        return sashForm;
    }

    public static Text newText(Composite parent) {
        return newText(parent, "");
    }

    public static Text newText(Composite parent, String value) {
        return newText(parent, value, 1);
    }

    public static Text newText(Composite parent, String value, int colspan) {
        Text text = new Text(parent, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        text.setLayoutData(gridData);
        text.setText(value);
        return text;
    }

    public static Label newLabel(Composite parent, String text) {
        return newLabel(parent, text, 1);
    }

    public static Label newFillingLabel(Composite parent, String text) {
        return newFillingLabel(parent, text, 1);
    }

    public static Label newFillingLabel(Composite parent, String text, int colspan) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = colspan;
        gridData.widthHint = 100;
        label.setLayoutData(gridData);
        return label;
    }

    public static Label newLabel(Composite parent, String text, int colspan) {
        Label label = new Label(parent, SWT.WRAP);
        label.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        label.setLayoutData(gridData);
        return label;
    }

    public static Link newLink(Composite composite, Listener linkListener, String linkText, int colspan, int widthHint, int heightHint) {
        Link link = new Link(composite, SWT.WRAP);
        link.setText(linkText);
        link.addListener(SWT.Selection, linkListener);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.horizontalSpan = colspan;
        data.widthHint = widthHint;
        data.heightHint = heightHint;
        link.setLayoutData(data);
        return link;
    }

    public static Link newLink(Composite composite, Listener linkListener, String linkText, int colspan) {
        return newLink(composite, linkListener, linkText, colspan, 100, SWT.DEFAULT);
    }

    public static Combo newCombo(Composite parent) {
        return newCombo(parent, 1);
    }

    public static List newList(Composite parent) {
        return newList(parent, 1);
    }

    public static Combo newCombo(Composite parent, int colspan) {
        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        combo.setLayoutData(gridData);
        return combo;
    }

    public static ComboViewer newComboViewer(Composite parent) {
        return newComboViewer(parent, 1);
    }

    public static ComboViewer newComboViewer(Composite parent, int colspan) {
        ComboViewer comboViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        comboViewer.getCombo().setLayoutData(gridData);

        return comboViewer;
    }

    public static Button newCheckbox(Composite parent, String text, int colspan) {
        Button button = new Button(parent, SWT.CHECK);
        button.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        button.setLayoutData(gridData);
        return button;
    }

    public static Button newRadioButton(Composite parent, String text, int colspan,
            boolean selected, SelectionListener selectionListener) {
        Button radioButton = new Button(parent, SWT.RADIO);
        radioButton.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        radioButton.setLayoutData(gridData);
        radioButton.addSelectionListener(selectionListener);
        radioButton.setSelection(selected);
        return radioButton;
    }

    public static Button newPushButton(Composite parent, String text) {
        return newPushButton(parent, text, 1);
    }

    public static Button newPushButton(Composite parent, String text, int colspan) {
        Button pushButton = new Button(parent, SWT.PUSH);
        pushButton.setText(text);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gridData.horizontalSpan = colspan;
        pushButton.setLayoutData(gridData);
        return pushButton;
    }

    public static List newList(Composite parent, int colspan) {
        List list = new List(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);


        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.horizontalSpan = colspan;
        gridData.verticalSpan = 1;
        gridData.heightHint = 150;
        list.setLayoutData(gridData);

        return list;
    }
}
