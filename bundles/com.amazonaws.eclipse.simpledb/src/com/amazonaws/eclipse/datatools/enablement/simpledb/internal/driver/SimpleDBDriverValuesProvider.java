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
package com.amazonaws.eclipse.datatools.enablement.simpledb.internal.driver;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.datatools.connectivity.drivers.DefaultDriverValuesProvider;
import org.eclipse.datatools.connectivity.drivers.IDriverValuesProvider;
import org.osgi.framework.Bundle;

/**
 * Searches for the driver jars in the driver plugin if present to provide the list to the driver UI.
 */
public class SimpleDBDriverValuesProvider extends DefaultDriverValuesProvider {

  public String getDriverDirName() {
    return "lib"; //$NON-NLS-1$
  }

  @SuppressWarnings("unchecked")
  @Override
  public String createDefaultValue(final String key) {
    /**
     * Check to see if the wrapper plug-in is in the Eclipse environment. If it is we'll use it and grab the driver jar
     * from there.
     */
    if (key.equals(IDriverValuesProvider.VALUE_CREATE_DEFAULT)) {
      Bundle[] bundles = Platform.getBundles("com.amazonaws.eclipse.datatools.enablement.simpledb.driver", null); //$NON-NLS-1$
      if (bundles != null && bundles.length > 0) {
        Enumeration<URL> jars = bundles[0].findEntries(getDriverDirName(), "*.jar", true); //$NON-NLS-1$
        while (jars != null && jars.hasMoreElements()) {
          URL url = jars.nextElement();
          if (url != null) {
            return Boolean.toString(true);
          }
        }
      }
    }
    if (key.equals(IDriverValuesProvider.VALUE_JARLIST)) {
      Bundle[] bundles = Platform.getBundles("com.amazonaws.eclipse.datatools.enablement.simpledb.driver", null); //$NON-NLS-1$
      if (bundles != null && bundles.length > 0) {
        Enumeration<URL> jars = bundles[0].findEntries(getDriverDirName(), "*.jar", true); //$NON-NLS-1$
        StringBuffer urls = null;
        while (jars != null && jars.hasMoreElements()) {
          URL url = jars.nextElement();

          if (url != null) {
            try {
              url = FileLocator.toFileURL(url);
              IPath path = new Path(url.getFile());
              if (urls == null) {
                urls = new StringBuffer();
              }
              if (urls.length() > 0) {
                urls.append(";"); //$NON-NLS-1$
              }
              urls.append(path.toOSString());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        if (urls != null && urls.length() > 0) {
          return urls.toString();
        }
      }
    }
    return super.createDefaultValue(key);
  }

}
