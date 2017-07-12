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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

import com.amazonaws.eclipse.core.AwsToolkitCore;

/**
 * Composite that uses gradients for the top and bottom borders. Subclasses are
 * expected to fill in the content through the header, main and footer
 * composites in this class.
 * <p>
 * This composite manages its own images so that they can be released as soon as
 * possible when this composite is disposed, so ensure that all instances are
 * properly disposed and that subclasses correctly call super.dispose() if they
 * override the dispose method.
 */
class GradientBoxComposite extends Composite {

    /**
     * Image registry so that any image resources are disposed of when this
     * object is disposed. Assumes only one GradientBoxComposite will be used,
     * so images aren't shared between multiple instances.
     */
    private ImageRegistry imageRegistry = new ImageRegistry();
    
    /** Form toolkit for creating the UI widgets */
    private FormToolkit toolkit;
    
    /** The header composite, for subclasses to populate */ 
    protected Composite headerComposite;
    
    /** The main composite, for subclasses to populate */
    protected Composite mainComposite;
    
    /** The footer composite, for subclasses to populate */
    protected Composite footerComposite;

    /**
     * Creates a new GradientBoxComposite widget within the specified parent and
     * using the specified form toolkit.
     * 
     * @param parent
     *            The composite to contain the new GradientBoxComposite.
     * @param toolkit
     *            The form toolkit to use for creating the UI.
     */
    public GradientBoxComposite(Composite parent, FormToolkit toolkit) {
        super(parent, SWT.NONE);
        this.toolkit = toolkit;
        
        initializeImageRegistry();
        setLayout(LayoutUtils.newSlimTableWrapLayout(3));
        
        createLabel("upper-left");
        headerComposite = createHorizontalComposite("top");
        createLabel("upper-right");

        createVerticalComposite("left");
        mainComposite = createHorizontalComposite(null);
        createVerticalComposite("right");

        createLabel("lower-left");
        footerComposite = createHorizontalComposite("bottom");
        createLabel("lower-right");
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        imageRegistry.dispose();
        super.dispose();
    }

    
    /*
     * Private Interface
     */

    /**
     * Creates a new label with a specified image, and sizes the label so that
     * it is the same size as the image.
     * 
     * @param imageKey
     *            The key of the image to use in the new label.
     * @return The new label.
     */
    private Label createLabel(String imageKey) {
        Image image = imageRegistry.get(imageKey);
        Label label = toolkit.createLabel(this, null);
        label.setImage(image);

        TableWrapData layoutData = new TableWrapData(TableWrapData.FILL);
        layoutData.heightHint = image.getImageData().height;
        layoutData.maxHeight = image.getImageData().height;
        layoutData.maxWidth = image.getImageData().width;
        label.setLayoutData(layoutData);

        return label;
    }

    /**
     * Creates a horizontal composite for this gradient box with the specified
     * background image tiled repeatedly in the horizontal direction.
     * 
     * @param imageKey
     *            The key of the image to use for the background.
     * @return The new composite.
     */
    private Composite createHorizontalComposite(String imageKey) {
        Composite composite = toolkit.createComposite(this);
        TableWrapData layoutData = new TableWrapData(TableWrapData.FILL);
        if (imageKey != null) {
            composite.setBackgroundImage(imageRegistry.get(imageKey));
            composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
            layoutData.heightHint = imageRegistry.get(imageKey).getImageData().height;
            layoutData.maxWidth = imageRegistry.get(imageKey).getImageData().width;
        }
        composite.setLayoutData(layoutData);

        return composite;
    }
    
    /**
     * Creates a vertical composite for this gradient box with the specified
     * background image tiled repeatedly in the vertical direction.
     * 
     * @param imageKey
     *            The key of the image to use for the background.
     * @return The new composite.
     */
    private Composite createVerticalComposite(String imageKey) {
        Composite composite = toolkit.createComposite(this);
        composite.setBackgroundImage(imageRegistry.get(imageKey));
        composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
        TableWrapData layoutData = new TableWrapData(TableWrapData.FILL, TableWrapData.FILL);
        layoutData.maxWidth = imageRegistry.get(imageKey).getImageData().width;
        layoutData.grabVertical = true;
        composite.setLayoutData(layoutData);

        return composite;
    }
    
    /**
     * Initializes the image resources used by this widget.
     */
    private void initializeImageRegistry() {
        imageRegistry.put("upper-left", createImageDescriptor("icons/gradient-box/upper-left.png"));
        imageRegistry.put("top", createImageDescriptor("icons/gradient-box/top.png"));
        imageRegistry.put("upper-right", createImageDescriptor("icons/gradient-box/upper-right.png"));
        
        imageRegistry.put("left", createImageDescriptor("icons/gradient-box/left.png"));
        imageRegistry.put("right", createImageDescriptor("icons/gradient-box/right.png"));

        imageRegistry.put("lower-left", createImageDescriptor("icons/gradient-box/lower-left.png"));
        imageRegistry.put("bottom", createImageDescriptor("icons/gradient-box/bottom.png"));
        imageRegistry.put("lower-right", createImageDescriptor("icons/gradient-box/lower-right.png"));
    }

    /**
     * Creates an ImageDescriptor from this plugin for the specified path.
     * 
     * @param imagePath
     *            The path of the image to load.
     * @return The new image descriptor.
     */
    private ImageDescriptor createImageDescriptor(String imagePath) {
        return ImageDescriptor.createFromURL(AwsToolkitCore.getDefault().getBundle().getEntry(imagePath));
    }

}
