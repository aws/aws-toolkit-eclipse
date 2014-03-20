/*
 * Copyright 2009-2012 Amazon Technologies, Inc.
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

import java.util.List;

import org.eclipse.jface.viewers.Viewer;

import com.amazonaws.eclipse.ec2.ui.SelectionTableComparator;
import com.amazonaws.eclipse.ec2.ui.views.instances.columns.TableColumn;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Comparator for sorting the instances by any column.
 */
class InstanceComparator extends SelectionTableComparator {

	/**
     * 
     */
    private final InstanceSelectionTable instanceSelectionTable;

    /**
	 * @param defaultColumn
	 * @param instanceSelectionTable TODO
	 */
	public InstanceComparator(InstanceSelectionTable instanceSelectionTable, int defaultColumn) {
		super(defaultColumn);
        this.instanceSelectionTable = instanceSelectionTable;
	}

	/* (non-Javadoc)
	 * @see com.amazonaws.eclipse.ec2.ui.views.instances.SelectionTableComparator#compareIgnoringDirection(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	protected int compareIgnoringDirection(Viewer viewer, Object e1, Object e2) {
		if (!(e1 instanceof Instance && e2 instanceof Instance)) {
			return 0;
		}

		Instance i1 = (Instance)e1;
		Instance i2 = (Instance)e2;

		List<TableColumn> columns = instanceSelectionTable.getContentAndLabelProvider().getColumns();
		return columns.get(this.sortColumn).compare(i1, i2);
	}
}
