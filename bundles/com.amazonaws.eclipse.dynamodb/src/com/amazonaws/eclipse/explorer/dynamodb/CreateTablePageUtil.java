/*
 * Copyright 2013 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.explorer.dynamodb;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/** Some utility methods that could be shared by different CreateTable pages. **/
public class CreateTablePageUtil {
    
    public static Text newTextField(Composite comp) {
        Text text = new Text(comp, SWT.BORDER);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(text);
        return text;
    }

    public static Link newLink(Listener linkListener, String linkText, Composite composite) {
        Link link = new Link(composite, SWT.WRAP);
        link.setText(linkText);
        link.addListener(SWT.Selection, linkListener);
        GridData data = new GridData(SWT.FILL, SWT.TOP, false, false);
        data.horizontalSpan = 3;
        link.setLayoutData(data);
        return link;
    }

    public static Group newGroup(Composite composite, String text, int columns) {
        Group group = new Group(composite, SWT.NONE);
        group.setText(text + ":");
        group.setLayout(new GridLayout(columns, false));
        GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        return group;
    }
    
    public static String stringJoin(List<String> stringList, String delimiter) {
        if (stringList == null) return "";
        
        StringBuilder sb = new StringBuilder();
        String loopDelimiter = "";
        
        for(String s : stringList) {
            sb.append(loopDelimiter);
            sb.append(s);            
            loopDelimiter = delimiter;
        }
        
        return sb.toString();
    }
}
