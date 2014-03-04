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

package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import com.amazonaws.eclipse.ec2.ui.views.instances.columns.BuiltinColumn;
import com.amazonaws.eclipse.ec2.ui.views.instances.columns.TableColumn;
import com.amazonaws.eclipse.ec2.ui.views.instances.columns.TagColumn;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Label and content provider for the EC2 Instance table.
 */
class ViewContentAndLabelProvider extends BaseLabelProvider
		implements ITreeContentProvider, ITableLabelProvider {

	private List<TableColumn> columns = new ArrayList<TableColumn>();
	
	public ViewContentAndLabelProvider() {
		setColumns();
	}
	
	private void setColumns() {
		columns = new ArrayList<TableColumn>();
		columns.add(new TagColumn("Name"));
		columns.add(new BuiltinColumn(BuiltinColumn.INSTANCE_ID_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.PUBLIC_DNS_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.IMAGE_ID_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.ROOT_DEVICE_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.STATE_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.INSTANCE_TYPE_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.AVAILABILITY_ZONE_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.KEY_NAME_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.LAUNCH_TIME_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.SECURITY_GROUPS_COLUMN, instancesViewInput));
		columns.add(new BuiltinColumn(BuiltinColumn.TAGS_COLUMN, instancesViewInput));
	}

	/** The input to be displayed by this content / label provider */
	private InstancesViewInput instancesViewInput;

	/*
	 * IStructuredContentProvider Interface
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		instancesViewInput = (InstancesViewInput)newInput;
		setColumns();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object parent) {
		if (instancesViewInput == null) {
			return new Object[0];
		}

		return instancesViewInput.instances.toArray();
	}


	/*
	 * ITableLabelProvider Interface
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	public String getColumnText(Object obj, int index) {
		Instance instance = (Instance)obj;
		return columns.get(index).getText(instance);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	public Image getColumnImage(Object obj, int index) {

		Instance instance = (Instance)obj;

		return columns.get(index).getImage(instance);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		return null;
	}

	/*
	 * Package Interface
	 */

	/**
	 * Provides access to the instance -> security groups mapping contained in
	 * this class. This should probably be shifted around into another location,
	 * but for now we'll provide this data from here. If the amount of other
	 * data we need to associated with the Instance datatype grows a lot,
	 * we should definitely clean this up so that other objects can more easily
	 * access this data.
	 *
	 * @param instanceId
	 *            The ID of the instance to look up.
	 *
	 * @return A list of the security groups the specified instance is in.
	 */
	List<String> getSecurityGroupsForInstance(String instanceId) {
		if (instancesViewInput == null) {
			return null;
		}

		return instancesViewInput.securityGroupMap.get(instanceId);
	}


	/*
	 * Private Interface
	 */

    public Object[] getChildren(Object parentElement) {
		return new Object[0];
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}

	public List<TableColumn> getColumns() {
		return columns;
	}
}
