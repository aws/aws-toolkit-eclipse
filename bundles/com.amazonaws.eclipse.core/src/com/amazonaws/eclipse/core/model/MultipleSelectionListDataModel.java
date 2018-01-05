/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a widget of List with check box. The data model collects the checked items instead of
 * the selected items from the List.
 */
public class MultipleSelectionListDataModel<T> {
    private final List<T> selectedList = new ArrayList<>();

    public List<T> getSelectedList() {
        return selectedList;
    }
}
