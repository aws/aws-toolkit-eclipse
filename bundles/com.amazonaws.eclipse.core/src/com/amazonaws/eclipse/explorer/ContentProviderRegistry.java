/*
 * Copyright 2011-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer;

import java.util.HashSet;
import java.util.Set;

/**
 * Registry of content providers which are providing content for the AWS Explorer view.
 */
public class ContentProviderRegistry {

    private static Set<AbstractContentProvider> contentProviders = new HashSet<>();

    private ContentProviderRegistry() {}

    public static void registerContentProvider(AbstractContentProvider contentProvider) {
        contentProviders.add(contentProvider);
    }

    public static void unregisterContentProvider(AbstractContentProvider contentProvider) {
        contentProviders.remove(contentProvider);
    }

    public static void refreshAllContentProviders() {
        for (AbstractContentProvider contentProvider : contentProviders) {
            contentProvider.refresh();
        }
    }

    public static void clearAllCachedResponses () {
        for (AbstractContentProvider contentProvider : contentProviders) {
            contentProvider.clearCachedResponse();
        }
    }
}
