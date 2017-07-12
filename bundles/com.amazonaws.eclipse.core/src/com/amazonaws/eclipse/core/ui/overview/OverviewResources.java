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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.services.IDisposable;
import org.osgi.framework.Bundle;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Manager for resources used in the AWS Toolkit for Eclipse overview page
 * (fonts, colors, form toolkit, images, etc).
 * <p>
 * This class is intended to be constructed only once and shared among all the
 * overview page components.
 * <p>
 * An instance of OverviewResources must be properly disposed of with the
 * {@link #dispose()} method when is isn't needed anymore so that the underlying
 * resources can be released.
 */
public class OverviewResources implements IDisposable {

    /** Form toolkit for creating widgets */
    private final FormToolkit toolkit;

    /** Map of allocated fonts by ID for the overview components */
    private Map<String, Font> managedFonts = new HashMap<>();

    /**
     * Map of shared fonts by ID - these fonts are allocated by the system and
     * will _not_ be released with the rest of the overview resources.
     */
    private Map<String, Font> sharedFonts = new HashMap<>();

    /** Registry of shared images for the overview components */
    private final ImageRegistry imageRegistry = new ImageRegistry();

    /*
     * ImageRegistry keys
     */
    public static final String IMAGE_GRADIENT = "gradient";
    public static final String IMAGE_GRADIENT_WITH_LOGO = "logo-gradient";
    public static final String IMAGE_CONFIGURE_BUTTON = "configure-button";
    public static final String IMAGE_BULLET = "orange-bullet";


    /**
     * Constructs a new OverviewResources instance, and prepares resources for
     * use.
     */
    public OverviewResources() {
        toolkit = new FormToolkit(Display.getCurrent());
        toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_NEVER);

        initializeColors();
        initializeFonts();
        initializeImages();
    }

    /**
     * Returns the FormToolkit resource for creating UI widgets in the overview
     * page components.
     *
     * @return The FormToolkit resource for creating UI widgets in the overview
     *         page components.
     */
    public FormToolkit getFormToolkit() {
        return toolkit;
    }

    /**
     * Returns the font resource with the specified ID.
     *
     * @param key
     *            The ID of the font resource to return.
     *
     * @return The font resource with the specified ID.
     */
    public Font getFont(String key) {
        if (managedFonts.containsKey(key)) {
            return managedFonts.get(key);
        } else {
            return sharedFonts.get(key);
        }
    }

    /**
     * Returns the color resource with the specified ID.
     *
     * @param key
     *            The ID of the color resource to return.
     *
     * @return The color resource with the specified ID.
     */
    public Color getColor(String key) {
        return toolkit.getColors().getColor(key);
    }

    /**
     * Returns the image resource with the specified ID.
     *
     * @param key
     *            The ID of the image resource to return.
     *
     * @return The image resource with the specified ID.
     */
    public Image getImage(String key) {
        return imageRegistry.get(key);
    }

    /**
     * Returns the external link image.
     *
     * @return The external link image.
     */
    public static Image getExternalLinkImage() {
        ImageRegistry imageRegistry = AwsToolkitCore.getDefault().getImageRegistry();
        return imageRegistry.get(AwsToolkitCore.IMAGE_EXTERNAL_LINK);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.services.IDisposable#dispose()
     */
    @Override
    public void dispose() {
        toolkit.dispose();
        imageRegistry.dispose();
        for (Font font : managedFonts.values()) {
            font.dispose();
        }
    }

    /*
     * Private Interface
     */

    /** Initializes image resources */
    private void initializeImages() {
        Bundle bundle = AwsToolkitCore.getDefault().getBundle();
        imageRegistry.put(IMAGE_GRADIENT, ImageDescriptor.createFromURL(bundle.getEntry("icons/overview/header-gradient.png")));
        imageRegistry.put(IMAGE_GRADIENT_WITH_LOGO, ImageDescriptor.createFromURL(bundle.getEntry("icons/overview/logo-header-gradient.png")));
        imageRegistry.put(IMAGE_CONFIGURE_BUTTON, ImageDescriptor.createFromURL(bundle.getEntry("icons/overview/configure-button.png")));
        imageRegistry.put(IMAGE_BULLET, ImageDescriptor.createFromURL(bundle.getEntry("icons/overview/bullet.png")));
    }

    /** Initializes color resources */
    private void initializeColors() {
        toolkit.getColors().createColor("amazon-orange", 222, 123, 32);
        toolkit.getColors().createColor("module-header", 94, 124, 169);
        toolkit.getColors().createColor("module-subheader", 102, 102, 102);
    }

    /** Initializes font resources */
    private void initializeFonts() {
        managedFonts.put("big-header", newFont(new FontData[] {
                new FontData("Verdana", 16, SWT.BOLD),
                new FontData("Arial", 16, SWT.BOLD),
                new FontData("Helvetica", 16, SWT.BOLD),
        }));
        managedFonts.put("resources-header", newFont(new FontData[] {
                new FontData("Verdana", 18, SWT.NORMAL),
                new FontData("Arial", 18, SWT.NORMAL),
                new FontData("Helvetica", 18, SWT.NORMAL),
        }));
        managedFonts.put("module-header", newFont(new FontData[] {
                new FontData("Verdana", 12, SWT.BOLD),
                new FontData("Arial", 12, SWT.BOLD),
                new FontData("Helvetica", 12, SWT.BOLD),
        }));
        managedFonts.put("module-subheader", newFont(new FontData[] {
                new FontData("Verdana", 11, SWT.BOLD),
                new FontData("Arial", 11, SWT.BOLD),
                new FontData("Helvetica", 11, SWT.BOLD),
        }));

        sharedFonts.put("text", JFaceResources.getDialogFont());
    }

    /**
     * Creates a new Font from the specified font data.
     *
     * @param fontData
     *            The font data for creating the new font.
     *
     * @return The new font.
     */
    private Font newFont(FontData[] fontData) {
        return new Font(Display.getCurrent(), fontData);
    }

}
