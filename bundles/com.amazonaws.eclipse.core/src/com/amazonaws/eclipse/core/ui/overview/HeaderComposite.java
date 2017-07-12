/*
 * Copyright 2010-2012 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.overview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.TableWrapData;

/**
 * Composite for creating the header on the AWS Toolkit for Eclipse overview
 * page.
 */
class HeaderComposite extends Composite {

    /**
     * Constructs a new header composite for the AWS Toolkit for Eclipse
     * overview page.
     * 
     * @param parent
     *            The parent composite in which to create this header composite.
     * @param resources
     *            The shared resources for creating this composite (colors,
     *            images, fonts, etc).
     */
    public HeaderComposite(Composite parent, OverviewResources resources) {
        super(parent, SWT.NONE);
        setLayout(LayoutUtils.newSlimTableWrapLayout(2));
        
        Image blueGradientImage = resources.getImage(OverviewResources.IMAGE_GRADIENT); 
        Image blueGradientLogoImage = resources.getImage(OverviewResources.IMAGE_GRADIENT_WITH_LOGO); 
        
        resources.getFormToolkit().createLabel(this, null)
            .setImage(blueGradientLogoImage);
        
        Composite composite = resources.getFormToolkit().createComposite(this);
        composite.setBackgroundImage(blueGradientImage);
        composite.setBackgroundMode(SWT.INHERIT_DEFAULT); 
        TableWrapData tableWrapData = new TableWrapData(TableWrapData.FILL_GRAB);
        tableWrapData.heightHint = blueGradientImage.getImageData().height;
        composite.setLayoutData(tableWrapData);
        composite.setLayout(new GridLayout());

        Label titleLabel = new Label(composite, SWT.NONE);
        titleLabel.setText("AWS Toolkit for Eclipse");
        titleLabel.setFont(resources.getFont("big-header"));
        titleLabel.setForeground(resources.getColor("amazon-orange"));
        titleLabel.setLayoutData(new GridData(SWT.END, SWT.END, true, true));
    }

}
