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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.ui.explorer;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.providers.content.virtual.ColumnNode;
import org.eclipse.datatools.connectivity.sqm.core.internal.ui.explorer.providers.content.virtual.VirtualNodeServiceFactory;
import org.eclipse.datatools.connectivity.sqm.core.ui.explorer.virtual.IVirtualNode;
import org.eclipse.datatools.connectivity.sqm.server.internal.ui.explorer.providers.SQLModelContentProviderExtension;
import org.eclipse.datatools.modelbase.sql.schema.Catalog;
import org.eclipse.datatools.modelbase.sql.schema.Database;
import org.eclipse.datatools.modelbase.sql.schema.Schema;
import org.eclipse.datatools.modelbase.sql.tables.Column;
import org.eclipse.datatools.modelbase.sql.tables.Table;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

/**
 * This class is a content provider implementation for navigatorContent extensions. This class provides SQL model
 * content to the navigator.
 */
public class SimpleDBContentProviderExtension extends SQLModelContentProviderExtension implements
    IPipelinedTreeContentProvider {

  public static final String DOMAINS = "Domains"; //$NON-NLS-1$
  public static final String DB_DEFINITION_VENDOR = "SimpleDB"; //$NON-NLS-1$

  public SimpleDBContentProviderExtension() {
    super();
  }

  @Override
public Object[] getChildren(final Object parentElement) {
    return new Object[0];
  }

  @Override
public boolean hasChildren(final Object element) {
    return false;
  }

  @Override
public void getPipelinedChildren(final Object parent, final Set theCurrentChildren) {
  }

  @Override
public void getPipelinedElements(final Object anInput, final Set theCurrentElements) {
  }

  @Override
public Object getPipelinedParent(final Object anObject, final Object suggestedParent) {
    return suggestedParent;
  }

  @Override
public boolean interceptRefresh(final PipelinedViewerUpdate refreshSynchronization) {
    return false;
  }

  @Override
public PipelinedShapeModification interceptRemove(final PipelinedShapeModification removeModification) {
    return removeModification;
  }

  @Override
public boolean interceptUpdate(final PipelinedViewerUpdate anUpdateSynchronization) {
    return false;
  }

  @Override
public PipelinedShapeModification interceptAdd(final PipelinedShapeModification anAddModification) {
    //anAddModification.getChildren().clear();

    if (anAddModification.getParent() instanceof Database
        && DB_DEFINITION_VENDOR.equals(((Database) anAddModification.getParent()).getVendor())) {
      try {
        Database db = (Database) anAddModification.getParent();
        Schema schema = ((Schema) ((Catalog) db.getCatalogs().get(0)).getSchemas().get(0));

        anAddModification.getChildren().clear();
        IVirtualNode domains = VirtualNodeServiceFactory.INSTANCE.makeTableNode(DOMAINS, DOMAINS, db);
        domains.addChildren(schema.getTables());
        anAddModification.getChildren().add(domains);
      } catch (Exception e) {
        // strange broken tree, nothing to mangle
      }
    }

    if (anAddModification.getParent() instanceof Table) {
      Iterator it = anAddModification.getChildren().iterator();
      while (it.hasNext()) {
        Object o = it.next();
        if (!(o instanceof ColumnNode)) {
          it.remove();
        }
      }
    }

    if (anAddModification.getParent() instanceof Column) {
      anAddModification.getChildren().clear();
    }

    return anAddModification;
  }
}
