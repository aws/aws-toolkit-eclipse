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
package com.amazonaws.eclipse.core.ui.swt;

/**
 * A callback allowing the creator to inject custom logic in after building a
 * particular widget (ie to wire up data binding).
 *
 * @param <T> the type of the widget being built
 */
public interface PostBuildHook<T> {
    /**
     * A method which is called with the newly-created widget, allowing the
     * callback to customize the object.
     *
     * @param value the newly-created value
     */
    void run(T value);
}
