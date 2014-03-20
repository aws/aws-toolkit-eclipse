/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.amazonaws.eclipse.ec2.ui.views.instances.columns;

import org.eclipse.swt.graphics.Image;

import com.amazonaws.services.ec2.model.Instance;

public abstract class TableColumn {
	public abstract String getText(Instance instance);

	public abstract Image getImage(Instance instance);

	public abstract int compare(Instance i1, Instance i2);

	public abstract String getColumnName();
}
