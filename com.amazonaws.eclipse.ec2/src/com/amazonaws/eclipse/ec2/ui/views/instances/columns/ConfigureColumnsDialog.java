package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import java.util.HashMap;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ConfigureColumnsDialog extends Dialog {
	private String tagColumnStr;
	private Map<BuiltinColumn.ColumnType, Boolean> builtinColumns
		= new HashMap<BuiltinColumn.ColumnType, Boolean>();
	
	public ConfigureColumnsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, true));
		
		new Label(container, SWT.NONE).setText("Tag columns (comma-separated):");
		Text tagColumnText = new Text(container, SWT.BORDER);
		tagColumnText.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		tagColumnText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		tagColumnText.addListener(SWT.CHANGED, new Listener() {
			public void handleEvent(Event event) {
				tagColumnStr = ((Text)event.widget).getText();
			}
		});
		
		for (BuiltinColumn.ColumnType t : BuiltinColumn.ColumnType.values()) {
			Button ck = new Button(container, SWT.CHECK);
			ck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
			ck.setText(t.toString());
			ck.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					Button changedCk = (Button)e.widget;
					builtinColumns.put(
							BuiltinColumn.ColumnType.valueOf(
									changedCk.getText()), changedCk.getSelection());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});
		}
		
//		Button button = new Button(container, SWT.PUSH);
//		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
//				false));
//		button.setText("Press me");
//		button.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				System.out.println("Pressed");
//			}
//		});

		return container;
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

}
