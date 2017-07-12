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
package com.amazonaws.eclipse.explorer.cloudfront;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.util.DateUtils;

public abstract class ResourceEditorDataLoaderThread extends Thread {

    public abstract void loadData();

    @Override
    public void run() {
        try {
            loadData();
        } catch (Exception e) {
            AwsToolkitCore.getDefault().logError("Unable to load resource information", e);
        }
    }

    protected void setText(Text label, Date value) {
        if (value == null) {
            label.setText("");
        } else {
            label.setText(DateUtils.formatRFC822Date(value));
        }
    }

    protected void setText(Text label, Boolean value) {
        if (value == null) label.setText("");
        else if (value) label.setText("Yes");
        else label.setText("No");
    }

    protected void setText(Text label, String value) {
        if (value == null) label.setText("");
        else label.setText(value);
    }

    protected void setText(Text label, List<String> value) {
        if (value == null) value = new ArrayList<>(0);

        StringBuilder buffer = new StringBuilder();
        for (String s : value) {
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(s);
        }
        label.setText(buffer.toString());
    }

    protected void setText(Text label, Map<String, String> map, String key) {
        if (map.get(key) == null) label.setText("");
        else label.setText(map.get(key));
    }

    protected void setText(Label label, Date value) {
        if (value == null) {
            label.setText("");
        } else {
            label.setText(DateUtils.formatRFC822Date(value));
        }
    }

    protected void setText(Label label, Boolean value) {
        if (value == null) label.setText("");
        else label.setText(value.toString());
    }

    protected void setText(Label label, String value) {
        if (value == null) label.setText("");
        else label.setText(value);
    }

    protected void setText(Label label, List<String> value) {
        if (value == null) value = new ArrayList<>(0);

        StringBuilder buffer = new StringBuilder();
        for (String s : value) {
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(s);
        }
        label.setText(buffer.toString());
    }

    protected void setText(Label label, Map<String, String> map, String key) {
        if (map.get(key) == null) label.setText("");
        else label.setText(map.get(key));
    }
}
