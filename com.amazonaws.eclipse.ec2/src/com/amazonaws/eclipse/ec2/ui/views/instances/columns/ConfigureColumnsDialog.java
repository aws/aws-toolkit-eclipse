package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.ec2.ui.views.instances.columns.BuiltinColumn.ColumnType;

public class ConfigureColumnsDialog extends Dialog {
	private String tagColumnStr;
	private Map<BuiltinColumn.ColumnType, Boolean> builtinColumns
		= new HashMap<BuiltinColumn.ColumnType, Boolean>();
	private List<TableColumn> columns;
	
	public ConfigureColumnsDialog(Shell parentShell) {
		super(parentShell);
	}

	protected boolean isResizable() {
		return true;
	}
	
	private void initializeTagText(Text tagColumnText) {
		tagColumnText.setText("");
		for (TableColumn c : columns) {
			if (c instanceof TagColumn) {
				String t = tagColumnText.getText();
				String tagName = ((TagColumn)c).getTagName();
				if (t.isEmpty())
					tagColumnText.setText(tagName);
				else
					tagColumnText.setText(tagColumnText.getText() + "," + tagName);
			}
		}
		tagColumnStr = tagColumnText.getText();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));
		
		new Label(composite, SWT.NONE).setText("Tag columns (comma-separated)");
		Text tagColumnText = new Text(composite, SWT.BORDER);
		tagColumnText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		tagColumnText.addListener(SWT.CHANGED, new Listener() {
			public void handleEvent(Event event) {
				tagColumnStr = ((Text)event.widget).getText();
			}
		});
		initializeTagText(tagColumnText);
		
		Group builtins = new Group(composite, SWT.NONE);
		builtins.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		builtins.setLayout(new GridLayout(1, true));
		builtins.setText("Built-in columns");
		for (BuiltinColumn.ColumnType t : BuiltinColumn.ColumnType.values()) {
			Button ck = new Button(builtins, SWT.CHECK);
			ck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			ck.setText(t.getName());
			initializeCheckbox(ck, t);
			ck.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					Button changedCk = (Button)e.widget;
					builtinColumns.put(
							BuiltinColumn.ColumnType.fromName(
									changedCk.getText()), changedCk.getSelection());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		}
		return composite;
	}
	
	private void initializeCheckbox(Button ck, ColumnType t) {
		for (TableColumn c : columns) {
			if (c instanceof BuiltinColumn && t == ColumnType.fromName(c.getColumnName()))
				ck.setSelection(true);
		}
	}

	public String getTagColumnText() {
		return tagColumnStr==null? "" : tagColumnStr;
	}

	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Configure columns");
	}

	public Map<BuiltinColumn.ColumnType, Boolean> getBuiltinColumns() {
		return builtinColumns;
	}

	public void initialize(List<TableColumn> columns) {
		this.columns = columns;
		for (TableColumn c : columns) {
			if (c instanceof BuiltinColumn)
				builtinColumns.put(((BuiltinColumn)c).getColumnType(), true);
		}
	}

}
