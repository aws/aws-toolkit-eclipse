package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.services.ec2.model.Instance;

public abstract class TableColumn {
	public abstract String getText(Instance instance);

	public abstract Image getImage(Instance instance);

	public abstract int compare(Instance i1, Instance i2);

	public abstract String getColumnName();
}
