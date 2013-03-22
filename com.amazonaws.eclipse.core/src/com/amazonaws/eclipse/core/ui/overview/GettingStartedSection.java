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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;

/**
 * Getting started section for the AWS Toolkit overview page.
 */
class GettingStartedSection extends GradientBoxComposite {

    /** Shared resources for the overview page components */
    private final OverviewResources resources;

    /**
     * Constructs a new header composite for the AWS Toolkit for Eclipse
     * overview page.
     *
     * @param parent
     *            The parent composite in which to create this header composite.
     * @param resources
     *            The UI resources for creating this composite (images, colors,
     *            fonts, etc).
     */
    public GettingStartedSection(Composite parent, OverviewResources resources) {
        super(parent, resources.getFormToolkit());
        this.resources = resources;

        GridLayout gridLayout = new GridLayout();
        gridLayout.marginBottom = 7;
        headerComposite.setLayout(gridLayout);

        // Header
        Label headerLabel = new Label(headerComposite, SWT.NONE);
        headerLabel.setText("Get Started");
        headerLabel.setFont(resources.getFont("module-header"));
        headerLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
        headerLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

        // Main Body
        TableWrapLayout layout = new TableWrapLayout();
        layout.leftMargin = 10;
        layout.rightMargin = 10;
        layout.verticalSpacing = 2;
        layout.topMargin = 10;
        mainComposite.setLayout(layout);
        mainComposite.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        Label testLabel = resources.getFormToolkit().createLabel(mainComposite,
                "Configure the toolkit with your access identifiers");
        testLabel.setLayoutData(new TableWrapData(TableWrapData.CENTER));
        testLabel.setFont(resources.getFont("text"));

        Image configureButtonImage = resources.getImage(OverviewResources.IMAGE_CONFIGURE_BUTTON);
        createImageHyperlink(mainComposite, configureButtonImage, null,
                "preference:" + AwsToolkitCore.ACCOUNT_PREFERENCE_PAGE_ID)
                .setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        layout = LayoutUtils.newSlimTableWrapLayout(1);
        layout.topMargin = 20;
        footerComposite.setLayout(layout);

        // Footer
        createImageHyperlink(footerComposite,
                AwsToolkitCore.getDefault().getImageRegistry().get(AwsToolkitCore.IMAGE_EXTERNAL_LINK),
                "Sign up for Amazon Web Services", AwsUrls.SIGN_UP_URL)
                .setFont(resources.getFont("text"));
    }

    /**
     * Creates a new hyperlink, centered in the specified parent composite.
     *
     * @param parent
     *            The parent in which to create the new hyperlink.
     * @param image
     *            Optional image to include in the hyperlink.
     * @param text
     *            Optional text for the hyperlink.
     * @param href
     *            Optional hyperlink href target.
     *
     * @return The new hyperlink widget.
     */
    private ImageHyperlink createImageHyperlink(Composite parent, Image image, String text, String href) {
        ImageHyperlink link = resources.getFormToolkit().createImageHyperlink(parent, SWT.RIGHT | SWT.NO_FOCUS);
        link.setText(text);
        link.setBackground(null);
        link.setImage(image);
        link.setHref(href);
        link.setUnderlined(false);
        link.addHyperlinkListener(new HyperlinkHandler());

        TableWrapData layoutData = new TableWrapData(TableWrapData.CENTER);
        layoutData.grabHorizontal = true;
        link.setLayoutData(layoutData);
        link.setFont(resources.getFont("text"));

        return link;
    }
}
